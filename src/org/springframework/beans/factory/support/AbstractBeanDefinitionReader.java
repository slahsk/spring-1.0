package org.springframework.beans.factory.support;

public abstract class AbstractBeanDefinitionReader {
	private BeanDefinitionRegistry beanFactory;
	
	private ClassLoader beanClassLoader = Thread.currentThread().getContextClassLoader();
	
	protected AbstractBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
		this.beanFactory = beanFactory;
	}
	
	public BeanDefinitionRegistry getBeanFactory() {
		return beanFactory;
	}
	
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}
	
	public ClassLoader getBeanClassLoader() {
		return beanClassLoader;
	}

}
