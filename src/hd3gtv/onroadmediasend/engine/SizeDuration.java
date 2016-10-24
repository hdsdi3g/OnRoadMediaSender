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

import org.apache.commons.io.FileUtils;

import hd3gtv.tools.Timecode;

public class SizeDuration {
	
	private long size;
	private Timecode timecode;
	
	public SizeDuration(long size) {
		this.size = size;
	}
	
	public SizeDuration(long size, Timecode timecode) {
		this(size);
		this.timecode = timecode;
	}
	
	@Override
	public String toString() {
		if (timecode == null) {
			return FileUtils.byteCountToDisplaySize(size);
		}
		return timecode.toString();
	}
}
