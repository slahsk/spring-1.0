package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

public class RootBeanDefinition extends AbstractBeanDefinition{
	
	public static final int AUTOWIRE_NO = 0;
	public static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
	public static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;
	public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;
	public static final int AUTOWIRE_AUTODETECT = AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT;
	public static final int DEPENDENCY_CHECK_NONE = 0;
	public static final int DEPENDENCY_CHECK_OBJECTS = 1;
	public static final int DEPENDENCY_CHECK_SIMPLE = 2;
	public static final int DEPENDENCY_CHECK_ALL = 3;
	private Object beanClass;
	private ConstructorArgumentValues constructorArgumentValues;
	private int autowireMode = AUTOWIRE_NO;
	private int dependencyCheck = DEPENDENCY_CHECK_NONE;
	private String[] dependsOn;
	private String initMethodName;
	private String destroyMethodName;
	
	public RootBeanDefinition(Class beanClass, int autowireMode) {
		super(null);
		this.beanClass = beanClass;
		setAutowireMode(autowireMode);
	}
	
	public RootBeanDefinition(Class beanClass, int autowireMode, boolean dependencyCheck) {
		super(null);
		this.beanClass = beanClass;
		setAutowireMode(autowireMode);
		//자동 모드 체크
		if (dependencyCheck && getResolvedAutowireMode() != AUTOWIRE_CONSTRUCTOR) {
			setDependencyCheck(RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
		}
	}
	
	public RootBeanDefinition(Class beanClass, MutablePropertyValues pvs) {
		super(pvs);
		this.beanClass = beanClass;
	}
	
	public RootBeanDefinition(Class beanClass, MutablePropertyValues pvs, boolean singleton) {
		super(pvs);
		this.beanClass = beanClass;
		setSingleton(singleton);
	}
	
	public RootBeanDefinition(Class beanClass, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		super(pvs);
		this.beanClass = beanClass;
		this.constructorArgumentValues = cargs;
	}
	
	public RootBeanDefinition(String beanClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
		super(pvs);
		this.beanClass = beanClassName;
		this.constructorArgumentValues = cargs;
	}

	public RootBeanDefinition(RootBeanDefinition other) {
		super(new MutablePropertyValues(other.getPropertyValues()));
		this.beanClass = other.beanClass;
		this.constructorArgumentValues = other.constructorArgumentValues;
		setSingleton(other.isSingleton());
		setLazyInit(other.isLazyInit());
		setDependsOn(other.getDependsOn());
		setDependencyCheck(other.getDependencyCheck());
		setAutowireMode(other.getAutowireMode());
		setInitMethodName(other.getInitMethodName());
		setDestroyMethodName(other.getDestroyMethodName());
	}
	
	public ConstructorArgumentValues getConstructorArgumentValues() {
		return constructorArgumentValues;
	}
	
	public boolean hasConstructorArgumentValues() {
		return (constructorArgumentValues != null && !constructorArgumentValues.isEmpty());
	}
	
	public final Class getBeanClass() throws IllegalStateException {
		if (!(this.beanClass instanceof Class)) {
			throw new IllegalStateException("Bean definition does not carry a resolved bean class");
		}
		return (Class) this.beanClass;
	}
	
	public final String getBeanClassName() {
		if (this.beanClass instanceof Class) {
			return ((Class) this.beanClass).getName();
		}
		else {
			return (String) this.beanClass;
		}
	}
	
	public void setAutowireMode(int autowireMode) {
		this.autowireMode = autowireMode;
	}
	
	public int getAutowireMode() {
		return autowireMode;
	}
	
	public int getResolvedAutowireMode() {
		//자동이면
		if (this.autowireMode == AUTOWIRE_AUTODETECT) {
			Constructor[] constructors = getBeanClass().getConstructors();
			//생성자 개수 만큼
			for (int i = 0; i < constructors.length; i++) {
				//기본 생성자가 있으면
				if (constructors[i].getParameterTypes().length == 0) {
					return AUTOWIRE_BY_TYPE;
				}
			}
			return AUTOWIRE_CONSTRUCTOR;
		}
		else {
			return this.autowireMode;
		}
	}
	
	public void setDependencyCheck(int dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}
	
	public int getDependencyCheck() {
		return dependencyCheck;
	}
	
	public void setDependsOn(String[] dependsOn) {
		this.dependsOn = dependsOn;
	}
	
	public String[] getDependsOn() {
		return dependsOn;
	}
	
	public void setInitMethodName(String initMethodName) {
		this.initMethodName = initMethodName;
	}
	
	public String getInitMethodName() {
		return this.initMethodName;
	}
	
	public void setDestroyMethodName(String destroyMethodName) {
		this.destroyMethodName = destroyMethodName;
	}
	
	public String getDestroyMethodName() {
		return this.destroyMethodName;
	}
	
	public void validate() throws BeanDefinitionValidationException {
		super.validate();
		if (this.beanClass == null) {
			throw new BeanDefinitionValidationException("beanClass must be set in RootBeanDefinition");
		}
		if (this.beanClass instanceof Class) {
			if (FactoryBean.class.isAssignableFrom(getBeanClass()) && !isSingleton()) {
				throw new BeanDefinitionValidationException("FactoryBean must be defined as singleton - " +
																										"FactoryBeans themselves are not allowed to be prototypes");
			}
			if (getBeanClass().getConstructors().length == 0) {
				throw new BeanDefinitionValidationException("No public constructor in class [" + getBeanClass() + "]");
			}
		}
	}

	public String toString() {
		return "Root bean with class [" + getBeanClassName() + "] defined in " + getResourceDescription();
	}
}
