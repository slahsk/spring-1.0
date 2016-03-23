package org.springframework.beans.factory;

public class BeanIsNotAFactoryException extends BeanNotOfRequiredTypeException{
	
	public BeanIsNotAFactoryException(String name, Object actualInstance) {
		super(name, FactoryBean.class, actualInstance);
	}

}
