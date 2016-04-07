package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;

public class AbstractXmlApplicationContext extends AbstractApplicationContext{
	
	private ConfigurableListableBeanFactory beanFactory;
	
	public AbstractXmlApplicationContext() {
	}
	
	public AbstractXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}
	
	protected void refreshBeanFactory() throws BeansException {
		try {
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
			beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
			initBeanDefinitionReader(beanDefinitionReader);
			loadBeanDefinitions(beanDefinitionReader);
			this.beanFactory = beanFactory;
			if (logger.isInfoEnabled()) {
				logger.info("Bean factory for application context '" + getDisplayName() + "': " + beanFactory);
			}
		}
		catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing XML document for application context [" +
			                                      getDisplayName() + "]", ex);
		} 
	}

}
