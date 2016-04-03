package org.springframework.beans.factory.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.util.StringUtils;

public class StaticListableBeanFactory implements ListableBeanFactory{
	
	private Map beans = new HashMap();
	
	public Object getBean(String name) throws BeansException {
		Object bean = this.beans.get(name);
		if (bean instanceof FactoryBean) {
			try {
				return ((FactoryBean) bean).getObject();
			}
			catch (Exception ex) {
				throw new BeanCreationException("FactoryBean threw exception on object creation", ex);
			}
		}
		if (bean == null)
			throw new NoSuchBeanDefinitionException(name, "defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
		return bean;
	}
	
	public Object getBean(String name, Class requiredType) throws BeansException {
		Object bean = getBean(name);
		if (!requiredType.isAssignableFrom(bean.getClass())) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean);
		}
		return bean;
	}
	
	public boolean containsBean(String name) {
		return this.beans.containsKey(name);
	}
	
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		Object bean = getBean(name);
		if (bean instanceof FactoryBean) {
			return ((FactoryBean) bean).isSingleton();
		}
		else {
			return true;
		}
	}
	
	public String[] getAliases(String name) {
		return null;
	}

	public int getBeanDefinitionCount() {
		return this.beans.size();
	}
	
	//key  = bean 이름
	public String[] getBeanDefinitionNames() {
		return (String[]) this.beans.keySet().toArray(new String[this.beans.keySet().size()]);
	}
	
	//type 일치하는 것만 가져오기
	public String[] getBeanDefinitionNames(Class type) {
		List matches = new LinkedList();
		Set keys = this.beans.keySet();
		Iterator itr = keys.iterator();
		while (itr.hasNext()) {
			String name = (String) itr.next();
			Class clazz = this.beans.get(name).getClass();
			if (type.isAssignableFrom(clazz)) {
				matches.add(name);
			}
		}
		return (String[]) matches.toArray(new String[matches.size()]);
	}

	public boolean containsBeanDefinition(String name) {
		return this.beans.containsKey(name);
	}
	
	//FactoryBean객체 검사 할때만 includePrototypes,includeFactoryBeans 값을 사용한다
	//FactoryBean 객체에서 검사기 위한 메소드로 추정
	public Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans) {
		Map matches = new HashMap();
		Set keys = this.beans.keySet();
		Iterator itr = keys.iterator();
		while (itr.hasNext()) {
			String name = (String) itr.next();
			Object bean = this.beans.get(name);
			if (bean instanceof FactoryBean && includeFactoryBeans) {
				FactoryBean factory = (FactoryBean) bean;
				Class objectType = factory.getObjectType();
				
				//FactoryBean 에 getObjectType가 null인 객체????
				if ((objectType == null && factory.isSingleton()) || ((factory.isSingleton() || includePrototypes) && objectType != null && type.isAssignableFrom(objectType))) {
					Object createdObject = getBean(name);
					//같은 class인지 검사하기
					if (type.isInstance(createdObject)) {
						matches.put(name, createdObject);
					}
				}
			}
			else if (type.isAssignableFrom(bean.getClass())) {
				matches.put(name, bean);
			}
		}
		return matches;
	}
	
	public void addBean(String name, Object bean) {
		this.beans.put(name, bean);
	}
}
