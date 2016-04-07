package org.springframework.context.event;

import java.util.EventObject;

public class ApplicationEvent extends EventObject{
	
	private long timestamp;
	
	public ApplicationEvent(Object source) {
		super(source);
		timestamp = System.currentTimeMillis();
	}
	
	public long getTimestamp() {
		return timestamp;
	}

}
