package org.springframework.beans.factory.xml;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;

public interface XmlBeanDefinitionParser {
	
	void registerBeanDefinitions(BeanDefinitionRegistry beanFactory, ClassLoader beanClassLoader,
			 Document doc, Resource resource) throws BeansException;

}
