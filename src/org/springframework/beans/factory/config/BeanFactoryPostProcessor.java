package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

public interface BeanFactoryPostProcessor {
	
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
