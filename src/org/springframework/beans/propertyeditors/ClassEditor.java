package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

public class ClassEditor extends PropertyEditorSupport{
	
	public void setAsText(String text) throws IllegalArgumentException {
		Class clazz = null;
		try {
			clazz = Class.forName(text, true, Thread.currentThread().getContextClassLoader());
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Invalid class name [" + text + "]: " + ex.getMessage());
		}
		setValue(clazz);
	}
	
	public String getAsText() {
		return ((Class) getValue()).getName();
	}
}
