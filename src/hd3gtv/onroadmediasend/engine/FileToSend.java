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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import hd3gtv.onroadmediasend.AppAlert;
import hd3gtv.onroadmediasend.MainControler;
import hd3gtv.onroadmediasend.Messages;
import hd3gtv.onroadmediasend.ffmpeg.FFprobe;
import hd3gtv.tools.FileValidation;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;

public class FileToSend {
	
	private final static Logger log = Logger.getLogger(FileToSend.class);
	
	private ObjectProperty<File> source;
	private ObjectProperty<Format> format;
	private ObjectProperty<String> convert_progress;
	private ObjectProperty<String> upload_progress;
	private ObjectProperty<SizeDuration> size_duration;
	private ObjectProperty<OperationState> operation;
	private FFprobe ffprobe;
	
	private File temp_output_file;
	private File main_output_file;
	
	private MainControler controller;
	
	private static final int MAX_VIDEO_BITRATE_BEFORE_TRANSCODE = Integer.parseInt(System.getProperty("engine.max_video_bitrate", "6000000")); //$NON-NLS-1$ //$NON-NLS-2$
	private static final int MAX_AUDIO_BITRATE_BEFORE_TRANSCODE = Integer.parseInt(System.getProperty("engine.max_audio_bitrate", "1000000")); //$NON-NLS-1$ //$NON-NLS-2$
	
	public FileToSend(File source, MainControler controller) throws NullPointerException, IOException {
		this.controller = controller;
		if (controller == null) {
			throw new NullPointerException("\"controller\" can't to be null"); //$NON-NLS-1$
		}
		
		FileValidation.checkExistsCanRead(source);
		
		this.source = new SimpleObjectProperty<File>(source);
		size_duration = new SimpleObjectProperty<SizeDuration>();
		format = new SimpleObjectProperty<Format>(Format.DOCUMENT);
		operation = new SimpleObjectProperty<OperationState>(OperationState.ADDED);
		convert_progress = new SimpleObjectProperty<String>(""); //$NON-NLS-1$
		upload_progress = new SimpleObjectProperty<String>(""); //$NON-NLS-1$
	}
	
	public void setOutputFiles(File main_output_file, File temp_output_file) {
		this.main_output_file = main_output_file;
		this.temp_output_file = temp_output_file;
	}
	
	/**
	 * @return main_output_file or source if null
	 */
	public File getMainOutputFile() {
		if (main_output_file != null) {
			if (main_output_file.exists()) {
				return main_output_file;
			}
		}
		return source.get();
	}
	
	public void deleteOutFiles() {
		FileUtils.deleteQuietly(main_output_file);
		FileUtils.deleteQuietly(temp_output_file);
	}
	
	public void removed() {
		controller.getQueue().stopAllForFile(this);
		controller.removeFileToSend(this);
		FileUtils.deleteQuietly(main_output_file);
		FileUtils.deleteQuietly(temp_output_file);
	}
	
	public ObjectProperty<File> getSource() {
		return source;
	}
	
	public ObjectProperty<Format> getFormat() {
		return format;
	}
	
	public ObjectProperty<String> getConvertProgress() {
		return convert_progress;
	}
	
	public ObjectProperty<String> getUploadProgress() {
		return upload_progress;
	}
	
	public ObjectProperty<OperationState> getState() {
		return operation;
	}
	
	public ObjectProperty<SizeDuration> getSizeDuration() {
		return size_duration;
	}
	
	public String toString() {
		return this.source.get().getPath();
	}
	
