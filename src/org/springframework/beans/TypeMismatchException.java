package org.springframework.beans;

import java.beans.PropertyChangeEvent;

public class TypeMismatchException extends PropertyAccessException{
	
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class requiredType) {
		super("Cannot convert property value of type [" +
		      (propertyChangeEvent.getNewValue() != null ?
		       propertyChangeEvent.getNewValue().getClass().getName() : null) +
		      "] to required type [" + requiredType.getName() + "]" +
					(propertyChangeEvent.getPropertyName() != null ?
					 " for property '" + propertyChangeEvent.getPropertyName() + "'" : ""),
					propertyChangeEvent);
	}
	
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class requiredType, Throwable ex) {
		super("Failed to convert property value of type [" +
		      (propertyChangeEvent.getNewValue() != null ?
		       propertyChangeEvent.getNewValue().getClass().getName() : null) +
		      "] to required type [" + requiredType.getName() + "]" +
					(propertyChangeEvent.getPropertyName() != null ?
					 " for property '" + propertyChangeEvent.getPropertyName() + "'" : ""),
					propertyChangeEvent, ex);
	}
	
	public String getErrorCode() {
		return "typeMismatch";
	}
}
