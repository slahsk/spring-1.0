package org.springframework.beans.factory;

public interface BeanFactory {
	Object getObject() throws Exception;
	Class getObjectType();
	boolean isSingleton();
}
