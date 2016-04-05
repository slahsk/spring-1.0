package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;

public interface BeanDefinitionRegistry {
	int getBeanDefinitionCount();
	
	String[] getBeanDefinitionNames();
	
	boolean containsBeanDefinition(String name);
	
	BeanDefinition getBeanDefinition(String name) throws BeansException;
	
	void registerBeanDefinition(String name, BeanDefinition beanDefinition)
			throws BeansException;
	
	String[] getAliases(String name) throws NoSuchBeanDefinitionException;
	
	void registerAlias(String name, String alias) throws BeansException;
}
