package org.springframework.beans.factory.support;

import org.springframework.beans.MutablePropertyValues;

public class ChildBeanDefinition extends AbstractBeanDefinition{
	
	private String parentName;
	
	public ChildBeanDefinition(String parentName, MutablePropertyValues pvs) {
		super(pvs);
		this.parentName = parentName;
	}
	
	public String getParentName() {
		return parentName;
	}
	
	public void validate() throws BeanDefinitionValidationException {
		super.validate();
		if (this.parentName == null) {
			throw new BeanDefinitionValidationException("parentName must be set in ChildBeanDefinition");
		}
	}

	public String toString() {
		return "Child bean with parent '" + getParentName() + "' defined in " + getResourceDescription();
	}

}
