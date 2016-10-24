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
package hd3gtv.onroadmediasend.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;

import hd3gtv.onroadmediasend.engine.Progression;
import hd3gtv.onroadmediasend.engine.Quality;
import hd3gtv.onroadmediasend.engine.TaskUpdateGlobalProgress;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessEvent;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.FileValidation;

public class FFmpeg {
	
	private final static Logger log = Logger.getLogger(FFmpeg.class);
	
	private File source_file;
	private File output_file;
	private File ffmpeg_exec_file;
	private FFprobe analyst;
	private File temp_directory;
	private Quality quality;
	private Execprocess process;
	
	public FFmpeg(File source_file, File output_file, Quality quality, FFprobe analyst, File ffmpeg_exec_file, File temp_directory) throws NullPointerException, IOException {
		this.source_file = source_file;
		if (source_file == null) {
			throw new NullPointerException("\"source_file\" can't to be null"); //$NON-NLS-1$
		}
		FileValidation.checkExistsCanRead(source_file);
		
		this.output_file = output_file;
		if (output_file == null) {
			throw new NullPointerException("\"output_file\" can't to be null"); //$NON-NLS-1$
		}
		
		this.quality = quality;
		if (quality == null) {
			throw new NullPointerException("\"quality\" can't to be null"); //$NON-NLS-1$
		}
		
		this.analyst = analyst;
		if (analyst == null) {
			throw new NullPointerException("\"analyst\" can't to be null"); //$NON-NLS-1$
		}
		
		this.ffmpeg_exec_file = ffmpeg_exec_file;
		if (ffmpeg_exec_file == null) {
			throw new NullPointerException("\"ffmpeg_exec_file\" can't to be null"); //$NON-NLS-1$
		}
		FileValidation.checkExistsCanRead(source_file);
		FileValidation.checkCanExecute(source_file);
		
		this.temp_directory = temp_directory;
		if (temp_directory == null) {
			throw new NullPointerException("\"temp_directory\" can't to be null"); //$NON-NLS-1$
		}
		FileValidation.checkExistsCanRead(temp_directory);
		FileValidation.checkIsDirectory(temp_directory);
		FileValidation.checkIsWritable(temp_directory);
	}
	
	/**
	 * @param base_dir can be a file, the parent dir will be used.
	 * @param base_name can be have a path and an extension.
	 */
	public static File prepareNewFile(File base_dir, String base_name, String suffix, boolean add_random, String extension) {
		StringBuilder sb = new StringBuilder();
		
		if (base_dir.isDirectory()) {
			sb.append(base_dir.getAbsolutePath());
		} else {
			sb.append(base_dir.getParentFile().getAbsolutePath());
		}
		
		sb.append(File.separator);
		
		sb.append(FilenameUtils.getBaseName(base_name));
		
		if (suffix.equals("") == false) { //$NON-NLS-1$
			if (suffix.startsWith("_") == false && suffix.startsWith("-") == false && suffix.startsWith(" ") == false) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sb.append("_"); //$NON-NLS-1$
			}
			sb.append(suffix);
		}
		
		if (add_random) {
			sb.append("_"); //$NON-NLS-1$
			sb.append(RandomStringUtils.random(6, true, false));
		}
		
		if (extension.startsWith(".") == false) { //$NON-NLS-1$
			sb.append("."); //$NON-NLS-1$
		}
		sb.append(extension);
		
