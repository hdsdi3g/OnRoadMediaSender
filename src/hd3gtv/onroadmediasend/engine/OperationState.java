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

public enum OperationState {
	
	ADDED, WAITING, CONVERTING, CONVERTED, SENDING, SENDED, ERROR, CANCELED;
	
	public String toString() {
		if (this == OperationState.WAITING) {
			return Messages.getString("OperationState.waiting"); //$NON-NLS-1$
		} else if (this == OperationState.ADDED) {
			return Messages.getString("OperationState.justadded"); //$NON-NLS-1$
		} else if (this == OperationState.CONVERTING) {
			return Messages.getString("OperationState.converting"); //$NON-NLS-1$
		} else if (this == OperationState.CONVERTED) {
			return Messages.getString("OperationState.converted"); //$NON-NLS-1$
		} else if (this == OperationState.SENDING) {
			return Messages.getString("OperationState.sending"); //$NON-NLS-1$
		} else if (this == OperationState.SENDED) {
			return Messages.getString("OperationState.sended"); //$NON-NLS-1$
		} else if (this == OperationState.ERROR) {
			return Messages.getString("OperationState.error"); //$NON-NLS-1$
		} else if (this == OperationState.CANCELED) {
			return Messages.getString("OperationState.canceled"); //$NON-NLS-1$
		} else {
			return Messages.getString("OperationState.invalid"); //$NON-NLS-1$
		}
	}
}
