package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

public class BeanNotOfRequiredTypeException extends BeansException{
	
	private String name;
	private Class requiredType;
	private Object actualInstance;
	
	public BeanNotOfRequiredTypeException(String name, Class requiredType, Object actualInstance) {
		super("Bean named '" + name + "' must be of type [" + requiredType.getName() +
					"], but was actually of type [" + actualInstance.getClass().getName() + "]", null);
		this.name = name;
		this.requiredType = requiredType;
		this.actualInstance = actualInstance;
	}
	
	public String getBeanName() {
		return name;
	}

	public Class getRequiredType() {
		return requiredType;
	}

	public Class getActualType() {
		return actualInstance.getClass();
	}

	public Object getActualInstance() {
		return actualInstance;
	}

}
