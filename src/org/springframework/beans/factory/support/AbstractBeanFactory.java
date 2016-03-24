package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanCircularReferenceException;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

public abstract class AbstractBeanFactory implements ConfigurableBeanFactory, HierarchicalBeanFactory{
	public static final String FACTORY_BEAN_PREFIX = "&";
	
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	private BeanFactory parentBeanFactory;
	private Map customEditors = new HashMap();
	private final Set ignoreDependencyTypes = new HashSet();
	private final List beanPostProcessors = new ArrayList();
	private final Map aliasMap = Collections.synchronizedMap(new HashMap());
	private final Map singletonCache = Collections.synchronizedMap(new HashMap());
	
	public AbstractBeanFactory() {
		ignoreDependencyType(BeanFactory.class);
	}
	
	public AbstractBeanFactory(BeanFactory parentBeanFactory) {
		this();
		this.parentBeanFactory = parentBeanFactory;
	}
	
	public Object getBean(String name) throws BeansException {
		String beanName = transformedBeanName(name);
		Object sharedInstance = this.singletonCache.get(beanName);
		
		
		if (sharedInstance != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
			}
			return getObjectForSharedInstance(name, sharedInstance);
		}
		else {
			RootBeanDefinition mergedBeanDefinition = null;
			try {
				mergedBeanDefinition = getMergedBeanDefinition(beanName, false);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// not found -> check parent
				if (this.parentBeanFactory != null) {
					return this.parentBeanFactory.getBean(name);
				}
				throw ex;
			}
			// create bean instance
			if (mergedBeanDefinition.isSingleton()) {
				synchronized (this.singletonCache) {
					// re-check singleton cache within synchronized block
					sharedInstance = this.singletonCache.get(beanName);
					if (sharedInstance == null) {
						logger.info("Creating shared instance of singleton bean '" + beanName + "'");
						sharedInstance = createBean(beanName, mergedBeanDefinition);
						addSingleton(beanName, sharedInstance);
					}
				}
				return getObjectForSharedInstance(name, sharedInstance);
			}
			else {
				return createBean(name, mergedBeanDefinition);
			}
		}
	}
	
	protected String transformedBeanName(String name) throws NoSuchBeanDefinitionException {
		if (name == null) {
			throw new NoSuchBeanDefinitionException(name, "Cannot get bean with null name");
		}
		
		//prefix 제거
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			name = name.substring(FACTORY_BEAN_PREFIX.length());
		}
		
		//alias 있으면 대신 가져오기
		String canonicalName = (String) this.aliasMap.get(name);
		return canonicalName != null ? canonicalName : name;
	}
	
	protected Object getObjectForSharedInstance(String name, Object beanInstance) {
		String beanName = transformedBeanName(name);

		//& 이름이면서 FactoryBean 아니면
		if (isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(beanName, beanInstance);
		}

		if (beanInstance instanceof FactoryBean) {
			//factoryBean 이지만 이름이 &없는경우
			if (!isFactoryDereference(name)) {
				FactoryBean factory = (FactoryBean) beanInstance;
				logger.debug("Bean with name '" + beanName + "' is a factory bean");
				try {
					beanInstance = factory.getObject();
				}
				catch (BeansException ex) {
					throw ex;
				}
				catch (Exception ex) {
					throw new BeanCreationException("FactoryBean threw exception on object creation", ex);
				}
				
				if (beanInstance == null) {
					throw new FactoryBeanCircularReferenceException(
					    "Factory bean '" + beanName + "' returned null object - " +
					    "possible cause: not fully initialized due to circular bean reference");
				}
			}
			else {
				logger.debug("Calling code asked for FactoryBean instance for name '" + beanName + "'");
			}
		}

		return beanInstance;
	}
	
	protected boolean isFactoryDereference(String name) {
		return name.startsWith(FACTORY_BEAN_PREFIX);
	}
	
	public RootBeanDefinition getMergedBeanDefinition(String beanName, boolean includingAncestors)
		    throws BeansException {
			try {
				return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
			}
			catch (NoSuchBeanDefinitionException ex) {
				if (includingAncestors && getParentBeanFactory() instanceof AbstractAutowireCapableBeanFactory) {
					return ((AbstractAutowireCapableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName, true);
				}
				else {
					throw ex;
				}
			}
		}

	
}
