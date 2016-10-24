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

import hd3gtv.onroadmediasend.Messages;

public enum Format {
	VIDEO, AUDIO, VIDEO_ONLY, DOCUMENT;
	
	public String toString() {
		if (this == Format.AUDIO) {
			return Messages.getString("Format.audio"); //$NON-NLS-1$
		} else if (this == Format.VIDEO) {
			return Messages.getString("Format.video"); //$NON-NLS-1$
		} else if (this == Format.VIDEO_ONLY) {
			return Messages.getString("Format.mutedvideo"); //$NON-NLS-1$
		} else if (this == Format.DOCUMENT) {
			return Messages.getString("Format.document"); //$NON-NLS-1$
		} else {
			return Messages.getString("Format.invalid"); //$NON-NLS-1$
		}
	}
}
