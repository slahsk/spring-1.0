package org.springframework.beans.factory.xml;

import java.io.InputStream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

public class XmlBeanFactory extends DefaultListableBeanFactory{
	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);
	
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}
	
	public XmlBeanFactory(InputStream is) throws BeansException {
		this(new InputStreamResource(is, "(no description)"), null);
	}
	
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException {
		super(parentBeanFactory);
		this.reader.loadBeanDefinitions(resource);
	}

}
