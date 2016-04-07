package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;

public class ApplicationContextAwareProcessor implements BeanPostProcessor{
	protected final Log logger = LogFactory.getLog(getClass());

	private final ApplicationContext applicationContext;
	
	public ApplicationContextAwareProcessor(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
		if (bean instanceof ResourceLoaderAware) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking setResourceLoader on ResourceLoaderAware bean '" + name + "'");
			}
			((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
		}
		if (bean instanceof ApplicationContextAware) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking setApplicationContext on ApplicationContextAware bean '" + name + "'");
			}
			((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
		}
		return bean;
	}
	
	public Object postProcessAfterInitialization(Object bean, String name) {
		return bean;
	}
}
