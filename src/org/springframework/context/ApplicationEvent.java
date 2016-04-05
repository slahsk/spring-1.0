package org.springframework.context;

import java.util.EventObject;

public abstract class ApplicationEvent extends EventObject{
	
	private long timestamp;
	
	public ApplicationEvent(Object source) {
		super(source);
		timestamp = System.currentTimeMillis();
	}
	
	public long getTimestamp() {
		return timestamp;
	}

}
