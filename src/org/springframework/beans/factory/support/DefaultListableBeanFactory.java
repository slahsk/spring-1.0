package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanCircularReferenceException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringUtils;


public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements ConfigurableListableBeanFactory, BeanDefinitionRegistry{
	
	private boolean allowBeanDefinitionOverriding = true;
	
	private Map beanDefinitionMap = new HashMap();
	
	private List beanDefinitionNames = new ArrayList();
	
	public DefaultListableBeanFactory() {
		super();
	}
	
	public DefaultListableBeanFactory(BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}
	
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}
	
	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}
	
	public String[] getBeanDefinitionNames() {
		return getBeanDefinitionNames(null);
	}
	
	public String[] getBeanDefinitionNames(Class type) {
		List matches = new ArrayList();
		Iterator it = this.beanDefinitionNames.iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			//null 이거나 definition 에서 객체 찾아서 같은 타입
			if (type == null || type.isAssignableFrom(getMergedBeanDefinition(name, false).getBeanClass())) {
				matches.add(name);
			}
		}
		return (String[]) matches.toArray(new String[matches.size()]);
	}
	
	public boolean containsBeanDefinition(String name) {
		return this.beanDefinitionMap.containsKey(name);
	}
	
	//3곳에서 모든 타입 가져오기
	public Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans)
			throws BeansException {
		//defintion 객체
		String[] beanNames = getBeanDefinitionNames(type);
		Map result = new HashMap();
		for (int i = 0; i < beanNames.length; i++) {
			if (includePrototypes || isSingleton(beanNames[i])) {
				result.put(beanNames[i], getBean(beanNames[i]));
			}
		}
		
		//스프링에서 관리하는 싱글톤객체
		String[] singletonNames = getSingletonNames(type);
		for (int i = 0; i < singletonNames.length; i++) {
			result.put(singletonNames[i], getBean(singletonNames[i]));
		}
		
		//Factory
		if (includeFactoryBeans) {
			String[] factoryNames = getBeanDefinitionNames(FactoryBean.class);
			for (int i = 0; i < factoryNames.length; i++) {
				try {
					FactoryBean factory = (FactoryBean) getBean(FACTORY_BEAN_PREFIX + factoryNames[i]);
					Class objectType = factory.getObjectType();
					if ((objectType == null && factory.isSingleton()) ||
							((factory.isSingleton() || includePrototypes) &&
							objectType != null && type.isAssignableFrom(objectType))) {
						Object createdObject = getBean(factoryNames[i]);
						if (type.isInstance(createdObject)) {
							result.put(factoryNames[i], createdObject);
						}
					}
				}
				catch (FactoryBeanCircularReferenceException ex) {
					logger.debug("Ignoring exception on FactoryBean type check", ex);
				}
			}
		}

		return result;
	}
	
	//객체 생성 안되어 있는거 생성 시키기??
	public void preInstantiateSingletons() {
		if (logger.isInfoEnabled()) {
			logger.info("Pre-instantiating singletons in factory [" + this + "]");
		}
		for (Iterator it = this.beanDefinitionNames.iterator(); it.hasNext();) {
			String beanName = (String) it.next();
			if (containsBeanDefinition(beanName)) {
				RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
				if (bd.isSingleton() && !bd.isLazyInit()) {
					//FactoryBean 객체이면
					if (FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
						FactoryBean factory = (FactoryBean) getBean(FACTORY_BEAN_PREFIX + beanName);
						if (factory.isSingleton()) {
							getBean(beanName);
						}
					}
					else {
						getBean(beanName);
					}
				}
			}
		}
	}
	
	public void registerBeanDefinition(String name, BeanDefinition beanDefinition)throws BeanDefinitionStoreException {
		
		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), name,
				                                       "Validation of bean definition with name failed", ex);
			}
		}
		
		Object oldBeanDefinition = this.beanDefinitionMap.get(name);
		//같은 이름의 defintion이 있으면
		if (oldBeanDefinition != null) {
			if (!this.allowBeanDefinitionOverriding) {
				throw new BeanDefinitionStoreException("Cannot register bean definition [" + beanDefinition + "] for bean '" +
																							 name + "': there's already [" + oldBeanDefinition + "] bound");
			}
			else {
				logger.info("Overriding bean definition for bean '" + name +
										"': replacing [" + oldBeanDefinition + "] with [" + beanDefinition + "]");
			}
		}
		else {
			//이름 저장
			this.beanDefinitionNames.add(name);
		}
		
		//객체 저장
		this.beanDefinitionMap.put(name, beanDefinition);
	}
	
	public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
		BeanDefinition bd = (BeanDefinition) this.beanDefinitionMap.get(beanName);
		if (bd == null) {
			throw new NoSuchBeanDefinitionException(beanName, toString());
		}
		return bd;
	}
	
	protected String[] getDependingBeanNames(String beanName) throws BeansException {
		List dependingBeanNames = new ArrayList();
		String[] beanDefinitionNames = getBeanDefinitionNames();
		for (int i = 0; i < beanDefinitionNames.length; i++) {
			if (containsBeanDefinition(beanDefinitionNames[i])) {
				RootBeanDefinition bd = getMergedBeanDefinition(beanDefinitionNames[i], false);
				if (bd.getDependsOn() != null) {
					List dependsOn = Arrays.asList(bd.getDependsOn());
					if (dependsOn.contains(beanName)) {
						logger.debug("Found depending bean '" + beanDefinitionNames[i] + "' for bean '" + beanName + "'");
						dependingBeanNames.add(beanDefinitionNames[i]);
					}
				}
			}
		}
		return (String[]) dependingBeanNames.toArray(new String[dependingBeanNames.size()]);
	}
	
	//계층 구조이면 부모에서도 빈 찾기
	protected Map findMatchingBeans(Class requiredType) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(this, requiredType, true, true);
	}


	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getName());
		sb.append(" defining beans [" + StringUtils.arrayToDelimitedString(getBeanDefinitionNames(), ",") + "]");
		if (getParentBeanFactory() == null) {
			sb.append("; Root of BeanFactory hierarchy");
		}
		else {
			sb.append("; parent=<" + getParentBeanFactory() + ">");
		}
		return sb.toString();
	}
}
