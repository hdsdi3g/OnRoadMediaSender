/*
 * This file is part of On Road Media Send.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.onroadmediasend.engine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import hd3gtv.onroadmediasend.MainControler;
import hd3gtv.onroadmediasend.Messages;
import hd3gtv.onroadmediasend.engine.Destination.Protocol;
import javafx.beans.property.ObjectProperty;

public class UploadOperation extends ActionTask {
	
	private volatile boolean want_to_stop;
	private final static Logger log = Logger.getLogger(UploadOperation.class);
	private long progress_size;
	private File file_version_to_send;
	private FTPClient ftpclient;
	private Destination destination;
	private static final long REFRESH_DURATION = 1000;
	private static final long TIME_TO_SLEEP_BETWEEN_TRIALS = 5000;
	
	public UploadOperation(FileToSend reference, MainControler controler) {
		super(reference, controler);
		file_version_to_send = reference.getMainOutputFile();
		progress_size = file_version_to_send.length();
		ftpclient = new FTPClient();
		ftpclient.setConnectTimeout(5000);
		ftpclient.setDataTimeout(5000);
		ftpclient.setDefaultTimeout(5000);
		destination = controler.getSelectedDestination();
	}
	
	public void wantToStop() {
		want_to_stop = true;
		reference.getState().set(OperationState.CANCELED);
		while (ftpclient.isConnected()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}
	}
	
	protected Logger getLogger() {
		return log;
	}
	
	public long getProgressSize() {
		return progress_size;
	}
	
	public void publishToQueue() {
		controler.getQueue().addUpload(this);
	}
	
	protected void internalProcess(TaskUpdateGlobalProgress global_progress) throws Exception {
		ObjectProperty<String> progression_display = reference.getUploadProgress();
		progression_display.set(Messages.getString("UploadOperation.starting")); //$NON-NLS-1$
		
		reference.getState().set(OperationState.SENDING);
		
		if (destination.getProtocol() != Protocol.FTP) {
			throw new IOException("Can't manage other protocols than FTP: " + destination.getProtocol()); //$NON-NLS-1$
		}
		
		Progression progression = new Progression(progress_size, progression_display);
		
		while (want_to_stop == false) {
			try {
				quietDisconnect();
				reconnect();
				
				FTPFile[] list = ftpclient.listFiles();
				Optional<FTPFile> opt_presence_server = getPresenceInServer(list);
				
				if (opt_presence_server.isPresent()) {
					FTPFile presence_server = opt_presence_server.get();
					
					if (presence_server.getSize() == file_version_to_send.length()) {
						progression_display.set(Messages.getString("UploadOperation.filewasuploaded")); //$NON-NLS-1$
						reference.getState().set(OperationState.SENDED);
						log.info("Skip file: " + file_version_to_send.getName()); //$NON-NLS-1$
					} else if (presence_server.getSize() < file_version_to_send.length()) {
						long skip = opt_presence_server.get().getSize();
						progression_display
								.set(Messages.getString("UploadOperation.restartuploadafterskip") + " " + FileUtils.byteCountToDisplaySize(skip) + " " + Messages.getString("UploadOperation.sended")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						upload(global_progress, progression, progression_display, skip);
					} else if (presence_server.getSize() > file_version_to_send.length()) {
						upload(global_progress, progression, progression_display, 0);
					}
				} else {
					upload(global_progress, progression, progression_display, 0);
				}
				break;
			} catch (Exception e) {
				log.error("Error during FTP operation", e); //$NON-NLS-1$
			}
			
			progression_display.set(Messages.getString("UploadOperation.errortrial")); //$NON-NLS-1$
			log.info("Wait next trial for connect/upload"); //$NON-NLS-1$
			
			/**
			 * Wait after the next trial.
			 */
			long end_date = System.currentTimeMillis() + TIME_TO_SLEEP_BETWEEN_TRIALS;
			while (end_date > System.currentTimeMillis() & want_to_stop == false) {
				Thread.sleep(50);
			}
			
			progression_display.set(Messages.getString("UploadOperation.restartupload")); //$NON-NLS-1$
		}
		
		if (ftpclient.isConnected() == false) {
			reconnect();
		}
		
		if (want_to_stop) {
			progression_display.set(Messages.getString("UploadOperation.removesended")); //$NON-NLS-1$
			boolean done = ftpclient.deleteFile(file_version_to_send.getName());
			log.info("Delete FTP file (stopped) " + file_version_to_send.getName() + " " + done); //$NON-NLS-1$ //$NON-NLS-2$
			ftpclient.disconnect();
		} else {
			progression_display.set(""); //$NON-NLS-1$
			reference.getState().set(OperationState.SENDED);
		}
	}
	
	private Optional<FTPFile> getPresenceInServer(FTPFile[] list) {
		return Arrays.asList(list).stream().filter((Predicate<? super FTPFile>) predicate -> {
			if (predicate.isFile() == false) {
				return false;
			}
			return predicate.getName().equals(file_version_to_send.getName());
		}).findFirst();
	}
	
	private void upload(TaskUpdateGlobalProgress global_progress, Progression progression, ObjectProperty<String> progression_display, long skip) throws IOException {
		ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
		
		if (skip > 0) {
			log.info("Restart upload " + file_version_to_send.getName() + " from " + skip); //$NON-NLS-1$ //$NON-NLS-2$
			ftpclient.setRestartOffset(skip);
		} else {
			log.info("Start upload " + file_version_to_send.getName() + " (" + file_version_to_send.length() + " bytes)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
		OutputStream os = ftpclient.storeFileStream(file_version_to_send.getName());
		if (os == null) {
			throw new NullPointerException("Store File Stream is null"); //$NON-NLS-1$
		}
		
		InputStream fis = new BufferedInputStream(new FileInputStream(file_version_to_send), 0xFFF);
		
		byte[] buffer = new byte[0xFFF];
		int len;
		long current_progress = skip;
		long last_update = 0;
		
		while ((len = fis.read(buffer)) != -1) {
			os.write(buffer, 0, len);
			current_progress += len;
			
			if (last_update + REFRESH_DURATION < System.currentTimeMillis()) {
				last_update = System.currentTimeMillis();
				global_progress.onUpdateLocalTaskProgress(current_progress);
				progression.update(current_progress);
				if (log.isTraceEnabled()) {
					log.trace("Progress " + file_version_to_send.getName() + " " + current_progress + "/" + file_version_to_send.length()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			if (want_to_stop) {
				break;
			}
		}
		
		IOUtils.closeQuietly(os);
		IOUtils.closeQuietly(fis);
	}
	
	private void reconnect() throws IOException {
		log.info("Reconnect to " + destination.toString()); //$NON-NLS-1$
		
		ftpclient.connect(destination.getHost(), destination.getPort());
		
		if (ftpclient.login(destination.getUsername(), destination.getPassword()) == false) {
			ftpclient.logout();
			throw new IOException("Can't login to server"); //$NON-NLS-1$
		}
		
		int reply = ftpclient.getReplyCode();
		if (FTPReply.isPositiveCompletion(reply) == false) {
			ftpclient.disconnect();
			throw new IOException("Can't login to server"); //$NON-NLS-1$
		}
		
		ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
		
		if (destination.isPassive() == false) {
			log.debug("Set FTP passive mode"); //$NON-NLS-1$
			ftpclient.enterLocalActiveMode();
		} else {
			log.debug("Set FTP active mode"); //$NON-NLS-1$
			ftpclient.enterLocalPassiveMode();
		}
	}
	
	private void quietDisconnect() {
		try {
			if (ftpclient.isConnected()) {
				ftpclient.disconnect();
			}
		} catch (Exception e) {
			log.warn("Disconnect error", e); //$NON-NLS-1$
		}
	}
	
	public String toString() {
		return "upload:" + reference.toString(); //$NON-NLS-1$
	}
	
}
