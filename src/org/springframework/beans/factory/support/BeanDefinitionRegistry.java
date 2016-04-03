package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;

public interface BeanDefinitionRegistry {
	int getBeanDefinitionCount();
	
	String[] getBeanDefinitionNames();
	
	boolean containsBeanDefinition(String name);
	
	BeanDefinition getBeanDefinition(String name) throws BeansException;
}
