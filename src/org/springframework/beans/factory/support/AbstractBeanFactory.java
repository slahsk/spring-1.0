package org.springframework.beans.factory.support;

import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanCircularReferenceException;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
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
		//BeanFactory 클레스는 의존주입 제외
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
				if (this.parentBeanFactory != null) {
					return this.parentBeanFactory.getBean(name);
				}
				throw ex;
			}
			
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
	
	
	
	public Object getBean(String name, Class requiredType) throws BeansException {
		Object bean = getBean(name);
		if (!requiredType.isAssignableFrom(bean.getClass())) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean);
		}
		return bean;
	}
	
	public boolean containsBean(String name) {
		String beanName = transformedBeanName(name);
		if (this.singletonCache.containsKey(beanName)) {
			return true;
		}
		if (containsBeanDefinition(beanName)) {
			return true;
		}
		else {
			//부모에서 찾기
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.containsBean(beanName);
			}
			else {
				return false;
			}
		}
	}
	
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		try {
			Class beanClass = null;
			boolean singleton = true;
			Object beanInstance = this.singletonCache.get(beanName);
			if (beanInstance != null) {
				beanClass = beanInstance.getClass();
				singleton = true;
			}
			else {
				RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
				beanClass = bd.getBeanClass();
				singleton = bd.isSingleton();
			}
			if (FactoryBean.class.isAssignableFrom(beanClass) && !isFactoryDereference(name)) {
				FactoryBean factoryBean = (FactoryBean) getBean(FACTORY_BEAN_PREFIX + beanName);
				return factoryBean.isSingleton();
			}
			else {
				return singleton;
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.isSingleton(beanName);
			}
			throw ex;
		}
	}
	
	public String[] getAliases(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		if (this.singletonCache.containsKey(beanName) || containsBeanDefinition(beanName)) {
			List aliases = new ArrayList();
			for (Iterator it = this.aliasMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				if (entry.getValue().equals(beanName)) {
					aliases.add(entry.getKey());
				}
			}
			return (String[]) aliases.toArray(new String[aliases.size()]);
		}
		else {
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.getAliases(beanName);
			}
			throw new NoSuchBeanDefinitionException(beanName, toString());
		}
	}
	
	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory
	//---------------------------------------------------------------------

	public BeanFactory getParentBeanFactory() {
		return parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableBeanFactory
	//---------------------------------------------------------------------

	public void setParentBeanFactory(BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}

	public void registerCustomEditor(Class requiredType, PropertyEditor propertyEditor) {
		this.customEditors.put(requiredType, propertyEditor);
	}
	
	protected String transformedBeanName(String name) throws NoSuchBeanDefinitionException {
		if (name == null) {
			throw new NoSuchBeanDefinitionException(name, "Cannot get bean with null name");
		}
		
		//FACTORY_BEAN_PREFIX 값 제거
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			name = name.substring(FACTORY_BEAN_PREFIX.length());
		}
		
		//alias 되어 있는지 확인
		String canonicalName = (String) this.aliasMap.get(name);
		return canonicalName != null ? canonicalName : name;
	}
	
	
	protected Object getObjectForSharedInstance(String name, Object beanInstance) {
		String beanName = transformedBeanName(name);
		
		//isFactoryDereference 이름 검사 & 포함 되어 있으면 TRUE
		//FactoryBean 이 아니면 객체를 생성할수 없다
		if (isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(beanName, beanInstance);
		}
		
		//FactoryBean 검사
		if (beanInstance instanceof FactoryBean) {
			//이름에 & 없으면
			if (!isFactoryDereference(name)) {
				FactoryBean factory = (FactoryBean) beanInstance;
				logger.debug("Bean with name '" + beanName + "' is a factory bean");
				try {
					//객체 가져오기
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
	
	public RootBeanDefinition getMergedBeanDefinition(String beanName, boolean includingAncestors) throws BeansException {
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
	
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd) {
		//root 나올때까지 재귀 호출
		if (bd instanceof RootBeanDefinition) {
			return (RootBeanDefinition) bd;
		}//자식이면
		else if (bd instanceof ChildBeanDefinition) {
			ChildBeanDefinition cbd = (ChildBeanDefinition) bd;
			// 부모에 자식 추가
			RootBeanDefinition rbd = new RootBeanDefinition(getMergedBeanDefinition(cbd.getParentName(), true));
			
			//자식 -> 부모 프로퍼티 추가
			for (int i = 0; i < cbd.getPropertyValues().getPropertyValues().length; i++) {
				rbd.getPropertyValues().addPropertyValue(cbd.getPropertyValues().getPropertyValues()[i]);
			}
			rbd.setSingleton(cbd.isSingleton());
			rbd.setLazyInit(cbd.isLazyInit());
			rbd.setResourceDescription(cbd.getResourceDescription());
			return rbd;
		}
		else {
			throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
																						 "Definition is neither a RootBeanDefinition nor a ChildBeanDefinition");
		}
	}
	
	public Map getCustomEditors() {
		return customEditors;
	}

	public void ignoreDependencyType(Class type) {
		this.ignoreDependencyTypes.add(type);
	}
	
	public Set getIgnoredDependencyTypes() {
		return ignoreDependencyTypes;
	}

	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		this.beanPostProcessors.add(beanPostProcessor);
	}
	
	public List getBeanPostProcessors() {
		return beanPostProcessors;
	}

	public void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException {
		logger.debug("Registering alias '" + alias + "' for bean with name '" + beanName + "'");
		synchronized (this.aliasMap) {
			Object registeredName = this.aliasMap.get(alias);
			if (registeredName != null) {
				throw new BeanDefinitionStoreException("Cannot register alias '" + alias + "' for bean name '" + beanName +
																							 "': it's already registered for bean name '" + registeredName + "'");
			}
			this.aliasMap.put(alias, beanName);
		}
	}

	public void registerSingleton(String beanName, Object singletonObject) throws BeanDefinitionStoreException {
		synchronized (this.singletonCache) {
			Object oldObject = this.singletonCache.get(beanName);
			if (oldObject != null) {
				throw new BeanDefinitionStoreException("Could not register object [" + singletonObject +
																							 "] under bean name '" + beanName + "': there's already object [" +
																							 oldObject + " bound");
			}
			addSingleton(beanName, singletonObject);
		}
	}
	
	protected void addSingleton(String beanName, Object singletonObject) {
		this.singletonCache.put(beanName, singletonObject);
	}

	public void destroySingletons() {
		if (logger.isInfoEnabled()) {
			logger.info("Destroying singletons in factory {" + this + "}");
		}
		synchronized (this.singletonCache) {
			Set singletonCacheKeys = new HashSet(this.singletonCache.keySet());
			for (Iterator it = singletonCacheKeys.iterator(); it.hasNext();) {
				destroySingleton((String) it.next());
			}
		}
	}
	
	//케쉬 에서 빈 제거
	protected void destroySingleton(String beanName) {
		Object singletonInstance = this.singletonCache.remove(beanName);
		if (singletonInstance != null) {
			destroyBean(beanName, singletonInstance);
		}
	}
	
	//editor 등록
	protected void initBeanWrapper(BeanWrapper bw) {
		for (Iterator it = this.customEditors.keySet().iterator(); it.hasNext();) {
			Class clazz = (Class) it.next();
			bw.registerCustomEditor(clazz, (PropertyEditor) this.customEditors.get(clazz));
		}
	}
	
	// 싱글톤 케쉬에서 이름 다 가져오기
	public String[] getSingletonNames(Class type) {
		Set keys = this.singletonCache.keySet();
		Set matches = new HashSet();
		Iterator itr = keys.iterator();
		while (itr.hasNext()) {
			String name = (String) itr.next();
			Object singletonObject = this.singletonCache.get(name);
			if (type == null || type.isAssignableFrom(singletonObject.getClass())) {
				matches.add(name);
			}
		}
		return (String[]) matches.toArray(new String[matches.size()]);
	}
	
	
	
	public abstract boolean containsBeanDefinition(String beanName);
	
	public abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	protected abstract Object createBean(String beanName, RootBeanDefinition mergedBeanDefinition) throws BeansException;
	
	protected abstract void destroyBean(String beanName, Object bean);
	
}
