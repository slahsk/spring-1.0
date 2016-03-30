package org.springframework.beans.factory;

public class UnsatisfiedDependencyException extends BeanDefinitionStoreException{
	public UnsatisfiedDependencyException(String beanName, int ctorArgIndex, Class ctorArgType, String msg) {
		super("Bean with name '" + beanName + "' has an unsatisfied dependency expressed through " +
					"constructor argument with index " + ctorArgIndex + " of type [" + ctorArgType.getName() + "]" +
					(msg != null ? ": " + msg : ""));
	}
	
	public UnsatisfiedDependencyException(String beanName, String propertyName, String msg) {
		super("Bean with name '" + beanName + "' has an unsatisfied dependency expressed through property '" +
					propertyName + "': set this property value or disable dependency checking for this bean" +
					(msg != null ? ": " + msg : ""));
	}
}
