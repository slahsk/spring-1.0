package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

public class NoSuchBeanDefinitionException extends BeansException{
	private String beanName;
	private Class beanType;
	
	public NoSuchBeanDefinitionException(String name, String message) {
		super("No bean named '" + name + "' is defined: " + message);
		this.beanName = name;
	}
	
	public NoSuchBeanDefinitionException(Class type, String message) {
		super("No unique bean of type [" + type.getName() + "] is defined: " + message);
		this.beanType = type;
	}
	
	public String getBeanName() {
		return beanName;
	}
	
	public Class getBeanType() {
		return beanType;
	}
}
