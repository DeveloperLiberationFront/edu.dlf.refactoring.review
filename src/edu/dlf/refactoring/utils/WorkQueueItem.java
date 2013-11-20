package edu.dlf.refactoring.utils;

import org.apache.log4j.Logger;

import edu.dlf.refactoring.design.ServiceLocator;

public abstract class WorkQueueItem implements Runnable{

	private final Logger logger = ServiceLocator.ResolveType(Logger.class);
	private final String itemName;
	
	public WorkQueueItem(String itemName) {
		this.itemName = itemName;
	}
	
	@Override
	public final void run() {
		long startTime = System.currentTimeMillis();
		internalRun();
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		logger.info("Time for " + this.itemName + ": " + elapsedTime);
		callBack();
	}
	
	abstract protected void internalRun();
	protected void callBack() {}
}
