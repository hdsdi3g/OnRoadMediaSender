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

import hd3gtv.onroadmediasend.MainControler;
import javafx.concurrent.Task;

public abstract class ActionTask extends Task<Void> {
	
	protected FileToSend reference;
	protected MainControler controler;
	
	public ActionTask(FileToSend reference, MainControler controler) {
		super();
		this.reference = reference;
		if (reference == null) {
			throw new NullPointerException("\"reference\" can't to be null"); //$NON-NLS-1$
		}
		
		this.controler = controler;
		if (controler == null) {
			throw new NullPointerException("\"controler\" can't to be null"); //$NON-NLS-1$
		}
		
		setOnFailed(event -> {
			reference.getConvertProgress().set(null);
			reference.getUploadProgress().set(null);
			reference.getState().set(OperationState.ERROR);
			Throwable e = getException();
			if (e != null) {
				getLogger().error("Error during task " + getClass().getSimpleName() + " with " + reference.getSource().get().getPath(), e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			controler.getQueue().stopAllForFile(reference);
		});
	}
	
	/**
	 * Must be blocking.
	 */
	public abstract void wantToStop();
	
	public FileToSend getFileReference() {
		return reference;
	}
	
	protected abstract Logger getLogger();
	
	public abstract long getProgressSize();
	
	private TaskUpdateGlobalProgress global_progress;
	
	public final void setCallableForGlobalProgress(TaskUpdateGlobalProgress global_progress) {
		this.global_progress = global_progress;
	}
	
	protected abstract void internalProcess(TaskUpdateGlobalProgress global_progress) throws Exception;
	
	protected final Void call() throws Exception {
		try {
			internalProcess(global_progress);
		} catch (Exception e) {
			controler.updateActiveItems();
			throw e;
		}
		controler.updateActiveItems();
		return null;
	}
}
