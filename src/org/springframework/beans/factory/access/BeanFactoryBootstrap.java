package org.springframework.beans.factory.access;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;

public class BeanFactoryBootstrap {
	public static final String BEAN_FACTORY_BEAN_NAME = "bootstrapBeanFactory";
	
	private static BeanFactoryBootstrap instance;
	
	private static BeansException startupException;
	
	private static void initializeSingleton() {
		try {
			instance = new BeanFactoryBootstrap();
		}
		catch (BeansException ex) {
			startupException = ex;
		}
	}
	
	//객체 생성과 동시에 싱글톤 객체 생성
	static {
		initializeSingleton();
	}
	
	public static BeanFactoryBootstrap getInstance() throws BeansException {
		if (startupException != null)
			throw startupException;
		// Really an assertion
		if (instance == null)
			throw new BootstrapException("Anomaly: instance and exception null", null);
		return instance;
	}
	
	//리로드
	protected static void reinitialize() {
		instance = null;
		startupException = null;
		initializeSingleton();
	}
	
	private BeanFactory bootstrapFactory;
	
	private BeanFactoryBootstrap() throws BeansException {
		DefaultListableBeanFactory startupFactory = new DefaultListableBeanFactory();
		PropertiesBeanDefinitionReader propReader = new PropertiesBeanDefinitionReader(startupFactory);
		try {
			propReader.registerBeanDefinitions(System.getProperties());
			this.bootstrapFactory = (BeanFactory) startupFactory.getBean(BEAN_FACTORY_BEAN_NAME);
		}
		catch (ClassCastException ex) {
			throw new BootstrapException("Bootstrap bean factory class does not implement BeanFactory interface", ex);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new BootstrapException("No bean named '" + BEAN_FACTORY_BEAN_NAME + "' in system properties: [" + startupFactory + "]", null);
		}
		catch (BeansException ex) {
			throw new BootstrapException("Failed to bootstrap bean factory", ex);
		}
	}
}
