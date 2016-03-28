package org.springframework.beans;

import java.beans.PropertyChangeEvent;

public class MethodInvocationException extends PropertyAccessException{
	public MethodInvocationException(Throwable ex, PropertyChangeEvent propertyChangeEvent) {
		super("Property '" + propertyChangeEvent.getPropertyName() + "' threw exception", propertyChangeEvent, ex);
	}
	
	public String getErrorCode() {
		return "methodInvocation";
	}
}
