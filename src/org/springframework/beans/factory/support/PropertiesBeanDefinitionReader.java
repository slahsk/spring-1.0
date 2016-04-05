package org.springframework.beans.factory.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.io.Resource;

public class PropertiesBeanDefinitionReader extends AbstractBeanDefinitionReader{
	
	public static final String TRUE_VALUE = "true";
	
	public static final String SEPARATOR = ".";
	
	public static final String CLASS_KEY = "class";
	
	public static final String SINGLETON_KEY = "(singleton)";
	
	public static final String LAZY_INIT_KEY = "(lazy-init)";
	
	public static final String PARENT_KEY = "parent";
	
	public static final String REF_SUFFIX = "(ref)";
	
	public static final String REF_PREFIX = "*";
	
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	private String defaultParentBean;

	public PropertiesBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
		super(beanFactory);
	}
	
	public void setDefaultParentBean(String defaultParentBean) {
		this.defaultParentBean = defaultParentBean;
	}
	
	public String getDefaultParentBean() {
		return defaultParentBean;
	}
	
	public int loadBeanDefinitions(Resource resource) {
		return loadBeanDefinitions(resource, null);
	}
	
	public int loadBeanDefinitions(Resource resource, String prefix) {
		Properties props = new Properties();
		try {
			InputStream is = resource.getInputStream();
			try {
				props.load(is);
			}
			finally {
				is.close();
			}
			return registerBeanDefinitions(props, prefix, resource.getDescription());
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("IOException parsing properties from " + resource, ex);
		}
	}
	
	public int registerBeanDefinitions(ResourceBundle rb) throws BeanDefinitionStoreException {
		return registerBeanDefinitions(rb, null);
	}
	
	public int registerBeanDefinitions(ResourceBundle rb, String prefix) throws BeanDefinitionStoreException {
		Map m = new HashMap();
		Enumeration keys = rb.getKeys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			m.put(key, rb.getObject(key));
		}
		return registerBeanDefinitions(m, prefix);
	}
	
	public int registerBeanDefinitions(Map m) throws BeansException {
		return registerBeanDefinitions(m, null);
	}
	
	public int registerBeanDefinitions(Map m, String prefix) throws BeansException {
		return registerBeanDefinitions(m, prefix, "(no description)");
	}
	
	public int registerBeanDefinitions(Map m, String prefix, String resourceDescription) throws BeansException {
		if (prefix == null) {
			prefix = "";
		}
		int beanCount = 0;

		Set keys = m.keySet();
		Iterator itr = keys.iterator();
		while (itr.hasNext()) {
			String key = (String) itr.next();
			//맵 키가 prefix 일치하는것만 
			if (key.startsWith(prefix)) {
				String nameAndProperty = key.substring(prefix.length());
				int sepIndx = nameAndProperty.indexOf(SEPARATOR);
				//.이 포함 되어 있으면 앞부분이 beanName 이다
				if (sepIndx != -1) {
					String beanName = nameAndProperty.substring(0, sepIndx);
					logger.debug("Found bean name '" + beanName + "'");
					//bean 이 등록 안되어 있으면 등록
					if (!getBeanFactory().containsBeanDefinition(beanName)) {
						registerBeanDefinition(beanName, m, prefix + beanName, resourceDescription);
						++beanCount;
					}
				}
				else {
					logger.debug("Invalid bean name and property [" + nameAndProperty + "]");
				}
			}
		}

		return beanCount;
	}
	
	protected void registerBeanDefinition(String beanName, Map m, String prefix, String resourceDescription)
			throws BeansException {
		String className = null;
		String parent = null;
		boolean singleton = true;
		boolean lazyInit = false;

		MutablePropertyValues pvs = new MutablePropertyValues();
		Set keys = m.keySet();
		Iterator itr = keys.iterator();
		while (itr.hasNext()) {
			String key = (String) itr.next();
			if (key.startsWith(prefix + SEPARATOR)) {
				String property = key.substring(prefix.length() + SEPARATOR.length());
				if (property.equals(CLASS_KEY)) {
					className = (String) m.get(key);
				}
				else if (property.equals(SINGLETON_KEY)) {
					String val = (String) m.get(key);
					singleton = (val == null) || val.equals(TRUE_VALUE);
				}
				else if (property.equals(LAZY_INIT_KEY)) {
					String val = (String) m.get(key);
					lazyInit = val.equals(TRUE_VALUE);
				}
				else if (property.equals(PARENT_KEY)) {
					parent = (String) m.get(key);
				}
				else if (property.endsWith(REF_SUFFIX)) {
					property = property.substring(0, property.length() - REF_SUFFIX.length());
					String ref = (String) m.get(key);

					Object val = new RuntimeBeanReference(ref);
					pvs.addPropertyValue(new PropertyValue(property, val));
				}
				else{
					Object val = m.get(key);
					if (val instanceof String) {
						String strVal = (String) val;
						if (strVal.startsWith(REF_PREFIX)) {
							String targetName = strVal.substring(1);
							if (targetName.startsWith(REF_PREFIX)) {
								val = targetName;
							}
							else {
								val = new RuntimeBeanReference(targetName);
							}
						}
					}
					pvs.addPropertyValue(new PropertyValue(property, val));
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(pvs.toString());
		}

		if (parent == null) {
			parent = this.defaultParentBean;
		}

		if (className == null && parent == null) {
			throw new BeanDefinitionStoreException(resourceDescription, beanName,
																						 "Either 'class' or 'parent' is required");
		}

		try {
			AbstractBeanDefinition beanDefinition = null;
			//className 없으면 class 정보 불러오기
			if (className != null) {
				Class clazz = Class.forName(className, true, getBeanClassLoader());
				beanDefinition = new RootBeanDefinition(clazz, pvs);
			}
			else {
				beanDefinition = new ChildBeanDefinition(parent, pvs);
			}

			beanDefinition.setSingleton(singleton);
			beanDefinition.setLazyInit(lazyInit);
			getBeanFactory().registerBeanDefinition(beanName, beanDefinition);
		}
		catch (ClassNotFoundException ex) {
			throw new BeanDefinitionStoreException(resourceDescription, beanName, "Class [" + className + "] not found", ex);
		}
	}

}
