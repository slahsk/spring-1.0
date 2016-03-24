package org.springframework.beans.factory;

public class FactoryBeanCircularReferenceException extends BeanDefinitionStoreException{
	public FactoryBeanCircularReferenceException(String msg) {
		super(msg);
	}
}
