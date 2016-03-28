package org.springframework.beans;

public class NullValueInNestedPathException extends FatalBeanException{
	
	private String property;

	private Class clazz;
	
	public NullValueInNestedPathException(Class clazz, String propertyName) {
		super("Value of nested property '" + propertyName + "' is null in " + clazz, null);
		this.property = propertyName;
		this.clazz = clazz;
	}
	
	public String getPropertyName() {
		return property;
	}

	public Class getBeanClass() {
		return clazz;
	}

}
