package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CachedIntrospectionResults {
	private static final Log logger = LogFactory.getLog(CachedIntrospectionResults.class);
	
	//싱글톤으로 관리
	private static HashMap classCache = new HashMap();
	
	protected static CachedIntrospectionResults forClass(Class clazz) throws BeansException {
		Object results = classCache.get(clazz);
		//없으면 새로 생성
		if (results == null) {
			// can throw BeansException
			results = new CachedIntrospectionResults(clazz);
			classCache.put(clazz, results);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Using cached introspection results for class " + clazz.getName());
			}
		}
		return (CachedIntrospectionResults) results;
	}
	
	private BeanInfo beanInfo;
	
	private Map propertyDescriptorMap;
	
	private CachedIntrospectionResults(Class clazz) throws FatalBeanException {
		try {
			logger.debug("Getting BeanInfo for class [" + clazz.getName() + "]");
			this.beanInfo = Introspector.getBeanInfo(clazz);

			logger.debug("Caching PropertyDescriptors for class [" + clazz.getName() + "]");
			this.propertyDescriptorMap = new HashMap();
			
			//object 속성 가져오기????
			PropertyDescriptor[] pds = this.beanInfo.getPropertyDescriptors();
			for (int i = 0; i < pds.length; i++) {
				logger.debug("Found property '" + pds[i].getName() + "' of type [" + pds[i].getPropertyType() +"]; editor=[" + pds[i].getPropertyEditorClass() + "]");
				this.propertyDescriptorMap.put(pds[i].getName(), pds[i]);
			}
		}
		catch (IntrospectionException ex) {
			throw new FatalBeanException("Cannot get BeanInfo for object of class [" + clazz.getName() + "]", ex);
		}
	}
	
	protected BeanInfo getBeanInfo() {
		return beanInfo;
	}

	protected Class getBeanClass() {
		return beanInfo.getBeanDescriptor().getBeanClass();
	}
	
	protected PropertyDescriptor getPropertyDescriptor(String propertyName) throws BeansException {
		PropertyDescriptor pd = (PropertyDescriptor) this.propertyDescriptorMap.get(propertyName);
		if (pd == null) {
			throw new FatalBeanException("No property '" + propertyName + "' in class [" + getBeanClass().getName() + "]", null);
		}
		return pd;
	}
}
