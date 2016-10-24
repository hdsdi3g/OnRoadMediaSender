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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;

import org.apache.log4j.Logger;
import org.ffmpeg.ffprobe.FfprobeType;
import org.ffmpeg.ffprobe.StreamType;
import org.ffmpeg.ffprobe.StreamsType;

import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.FileValidation;
import hd3gtv.tools.Timecode;
import hd3gtv.tools.VideoConst.Framerate;
import hd3gtv.tools.XmlData;

public class FFprobe {
	
	private final static Logger log = Logger.getLogger(FFprobe.class);
	
	private File media_file;
	private File ffprobe_exec_file;
	private FfprobeType analyst;
	
	public FFprobe(File media_file, File ffprobe_exec_file) throws NullPointerException, IOException {
		this.media_file = media_file;
		if (media_file == null) {
			throw new NullPointerException("\"media_file\" can't to be null"); //$NON-NLS-1$
		}
		FileValidation.checkExistsCanRead(media_file);
		
		this.ffprobe_exec_file = ffprobe_exec_file;
		if (ffprobe_exec_file == null) {
			throw new NullPointerException("\"ffprobe_exec_file\" can't to be null"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Blocking
	 */
	public void process() throws IOException {
		ArrayList<String> params = new ArrayList<>();
		params.add("-print_format"); //$NON-NLS-1$
		params.add("xml"); //$NON-NLS-1$
		params.add("-show_streams"); //$NON-NLS-1$
		params.add("-show_format"); //$NON-NLS-1$
		params.add("-hide_banner"); //$NON-NLS-1$
		params.add("-i"); //$NON-NLS-1$
		params.add(media_file.getCanonicalPath());
		
		log.info("Start ffprobe analyst: " + ffprobe_exec_file + " " + params); //$NON-NLS-1$ //$NON-NLS-2$
		
		ExecprocessGettext process = new ExecprocessGettext(ffprobe_exec_file, params);
		process.setMaxexectime(5);
		
		try {
			process.start();
		} catch (Exception e) {
			log.info("ffmpeg result, STDOUT: " + process.getResultstdout().toString() + ", STDERR: " + process.getResultstderr().toString()); //$NON-NLS-1$ //$NON-NLS-2$
			throw e;
		}
		
		if (log.isTraceEnabled()) {
			log.trace("ffmpeg result, STDOUT: " + process.getResultstdout().toString() + ", STDERR: " + process.getResultstderr().toString()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		try {
			JAXBContext jc = JAXBContext.newInstance("org.ffmpeg.ffprobe"); //$NON-NLS-1$
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setEventHandler((ValidationEventHandler) e -> {
				ValidationEventLocator localtor = e.getLocator();
				log.warn("XML validation: " + e.getMessage() + " [s" + e.getSeverity() + "] at line " + localtor.getLineNumber() + ", column " + localtor.getColumnNumber() + " offset " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						+ localtor.getOffset() + " node: " + localtor.getNode() + ", object " + localtor.getObject()); //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			});
			
			XmlData xml_doc = XmlData.loadFromString(process.getResultstdout().toString());
			// Element root_document = xml_doc.getDocumentElement();
			// XmlData.removeAllTextAndComment(root_document);
			JAXBElement<FfprobeType> result = unmarshaller.unmarshal(xml_doc.getDocument(), FfprobeType.class);
			analyst = result.getValue();
		} catch (JAXBException e) {
			log.info("ffprobe XML: " + process.getResultstdout().toString()); //$NON-NLS-1$
			log.error("Can't load ffprobe XML", e); //$NON-NLS-1$
		}
	}
	
	public FfprobeType getAnalyst() {
		return analyst;
	}
	
	/**
	 * @param "video" or "audio"
	 * @return never null, maybe empty
	 */
	public ArrayList<StreamType> getByCodecType(String codec_type) {
		ArrayList<StreamType> result = new ArrayList<>(1);
		
		StreamsType streams = analyst.getStreams();
		if (streams == null) {
			return result;
		}
		streams.getStream().forEach(s -> {
			if (s.getCodecType().equalsIgnoreCase(codec_type)) {
				result.add(s);
			}
		});
		return result;
	}
	
	/**
	 * @return may be null, but never an artwork.
	 */
	public StreamType getVideoStream() {
		ArrayList<StreamType> streams = getByCodecType("video"); //$NON-NLS-1$
		if (streams.isEmpty()) {
			return null;
		}
		for (int pos = 0; pos < streams.size(); pos++) {
			StreamType video_s = streams.get(pos);
			if (video_s.getDisposition().getAttachedPic() == 0) {
				return video_s;
			}
		}
		return null;
	}
	
	/**
	 * @return never null, may be empty.
	 */
	public List<StreamType> getAllAudioStreams() {
		return getByCodecType("audio"); //$NON-NLS-1$
	}
	
	public Framerate getFramerate() {
		float duration = analyst.getFormat().getDuration();
		StreamType video_stream = getVideoStream();
		if (video_stream == null) {
			return Framerate.OTHER;
		}
		
		if (video_stream.getNbFrames() == null) {
			return Framerate.OTHER;
		}
		
		int nb_frames = video_stream.getNbFrames();
		String r_frame_rate = video_stream.getRFrameRate();
		String avg_frame_rate = video_stream.getAvgFrameRate();
		
		Framerate framerate = Framerate.getFramerate(r_frame_rate);
		if (framerate == null) {
			framerate = Framerate.getFramerate(avg_frame_rate);
		} else if (framerate == Framerate.OTHER) {
			if (Framerate.getFramerate(avg_frame_rate) != Framerate.OTHER) {
				framerate = Framerate.getFramerate(avg_frame_rate);
			}
		}
		if (((framerate == null) | (framerate == Framerate.OTHER)) & (nb_frames > 0) & (duration > 0f)) {
			framerate = Framerate.getFramerate((float) Math.round(((float) nb_frames / duration) * 10f) / 10f);
		}
		return framerate;
	}
	
	public Timecode getDuration() {
		return new Timecode(analyst.getFormat().getDuration(), getFramerate().getNumericValue());
	}
	
	public boolean hasAudio() {
		return getByCodecType("audio").isEmpty() == false; //$NON-NLS-1$
	}
	
	public boolean hasVideo() {
		return getByCodecType("video").isEmpty() == false; //$NON-NLS-1$
	}
	
	public int getAllAudiosBitrate() {
		int sum = 0;
		
		ArrayList<StreamType> streams = getByCodecType("audio"); //$NON-NLS-1$
		for (int pos = 0; pos < streams.size(); pos++) {
			StreamType audio_s = streams.get(pos);
			if (audio_s.getBitRate() != null) {
				sum += audio_s.getBitRate();
			}
		}
		return sum;
	}
	
	public int getVideoBitrate() {
		StreamType video_s = getVideoStream();
		if (video_s == null) {
			return 0;
		}
		if (video_s.getBitRate() != null) {
			return video_s.getBitRate();
		}
		return 0;
	}
	
}