		return new File(sb.toString());
	}
	
	public void process(Progression progression, TaskUpdateGlobalProgress global_progress) throws Exception {
		FFmpegEvents events = new FFmpegEvents();
		
		File progressfile = prepareNewFile(temp_directory, source_file.getName(), "progress", false, "log"); //$NON-NLS-1$ //$NON-NLS-2$
		FileUtils.deleteQuietly(progressfile);
		TranscodeProgress tprogress = new TranscodeProgress(progressfile, progression, global_progress);
		tprogress.startWatching();
		
		boolean want_audio = quality.videoOnly() == false && analyst.hasAudio();
		boolean want_video = quality.audioOnly() == false && analyst.hasVideo();
		
		ArrayList<String> params = new ArrayList<>();
		params.add("-i"); //$NON-NLS-1$
		params.add(source_file.getCanonicalPath());
		
		params.add("-y"); //$NON-NLS-1$
		params.add("-progress"); //$NON-NLS-1$
		params.add(progressfile.getAbsolutePath());
		
		if (want_audio) {
			if (analyst.getAllAudioStreams().get(0).getChannels() == 1 && analyst.getAllAudioStreams().size() > 1) {
				if (analyst.getAllAudioStreams().get(1).getChannels() == 1) {
					params.add("-filter_complex"); //$NON-NLS-1$
					params.add("[0:" + analyst.getAllAudioStreams().get(0).getIndex() + "][0:" + analyst.getAllAudioStreams().get(1).getIndex() + "]amerge=inputs=2[a12]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if (want_video) {
						params.add("-map"); //$NON-NLS-1$
						params.add("0:" + analyst.getVideoStream().getIndex() + ""); //$NON-NLS-1$ //$NON-NLS-2$
					}
					params.add("-map"); //$NON-NLS-1$
					params.add("[a12]"); //$NON-NLS-1$
				}
			}
		}
		
		if (want_video) {
			params.add("-vf"); //$NON-NLS-1$
			if (quality.getVideoResolution().equals(analyst.getVideoStream().getCodedWidth() + "x" + analyst.getVideoStream().getCodedHeight())) { //$NON-NLS-1$
				params.add("setfield=tff,fieldorder=tff"); //$NON-NLS-1$
			} else {
				if (quality.getVideoResolution().equals("720x576") | quality.getVideoResolution().equals("720x480")) { //$NON-NLS-1$ //$NON-NLS-2$
					params.add("scale=" + quality.getVideoResolution() + ":interl=1,setfield=tff,fieldorder=tff,setdar=dar=16/9"); //$NON-NLS-1$ //$NON-NLS-2$
					
					params.add("-aspect"); //$NON-NLS-1$
					params.add("16:9"); //$NON-NLS-1$
				} else {
					params.add("scale=" + quality.getVideoResolution() + ":interl=1,setfield=tff,fieldorder=tff"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		
		if (want_video) {
			params.add("-profile:v"); //$NON-NLS-1$
			params.add("high"); //$NON-NLS-1$
			
			params.add("-pix_fmt"); //$NON-NLS-1$
			params.add("yuv420p"); //$NON-NLS-1$
		}
		
		params.addAll(quality.makeFFmpegParams(analyst.hasAudio(), analyst.hasVideo()));
		
		params.add(output_file.getAbsolutePath());
		
		process = new Execprocess(ffmpeg_exec_file, params, events);
		process.setWorkingDirectory(temp_directory);
		
		try {
			process.start();
			
			while (process.isAlive()) {
				Thread.sleep(10);
			}
		} catch (Exception e) {
			log.error("Can't exec ffmpeg", e); //$NON-NLS-1$
		}
		
		tprogress.stopWatching();
		
		if (process.getExitvalue() != 0 && (process.getStatus() != Execprocess.STATE_KILL)) {
			throw new IOException("Bad ffmpeg execution (exit value is: " + process.getExitvalue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		process = null;
	}
	
	public synchronized boolean isAlive() {
		if (process == null) {
			return false;
		}
		return process.isAlive();
	}
	
	/**
	 * Blocking
	 */
	public synchronized void wantToStop() {
		if (process == null) {
			return;
		}
		process.blockingKill();
	}
	
	private class TranscodeProgress extends Thread {
		
		private File progressfile;
		private boolean stopthread;
		private Progression progression;
		private TaskUpdateGlobalProgress global_progress;
		
		TranscodeProgress(File progressfile, Progression progression, TaskUpdateGlobalProgress global_progress) {
			super();
			this.progressfile = progressfile;
			if (progressfile == null) {
				throw new NullPointerException("\"progressfile\" can't to be null"); //$NON-NLS-1$
			}
			this.progression = progression;
			if (progression == null) {
				throw new NullPointerException("\"progression\" can't to be null"); //$NON-NLS-1$
			}
			this.global_progress = global_progress;
			if (global_progress == null) {
				throw new NullPointerException("\"global_progress\" can't to be null"); //$NON-NLS-1$
			}
		}
		
		/**
		 * Non blocking
		 */
		synchronized void stopWatching() {
			stopthread = true;
		}
		
		void startWatching() {
			setDaemon(true);
			setName("TranscodeProgress"); //$NON-NLS-1$
			stopthread = false;
			start();
			log.debug("Start watch transcode progress for: " + progressfile.getPath()); //$NON-NLS-1$
		}
		
		public void run() {
			try {
				stopthread = false;
				String line;
				BufferedReader reader;
				
				String s_out_time_ms = "0"; //$NON-NLS-1$
				long out_time_ms = 0;
				
				/**
				 * bitrate= 175.9kbits/s
				 * total_size=98033
				 * out_time_ms=4458667
				 * out_time=00:00:04.458667
				 * dup_frames=0
				 * drop_frames=0
				 * speed=8.89x
				 * progress=continue
				 */
				reader = null;
				while (stopthread == false) {
					try {
						reader = new BufferedReader(new InputStreamReader(new FileInputStream(progressfile)), 0xFFFF);
					} catch (FileNotFoundException e) {
						Thread.sleep(100);
						continue;
					}
					
					while (((line = reader.readLine()) != null)) {
						if (line.startsWith("out_time_ms=")) { //$NON-NLS-1$
							s_out_time_ms = line.substring("out_time_ms=".length()); //$NON-NLS-1$
						} else if (line.equals("progress=end")) { //$NON-NLS-1$
							reader.close();
							log.debug("Watch ffmpeg progress is ended"); //$NON-NLS-1$
							return;
						}
					}
					reader.close();
					
					if (s_out_time_ms.equals("0")) { //$NON-NLS-1$
						Thread.sleep(500);
						continue;
					}
					
					out_time_ms = Long.parseLong(s_out_time_ms) / 1000;
					
					progression.update(out_time_ms);
					global_progress.onUpdateLocalTaskProgress(out_time_ms);
					
					Thread.sleep(500);
				}
			} catch (Exception e) {
				log.error("Error during progress analyst", e); //$NON-NLS-1$
			}
			
			if (progressfile.exists()) {
				try {
					FileUtils.forceDelete(progressfile);
				} catch (IOException e) {
					log.error("Can't delete progressfile: " + progressfile.getPath(), e); //$NON-NLS-1$
				}
			}
			
			log.trace("Watch transcode progress for: " + progressfile.getPath() + " is ended"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	private class FFmpegEvents implements ExecprocessEvent {
		
		// private String last_message;
		
		public void onError(IOException ioe) {
			log.error("FFmpeg error", ioe); //$NON-NLS-1$
		}
		
		public void onError(InterruptedException ie) {
			log.error("FFmpeg threads error", ie); //$NON-NLS-1$
		}
		
		public void onStdout(String message) {
			if (log.isTraceEnabled()) {
				log.trace("ffmpeg-stdout > " + message); //$NON-NLS-1$
			}
		}
		
		public void onStderr(String message) {
			if (log.isTraceEnabled()) {
				log.trace("ffmpeg-stderr > " + message); //$NON-NLS-1$
			}
			// last_message = message;
		}
		
		/*public String getLast_message() {
			return last_message;
		}*/
		
		public void onStart(String commandline, File working_directory) {
			if (working_directory != null) {
				log.info("start ffmpeg: " + commandline + "\t in " + working_directory); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				log.info("start ffmpeg: " + commandline); //$NON-NLS-1$
			}
		}
		
		public void onEnd(int exitvalue, long execution_duration) {
			log.debug("End ffmpeg execution, after " + (double) execution_duration / 1000d + " sec"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		public void onKill(long execution_duration) {
			log.info("FFmpeg is killed, after " + (double) execution_duration / 1000d + " sec"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public static void convertImage(File ffmpeg_exec, File source, File dest, int width, int height) throws IOException {
		ArrayList<String> params = new ArrayList<>();
		params.add("-y"); //$NON-NLS-1$
		params.add("-i"); //$NON-NLS-1$
		params.add(source.getAbsolutePath());
		params.add("-vf"); //$NON-NLS-1$
		params.add("scale=w=" + width + ":h=" + height + ":force_original_aspect_ratio=decrease"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		params.add(dest.getAbsolutePath());
		
		log.info("Start ffmpeg convert logo image: " + ffmpeg_exec + " " + params); //$NON-NLS-1$ //$NON-NLS-2$
		
		ExecprocessGettext process = new ExecprocessGettext(ffmpeg_exec, params);
		process.setMaxexectime(5);
		try {
			process.start();
		} catch (IOException e) {
			log.info("ffmpeg stdout " + process.getResultstdout()); //$NON-NLS-1$
			log.info("ffmpeg stderr " + process.getResultstderr()); //$NON-NLS-1$
			throw e;
		}
	}
	
}
