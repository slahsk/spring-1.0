package org.springframework.beans.factory.config;

import org.springframework.beans.MutablePropertyValues;

public interface BeanDefinition {
	MutablePropertyValues getPropertyValues();
	ConstructorArgumentValues getConstructorArgumentValues();
	String getResourceDescription();
}