	public void publish() throws Exception {
		final File fsource = source.get();
		final FileToSend fsuper = this;
		
		Task<Void> t = new Task<Void>() {
			protected Void call() throws Exception {
				size_duration.set(new SizeDuration(fsource.length()));
				controller.addFileToSend(fsuper);
				controller.updateActiveItems();
				
				if (FilenameUtils.isExtension(source.getName().toLowerCase(), Arrays.asList("jpg", "png", "jpeg", "gif", "ico", "svg", "tga", "tif", "tiff", "psd"))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
					operation.set(OperationState.WAITING);
					return null;
				}
				
				FFprobe ffprobe = null;
				try {
					ffprobe = new FFprobe(fsource, controller.getExecutable("ffprobe")); //$NON-NLS-1$
					ffprobe.process();
					setFfprobe(ffprobe);
				} catch (Exception e) {
					log.debug("Can't exec ffprobe with " + fsource, e); //$NON-NLS-1$
					operation.set(OperationState.WAITING);
					return null;
				}
				
				try {
					size_duration.set(new SizeDuration(fsource.length(), ffprobe.getDuration()));
				} catch (Exception e) {
					log.error("Can't get duration with " + fsource, e); //$NON-NLS-1$
					operation.set(OperationState.WAITING);
					return null;
				}
				
				if (ffprobe.getVideoStream() != null) {
					if (ffprobe.hasAudio()) {
						format.set(Format.VIDEO);
					} else {
						format.set(Format.VIDEO_ONLY);
					}
				} else if (ffprobe.hasAudio()) {
					format.set(Format.AUDIO);
				} else {
					log.warn("Media file has no audio or video stream ! " + fsource); //$NON-NLS-1$
				}
				
				operation.set(OperationState.WAITING);
				
				return null;
			}
		};
		
		t.setOnFailed(event -> {
			new AppAlert(Messages.getString("FileToSend.cantadd") + fsource.getAbsolutePath(), onclose -> { //$NON-NLS-1$
				controller.removeFileToSend(this);
			});
			controller.updateActiveItems();
		});
		
		t.setOnSucceeded(event -> {
			if (getState().getValue() == null) {
				new AppAlert(Messages.getString("FileToSend.inconsistent-state") + fsource.getAbsolutePath(), onclose -> { //$NON-NLS-1$
					controller.removeFileToSend(this);
				});
			} else if (getState().getValue() != OperationState.WAITING) {
				new AppAlert(Messages.getString("FileToSend.inconsistent-state") + fsource.getAbsolutePath(), onclose -> { //$NON-NLS-1$
					controller.removeFileToSend(this);
				});
			}
			createOperations();
		});
		
		controller.getQueue().getQAnalyst().submit(t);
	}
	
	private void setFfprobe(FFprobe ffprobe) {
		this.ffprobe = ffprobe;
	}
	
	public FFprobe getFFprobe() {
		return ffprobe;
	}
	
	private void createOperations() {
		TranscodeOperation transcode = null;
		try {
			transcode = new TranscodeOperation(this, controller);
		} catch (NullPointerException e) {
			if (log.isDebugEnabled()) {
				log.debug("Error during media analyst", e); //$NON-NLS-1$
			}
			log.info("File is not media, send as-it on FTP"); //$NON-NLS-1$
			new UploadOperation(this, controller).publishToQueue();
			return;
		}
		
		if (getFormat().get() == Format.VIDEO) {
			if (ffprobe.getVideoBitrate() > MAX_VIDEO_BITRATE_BEFORE_TRANSCODE) {
				log.debug("Video bitrate is to high, must transcode " + source.get().getPath()); //$NON-NLS-1$
				transcode.publishToQueue();
				pushUploadOperation(transcode);
			} else if (ffprobe.getAllAudiosBitrate() > MAX_AUDIO_BITRATE_BEFORE_TRANSCODE) {
				log.debug("Audio bitrate is to high, must transcode " + source.get().getPath()); //$NON-NLS-1$
				transcode.publishToQueue();
				pushUploadOperation(transcode);
			} else {
				new UploadOperation(this, controller).publishToQueue();
			}
		} else if (getFormat().get() == Format.AUDIO) {
			if (ffprobe.getAllAudiosBitrate() > MAX_AUDIO_BITRATE_BEFORE_TRANSCODE) {
				log.debug("Audio bitrate is to high, must transcode " + source.get().getPath()); //$NON-NLS-1$
				transcode.publishToQueue();
				pushUploadOperation(transcode);
			} else {
				new UploadOperation(this, controller).publishToQueue();
			}
		} else if (getFormat().get() == Format.VIDEO_ONLY) {
			if (ffprobe.getVideoBitrate() > MAX_VIDEO_BITRATE_BEFORE_TRANSCODE) {
				log.debug("Video bitrate is to high, must transcode " + source.get().getPath()); //$NON-NLS-1$
				transcode.publishToQueue();
				pushUploadOperation(transcode);
			} else {
				new UploadOperation(this, controller).publishToQueue();
			}
		} else {
			/**
			 * Document
			 */
			new UploadOperation(this, controller).publishToQueue();
		}
	}
	
	private void pushUploadOperation(TranscodeOperation transcode) {
		transcode.setOnSucceeded(event -> {
			UploadOperation upload = new UploadOperation(this, controller);
			upload.publishToQueue();
		});
	}
	
}
