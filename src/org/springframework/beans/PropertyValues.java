package org.springframework.beans;

public interface PropertyValues {
	PropertyValue[] getPropertyValues();
	PropertyValue getPropertyValue(String propertyName);
	boolean contains(String propertyName);
	PropertyValues changesSince(PropertyValues old);
}
