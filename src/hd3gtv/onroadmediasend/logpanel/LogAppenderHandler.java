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
package hd3gtv.onroadmediasend.logpanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import hd3gtv.onroadmediasend.MainConfiguration;

public class LogAppenderHandler extends AppenderSkeleton implements Appender {
	
	private final List<TextedEvent> events = Collections.synchronizedList(new ArrayList<>());
	private final List<TextedEventCallbacks> callbacks = new ArrayList<>(1);
	
	private PatternLayout layout_out;
	private PatternLayout layout_err;
	
	public LogAppenderHandler(MainConfiguration configuration) {
		layout_out = new PatternLayout(configuration.get().getString("messagepatternlayout.out", "%m%n")); //$NON-NLS-1$ //$NON-NLS-2$
		layout_err = new PatternLayout(configuration.get().getString("messagepatternlayout.err", "%m%n")); //$NON-NLS-1$ //$NON-NLS-2$
		Logger.getRootLogger().addAppender(this);
	}
	
	public void close() {
		events.clear();
	}
	
	public synchronized void registerCallback(TextedEventCallbacks callback) {
		callbacks.add(callback);
	}
	
	public synchronized void removeRegisterCallback(TextedEventCallbacks callback) {
		callbacks.remove(callback);
	}
	
	public boolean requiresLayout() {
		return false;
	}
	
	protected void append(LoggingEvent event) {
		TextedEvent t_event = new TextedEvent(event);
		events.add(t_event);
		
		callbacks.forEach(cb -> {
			cb.onTextedEvent(t_event);
		});
	}
	
	public class TextedEvent {
		private String content;
		private Level level;
		private String stacktrace;
		
		private TextedEvent(LoggingEvent event) {
			level = event.getLevel();
			if (level.isGreaterOrEqual(Level.WARN)) {
				content = layout_err.format(event);
			} else {
				content = layout_out.format(event);
			}
			ThrowableInformation ti = event.getThrowableInformation();
			if (ti != null) {
				stacktrace = StringUtils.join(ti.getThrowableStrRep(), "\n") + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				stacktrace = "";//$NON-NLS-1$
			}
			
		}
		
		public String getContent() {
			return content + stacktrace;
		}
		
		public boolean isRed() {
			return level.isGreaterOrEqual(Level.WARN);
		}
	}
	
	public List<TextedEvent> getAll() {
		return events;
	}
	
}