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
package hd3gtv.onroadmediasend;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

import hd3gtv.onroadmediasend.engine.ActionTask;
import hd3gtv.onroadmediasend.engine.FileToSend;
import hd3gtv.onroadmediasend.engine.Progression;
import hd3gtv.onroadmediasend.engine.TranscodeOperation;
import hd3gtv.onroadmediasend.engine.UploadOperation;

public class Queue {
	
	private final static Logger log = Logger.getLogger(Queue.class);
	
	private ExecutorService executor_analyst;
	private volatile InternalExecutor conversion_executor;
	private volatile InternalExecutor uploader_executor;
	private final Object lock;
	private MainControler controler;
	
	public Queue(MainControler controler) {
		lock = new Object();
		this.controler = controler;
		if (controler == null) {
			throw new NullPointerException("\"controler\" can't to be null"); //$NON-NLS-1$
		}
		
		executor_analyst = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("Exec Analyst"); //$NON-NLS-1$
			return t;
		});
		conversion_executor = new InternalExecutor(false);
		uploader_executor = new InternalExecutor(true);
	}
	
	public ExecutorService getQAnalyst() {
		return executor_analyst;
	}
	
	public void stopAllForFile(FileToSend file) {
		if (conversion_executor.isAlive()) {
			log.info("Stop all conv. for this file: " + file); //$NON-NLS-1$
			
			synchronized (lock) {
				if (conversion_executor.current != null) {
					if (conversion_executor.current.getFileReference().equals(file)) {
						conversion_executor.stopCurrentProcessing();
					} else {
						conversion_executor.internal_queue.removeIf((Predicate<ActionTask>) predicate -> {
							return predicate.getFileReference().equals(file);
						});
						conversion_executor.done_tasks.removeIf((Predicate<ActionTask>) predicate -> {
							return predicate.getFileReference().equals(file);
						});
					}
				}
			}
			conversion_executor.updateTotalProgression();
		}
		
		if (uploader_executor.isAlive()) {
			log.info("Stop all uploads for this file: " + file); //$NON-NLS-1$
			
			synchronized (lock) {
				if (uploader_executor.current != null) {
					if (uploader_executor.current.getFileReference().equals(file)) {
						uploader_executor.stopCurrentProcessing();
					} else {
						uploader_executor.internal_queue.removeIf((Predicate<ActionTask>) predicate -> {
							return predicate.getFileReference().equals(file);
						});
						uploader_executor.done_tasks.removeIf((Predicate<ActionTask>) predicate -> {
							return predicate.getFileReference().equals(file);
						});
					}
				}
			}
			uploader_executor.updateTotalProgression();
		}
	}
	
	public void addConversion(TranscodeOperation task) {
		log.debug("Add conv task " + task); //$NON-NLS-1$
		if (conversion_executor.isAlive() == false) {
			conversion_executor = new InternalExecutor(false);
			conversion_executor.internal_queue.add(task);
			conversion_executor.start();
		} else {
			synchronized (lock) {
				conversion_executor.internal_queue.add(task);
			}
		}
		conversion_executor.updateTotalProgression();
	}
	
	public void addUpload(UploadOperation task) {
		log.debug("Add upload task " + task); //$NON-NLS-1$
		if (uploader_executor.isAlive() == false) {
			uploader_executor = new InternalExecutor(true);
			uploader_executor.internal_queue.add(task);
			uploader_executor.start();
		} else {
			synchronized (lock) {
				uploader_executor.internal_queue.add(task);
			}
		}
		uploader_executor.updateTotalProgression();
	}
	
	/**
	 * Non blocking
	 */
	public void stopAllConversions() {
		if (conversion_executor.isAlive()) {
			log.info("Stop all conversions"); //$NON-NLS-1$
			
			synchronized (lock) {
				conversion_executor.internal_queue.clear();
			}
			conversion_executor.stopCurrentProcessing();
		}
		if (uploader_executor.isAlive()) {
			log.info("Stop all uploads"); //$NON-NLS-1$
			
			synchronized (lock) {
				uploader_executor.internal_queue.clear();
			}
			uploader_executor.stopCurrentProcessing();
		}
	}
	
	private class InternalExecutor extends Thread {
		
		private LinkedList<ActionTask> internal_queue;
		private ArrayList<ActionTask> done_tasks;
		private ActionTask current;
		private Progression progression;
		private long current_done;
		
		public InternalExecutor(boolean is_upload) {
			internal_queue = new LinkedList<>();
			done_tasks = new ArrayList<>();
			setDaemon(true);
			
			if (is_upload) {
				setName("Internal Executor Conversions"); //$NON-NLS-1$
				progression = controler.createTotalProgressUpload();
			} else {
				setName("Internal Executor Uploads"); //$NON-NLS-1$
				progression = controler.createTotalProgressConvertion();
			}
			log.debug("Create thread " + getName()); //$NON-NLS-1$
		}
		
		public void run() {
			while (internal_queue.isEmpty() == false) {
				synchronized (lock) {
					try {
						current = internal_queue.removeFirst();
					} catch (NoSuchElementException e) {
						current = null;
						progression.closeGlobal();
						return;
					}
				}
				
				updateTotalProgression();
				
				current.setCallableForGlobalProgress(actual_progress -> {
					progression.update(current_done + actual_progress, done_tasks.size() + 1);
				});
				
				Thread t = new Thread(current);
				t.setDaemon(true);
				t.setName("ActionTask"); //$NON-NLS-1$
				t.start();
				while (t.isAlive()) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					}
				}
				
				synchronized (lock) {
					done_tasks.add(current);
					current = null;
				}
				
				updateTotalProgression();
			}
			
			progression.closeGlobal();
		}
		
		private void updateTotalProgression() {
			synchronized (lock) {
				AtomicLong total_progress = new AtomicLong(0);
				int total_tasks = internal_queue.size() + done_tasks.size();
				
				done_tasks.forEach(item -> {
					total_progress.addAndGet(item.getProgressSize());
				});
				
				current_done = total_progress.get();
				
				if (current != null) {
					total_progress.addAndGet(current.getProgressSize());
					total_tasks++;
				}
				
				internal_queue.forEach(item -> {
					total_progress.addAndGet(item.getProgressSize());
				});
				
				progression.updateSize(total_progress.get(), total_tasks);
			}
		}
		
		public void stopCurrentProcessing() {
			synchronized (lock) {
				if (current != null) {
					log.info("Request to stop current processing: " + current); //$NON-NLS-1$
					current.wantToStop();
				}
			}
		}
		
	}
	
}
