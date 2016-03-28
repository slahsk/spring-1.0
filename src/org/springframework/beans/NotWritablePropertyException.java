package org.springframework.beans;

public class NotWritablePropertyException extends FatalBeanException{
	
	public NotWritablePropertyException(String propertyName, Class beanClass) {
		super("Property '" + propertyName + "' is not writable in bean class [" + beanClass.getName() + "]");
	}
	
	public NotWritablePropertyException(String propertyName, Class beanClass, Throwable ex) {
		super("Property '" + propertyName + "' is not writable in bean class [" + beanClass.getName() + "]", ex);
	}



}
