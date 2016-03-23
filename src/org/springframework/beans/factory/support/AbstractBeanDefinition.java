package org.springframework.beans.factory.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

public abstract class AbstractBeanDefinition implements BeanDefinition{
	
	private MutablePropertyValues propertyValues;
	private String resourceDescription;
	private boolean singleton = true;
	private boolean lazyInit = false;
	
	protected AbstractBeanDefinition(MutablePropertyValues pvs) {
		this.propertyValues = (pvs != null) ? pvs : new MutablePropertyValues();
	}
	
	public MutablePropertyValues getPropertyValues() {
		return propertyValues;
	}
	
	public ConstructorArgumentValues getConstructorArgumentValues() {
		return null;
	}
	
	public void setResourceDescription(String resourceDescription) {
		this.resourceDescription = resourceDescription;
	}
	
	public String getResourceDescription() {
		return resourceDescription;
	}
	
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}
	
	public boolean isSingleton() {
		return singleton;
	}
	
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}
	
	public boolean isLazyInit() {
		return lazyInit;
	}
	
	//¿ÀÁ÷ ½Ì±ÛÅæ¿¡¼­¸¸
	public void validate() throws BeanDefinitionValidationException {
		if (this.lazyInit && !this.singleton) {
			throw new BeanDefinitionValidationException("Lazy initialization is just applicable to singleton beans");
		}
	}

}
