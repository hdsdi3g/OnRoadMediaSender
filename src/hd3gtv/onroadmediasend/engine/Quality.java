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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import hd3gtv.tools.VideoConst.Resolution;

public class Quality {
	
	private String label;
	private String file_suffix;
	private String video_codec;
	private String audio_codec;
	private String video_bitrate;
	private String audio_bitrate;
	private String file_extension;
	private Resolution video_resolution;
	
	public static ArrayList<Quality> getConfiguredQualities(List<HierarchicalConfiguration<ImmutableNode>> conf) {
		ArrayList<Quality> result = new ArrayList<>();
		
		conf.forEach(node -> {
			Quality q = new Quality(node.getString("label"), node.getString("file_suffix")); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (node.containsKey("video_codec")) { //$NON-NLS-1$
				q.setVideo(node.getString("video_codec"), node.getString("video_bitrate"), node.getString("video_resolution")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			if (node.containsKey("audio_codec")) { //$NON-NLS-1$
				q.setAudio(node.getString("audio_codec"), node.getString("audio_bitrate")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			q.file_extension = "mp4"; //$NON-NLS-1$
			if (node.containsKey("file_extension")) { //$NON-NLS-1$
				q.file_extension = node.getString("file_extension"); //$NON-NLS-1$
			}
			result.add(q);
		});
		return result;
	}
	
	public Quality(String label, String file_suffix) {
		this.label = label;
		if (label == null) {
			throw new NullPointerException("\"label\" can't to be null"); //$NON-NLS-1$
		}
		this.file_suffix = file_suffix;
		if (file_suffix == null) {
			throw new NullPointerException("\"file_suffix\" can't to be null"); //$NON-NLS-1$
		}
	}
	
	public void setAudio(String audio_codec, String audio_bitrate) {
		this.audio_bitrate = audio_bitrate;
		if (audio_bitrate == null) {
			throw new NullPointerException("\"audio_bitrate\" can't to be null"); //$NON-NLS-1$
		}
		this.audio_codec = audio_codec;
		if (audio_codec == null) {
			throw new NullPointerException("\"audio_codec\" can't to be null"); //$NON-NLS-1$
		}
	}
	
	public void setVideo(String video_codec, String video_bitrate, String video_resolution) {
		this.video_bitrate = video_bitrate;
		if (video_bitrate == null) {
			throw new NullPointerException("\"video_bitrate\" can't to be null"); //$NON-NLS-1$
		}
		this.video_codec = video_codec;
		if (video_codec == null) {
			throw new NullPointerException("\"video_codec\" can't to be null"); //$NON-NLS-1$
		}
		if (video_resolution == null) {
			throw new NullPointerException("\"video_resolution\" can't to be null"); //$NON-NLS-1$
		}
		this.video_resolution = Resolution.valueOf(video_resolution);
	}
	
	public String toString() {
		return label;
	}
	
	public boolean audioOnly() {
		return (video_codec == null) || (video_bitrate == null) && (audio_codec != null) && (audio_bitrate != null);
	}
	
	public boolean videoOnly() {
		return (audio_codec == null) || (audio_bitrate == null) && (video_codec != null) && (video_bitrate != null);
	}
	
	public String getFile_suffix() {
		return file_suffix;
	}
	
	public String getFile_extension() {
		return file_extension;
	}
	
	public String getVideoResolution() {
		Point p = Resolution.getResolution(video_resolution);
		return p.x + "x" + p.y; //$NON-NLS-1$
	}
	
	public ArrayList<String> makeFFmpegParams(boolean has_audio, boolean has_video) {
		ArrayList<String> result = new ArrayList<>();
		
		if (has_audio && videoOnly()) {
			result.add("-s"); //$NON-NLS-1$
			result.add(getVideoResolution());
			result.add("-codec:v"); //$NON-NLS-1$
			result.add(video_codec);
			result.add("-b:v"); //$NON-NLS-1$
			result.add(video_bitrate);
			
			result.add("-an"); //$NON-NLS-1$
		} else if (has_video && audioOnly()) {
			result.add("-vn"); //$NON-NLS-1$
			
			result.add("-codec:a"); //$NON-NLS-1$
			result.add(audio_codec);
			result.add("-b:a"); //$NON-NLS-1$
			result.add(audio_bitrate);
		} else if (has_video == false && videoOnly()) {
			throw new NullPointerException("Stream has not video, but quality is for video only"); //$NON-NLS-1$
		} else if (has_audio == false && audioOnly()) {
			throw new NullPointerException("Stream has not audio, but quality is for audio only"); //$NON-NLS-1$
		} else if (has_audio && has_video) {
			result.add("-s"); //$NON-NLS-1$
			result.add(getVideoResolution());
			result.add("-codec:v"); //$NON-NLS-1$
			result.add(video_codec);
			result.add("-b:v"); //$NON-NLS-1$
			result.add(video_bitrate);
			
			result.add("-codec:a"); //$NON-NLS-1$
			result.add(audio_codec);
			result.add("-b:a"); //$NON-NLS-1$
			result.add(audio_bitrate);
		} else if (has_audio && has_video == false) {
			result.add("-codec:a"); //$NON-NLS-1$
			result.add(audio_codec);
			result.add("-b:a"); //$NON-NLS-1$
			result.add(audio_bitrate);
		} else if (has_audio == false && has_video) {
			result.add("-s"); //$NON-NLS-1$
			result.add(getVideoResolution());
			result.add("-codec:v"); //$NON-NLS-1$
			result.add(video_codec);
			result.add("-b:v"); //$NON-NLS-1$
			result.add(video_bitrate);
		}
		
		result.add("-f"); //$NON-NLS-1$
		result.add("mov"); //$NON-NLS-1$
		result.add("-movflags"); //$NON-NLS-1$
		result.add("faststart"); //$NON-NLS-1$
		
		return result;
	}
	
}
