package org.springframework.beans;

import java.beans.PropertyChangeEvent;

import org.springframework.core.ErrorCoded;

public abstract class PropertyAccessException extends BeansException implements ErrorCoded{
	private PropertyChangeEvent propertyChangeEvent;
	
	public PropertyAccessException(String msg, PropertyChangeEvent propertyChangeEvent) {
		super(msg);
		this.propertyChangeEvent = propertyChangeEvent;
	}
	
	public PropertyAccessException(String msg, PropertyChangeEvent propertyChangeEvent, Throwable ex) {
		super(msg, ex);
		this.propertyChangeEvent = propertyChangeEvent;
	}
	
	public PropertyChangeEvent getPropertyChangeEvent() {
		return propertyChangeEvent;
	}
}
