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

import org.apache.log4j.Logger;

import hd3gtv.tools.TimeUtils;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;

public class Progression {
	
	private final static Logger log = Logger.getLogger(Progression.class);
	
	private long pos = 0;
	private long size;
	private long start_date;
	private int iterations_size;
	
	private DoubleProperty progress_bar;
	private StringProperty possize_label;
	private StringProperty eta_label;
	private StringProperty percent_label;
	
	private ObjectProperty<String> progression_display;
	
	/**
	 * Used for global
	 */
	public Progression(long size, int iterations_size, DoubleProperty progress_bar, StringProperty possize_label, StringProperty eta_label, StringProperty percent_label) {
		start_date = System.currentTimeMillis();
		this.size = size;
		this.iterations_size = iterations_size;
		
		this.progress_bar = progress_bar;
		if (progress_bar == null) {
			throw new NullPointerException("\"progress_bar\" can't to be null"); //$NON-NLS-1$
		}
		this.possize_label = possize_label;
		if (possize_label == null) {
			throw new NullPointerException("\"possize_label\" can't to be null"); //$NON-NLS-1$
		}
		this.eta_label = eta_label;
		if (eta_label == null) {
			throw new NullPointerException("\"eta_label\" can't to be null"); //$NON-NLS-1$
		}
		this.percent_label = percent_label;
		if (percent_label == null) {
			throw new NullPointerException("\"percent_label\" can't to be null"); //$NON-NLS-1$
		}
		log.debug("Load Progression: size=" + size + ", iterations_size=" + iterations_size); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Used for items
	 */
	public Progression(long size, ObjectProperty<String> progression_display) {
		start_date = System.currentTimeMillis();
		this.size = size;
		this.progression_display = progression_display;
		if (progression_display == null) {
			throw new NullPointerException("\"progression_display\" can't to be null"); //$NON-NLS-1$
		}
		log.debug("Load Progression: size=" + size); //$NON-NLS-1$
	}
	
	/**
	 * @return pos unit / sec
	 */
	public double getSpeed() {
		long duration = System.currentTimeMillis() - start_date;
		if (duration == 0) {
			return 0d;
		}
		double result = (double) pos / ((double) duration / 1000d);
		if (log.isTraceEnabled()) {
			log.trace("getSpeed=" + result); //$NON-NLS-1$
		}
		return result;
	}
	
	public String getPercent() {
		double percent = ((double) pos / (double) size) * 100d;
		String result = Math.round(percent) + "%"; //$NON-NLS-1$
		if (log.isTraceEnabled()) {
			log.trace("getPercent=" + result); //$NON-NLS-1$
		}
		return result;
	}
	
	/**
	 * @return pos msec
	 */
	public long getETA() {
		long duration = System.currentTimeMillis() - start_date;
		if (duration == 0) {
			return 0l;
		}
		double speed = (double) pos / (double) duration;
		double eta = (double) (size - pos) / speed;
		long result = Math.round(eta);
		
		if (log.isTraceEnabled()) {
			log.trace("getETA=" + result + " / " + TimeUtils.secondstoHMS(result / 1000)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(TimeUtils.secondstoHMS(getETA() / 1000));
		sb.append(" "); //$NON-NLS-1$
		sb.append(getPercent());
		return sb.toString();
	}
	
	/**
	 * Used for items
	 */
	public synchronized void update(long pos) {
		this.pos = pos;
		if (log.isTraceEnabled()) {
			log.trace("update pos=" + pos); //$NON-NLS-1$
		}
		if (pos < this.pos) {
			throw new IndexOutOfBoundsException("\"pos\" can't be equals to " + pos); //$NON-NLS-1$
		}
		Platform.runLater(() -> progression_display.set(toString()));
	}
	
	/**
	 * Used for global
	 */
	public synchronized void update(long pos, int iteration_pos) {
		this.pos = pos;
		if (log.isTraceEnabled()) {
			log.trace("update pos=" + pos + ", iteration_pos=" + iteration_pos); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		if (progress_bar != null) {
			Platform.runLater(() -> progress_bar.set(((double) pos / (double) size)));
		}
		if (possize_label != null) {
			Platform.runLater(() -> possize_label.set(iteration_pos + "/" + iterations_size)); //$NON-NLS-1$
		}
		if (eta_label != null) {
			Platform.runLater(() -> eta_label.set(TimeUtils.secondstoHMS(getETA() / 1000)));
		}
		if (percent_label != null) {
			Platform.runLater(() -> percent_label.set(getPercent()));
		}
	}
	
	/**
	 * Used for global
	 */
	public synchronized void closeGlobal() {
		if (log.isTraceEnabled()) {
			log.trace("closeGlobal"); //$NON-NLS-1$
		}
		if (progress_bar != null) {
			Platform.runLater(() -> progress_bar.set(0));
		}
		if (possize_label != null) {
			Platform.runLater(() -> possize_label.set("")); //$NON-NLS-1$
		}
		if (eta_label != null) {
			Platform.runLater(() -> eta_label.set("")); //$NON-NLS-1$
		}
		if (percent_label != null) {
			Platform.runLater(() -> percent_label.set("")); //$NON-NLS-1$
		}
	}
	
	/**
	 * Used for global
	 */
	public synchronized void updateSize(long size, int iterations_size) {
		this.size = size;
		this.iterations_size = iterations_size;
		if (log.isTraceEnabled()) {
			log.trace("updateSize size=" + size + ", iterations_size=" + iterations_size); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
}
