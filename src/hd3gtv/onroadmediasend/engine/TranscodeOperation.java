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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ffmpeg.ffprobe.StreamType;

import hd3gtv.onroadmediasend.MainControler;
import hd3gtv.onroadmediasend.Messages;
import hd3gtv.onroadmediasend.ffmpeg.FFmpeg;
import hd3gtv.onroadmediasend.ffmpeg.FFprobe;
import javafx.beans.property.ObjectProperty;

public class TranscodeOperation extends ActionTask {
	
	private volatile boolean want_to_stop;
	private final static Logger log = Logger.getLogger(TranscodeOperation.class);
	private long progress_size;
	
	private FFmpeg ffmpeg;
	private FFprobe ffprobe;
	private StreamType video_stream;
	
	public TranscodeOperation(FileToSend reference, MainControler controler) {
		super(reference, controler);
		ffprobe = reference.getFFprobe();
		progress_size = Math.round(ffprobe.getAnalyst().getFormat().getDuration() * 1000f) + 1;
	}
	
	protected Logger getLogger() {
		return log;
	}
	
	protected void internalProcess(TaskUpdateGlobalProgress global_progress) throws Exception {
		File original = reference.getSource().get();
		
		List<StreamType> audio_stream = ffprobe.getAllAudioStreams();
		
		ObjectProperty<String> progression_display = reference.getConvertProgress();
		progression_display.set(Messages.getString("TranscodeOperation.starting")); //$NON-NLS-1$
		
		reference.getState().set(OperationState.CONVERTING);
		
		Quality quality = controler.getSelectedQuality();
		File transcoded_output_file = FFmpeg.prepareNewFile(original, original.getName(), quality.getFile_suffix(), false, quality.getFile_extension());
		File transcoded_temp_file = FFmpeg.prepareNewFile(controler.getTempDirectory(), original.getName(), "", true, quality.getFile_extension()); //$NON-NLS-1$
		
		reference.setOutputFiles(transcoded_output_file, transcoded_temp_file);
		
		if (transcoded_output_file.exists()) {
			progression_display.set(Messages.getString("TranscodeOperation.filewasconv")); //$NON-NLS-1$
			reference.getState().set(OperationState.CONVERTED);
			return;
		}
		
		if (quality.audioOnly() && audio_stream.isEmpty()) {
			throw new IOException("No audio stream, but selected codec has only audio in its configuration"); //$NON-NLS-1$
		} else if (quality.videoOnly() && (video_stream == null)) {
			throw new IOException("No video stream, but selected codec has only video in its configuration"); //$NON-NLS-1$
		}
		
		Progression progression = new Progression(progress_size, progression_display);
		
		try {
			ffmpeg = new FFmpeg(original, transcoded_temp_file, quality, ffprobe, controler.getExecutable("ffmpeg"), controler.getTempDirectory()); //$NON-NLS-1$
			ffmpeg.process(progression, global_progress);
		} catch (Exception e) {
			if (transcoded_temp_file.exists()) {
				log.info("Remove temp out file before throw exception. " + transcoded_temp_file.getPath()); //$NON-NLS-1$
				FileUtils.deleteQuietly(transcoded_temp_file);
			}
			throw e;
		}
		
		if (want_to_stop) {
			return;
		}
		
		FileUtils.moveFile(transcoded_temp_file, transcoded_output_file);
		
		reference.getState().set(OperationState.CONVERTED);
		reference.getConvertProgress().set(""); //$NON-NLS-1$
	}
	
	public long getProgressSize() {
		return progress_size;
	}
	
	public void wantToStop() {
		if (ffmpeg != null) {
			if (ffmpeg.isAlive()) {
				want_to_stop = true;
				ffmpeg.wantToStop();
				reference.getState().set(OperationState.CANCELED);
				reference.deleteOutFiles();
			}
		}
	}
	
	public void publishToQueue() {
		controler.getQueue().addConversion(this);
	}
	
	public String toString() {
		return "transcode:" + reference.toString(); //$NON-NLS-1$
	}
	
}
