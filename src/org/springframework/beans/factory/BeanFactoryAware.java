package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

public interface BeanFactoryAware {
	void setBeanFactory(BeanFactory beanFactory) throws BeansException;
}
