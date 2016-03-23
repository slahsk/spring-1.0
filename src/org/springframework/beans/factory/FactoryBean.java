package org.springframework.beans.factory;

public interface FactoryBean {
	Object getObject() throws Exception;
	Class getObjectType();
	boolean isSingleton();
}
