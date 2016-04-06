package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ApplicationEventMulticasterImpl;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.OrderComparator;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext{

	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	private ApplicationContext parent;
	
	private final List beanFactoryPostProcessors = new ArrayList();
	
	private String displayName = getClass().getName() + ";hashCode=" + hashCode();
	
	private long startupTime;
	
	private MessageSource messageSource;
	
	private final ApplicationEventMulticaster eventMulticaster = new ApplicationEventMulticasterImpl();
	
	public AbstractApplicationContext() {
	}
	
	public AbstractApplicationContext(ApplicationContext parent) {
		this.parent = parent;
	}
	
	public ApplicationContext getParent() {
		return parent;
	}
	
	protected void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public long getStartupDate() {
		return startupTime;
	}
	
	//이벤트 추가 부모 있으면 부모에도 추가
	public void publishEvent(ApplicationEvent event) {
		if (logger.isDebugEnabled()) {
			logger.debug("Publishing event in context [" + getDisplayName() + "]: " + event.toString());
		}
		this.eventMulticaster.onApplicationEvent(event);
		if (this.parent != null) {
			parent.publishEvent(event);
		}
	}
	

	public void setParent(ApplicationContext parent) {
		this.parent = parent;
	}

	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor beanFactoryPostProcessor) {
		this.beanFactoryPostProcessors.add(beanFactoryPostProcessor);
	}
	
	public List getBeanFactoryPostProcessors() {
		return beanFactoryPostProcessors;
	}
	
	public void refresh() throws BeansException {
		this.startupTime = System.currentTimeMillis();

		// tell subclass to refresh the internal bean factory
		refreshBeanFactory();
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();

		// configure the bean factory with context semantics
		beanFactory.registerCustomEditor(Resource.class, new ContextResourceEditor(this));
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		beanFactory.ignoreDependencyType(ResourceLoader.class);
		beanFactory.ignoreDependencyType(ApplicationContext.class);
		postProcessBeanFactory(beanFactory);

		// invoke factory processors registered with the context instance
		for (Iterator it = getBeanFactoryPostProcessors().iterator(); it.hasNext();) {
			BeanFactoryPostProcessor factoryProcessor = (BeanFactoryPostProcessor) it.next();
			factoryProcessor.postProcessBeanFactory(beanFactory);
		}

		if (getBeanDefinitionCount() == 0) {
			logger.warn("No beans defined in ApplicationContext [" + getDisplayName() + "]");
		}
		else {
			logger.info(getBeanDefinitionCount() + " beans defined in ApplicationContext [" + getDisplayName() + "]");
		}

		// invoke factory processors registered as beans in the context
		invokeBeanFactoryPostProcessors();

		// register bean processor that intercept bean creation
		registerBeanPostProcessors();

		// initialize message source for this context
		initMessageSource();

		// initialize other special beans in specific context subclasses
		onRefresh();

		// check for listener beans and register them
		refreshListeners();

		// instantiate singletons this late to allow them to access the message source
		beanFactory.preInstantiateSingletons();

		// last step: publish respective event
		publishEvent(new ContextRefreshedEvent(this));
	}
	
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}
	
	private void invokeBeanFactoryPostProcessors() throws BeansException {
		//BeanFactoryPostProcessor 티입의 객체 찾기
		String[] beanNames = getBeanDefinitionNames(BeanFactoryPostProcessor.class);
		BeanFactoryPostProcessor[] factoryProcessors = new BeanFactoryPostProcessor[beanNames.length];
		
		for (int i = 0; i < beanNames.length; i++) {
			factoryProcessors[i] = (BeanFactoryPostProcessor) getBean(beanNames[i]);
		}
		Arrays.sort(factoryProcessors, new OrderComparator());
		for (int i = 0; i < factoryProcessors.length; i++) {
			BeanFactoryPostProcessor factoryProcessor = factoryProcessors[i];
			factoryProcessor.postProcessBeanFactory(getBeanFactory());
		}
	}
	
	private void registerBeanPostProcessors() throws BeansException {
		//BeanPostProcessor 객체 찾아와서
		String[] beanNames = getBeanDefinitionNames(BeanPostProcessor.class);
		if (beanNames.length > 0) {
			List beanProcessors = new ArrayList();
			//찾아온 이름으로 리스트에 빈 등로하고
			for (int i = 0; i < beanNames.length; i++) {
				beanProcessors.add(getBean(beanNames[i]));
			}
			//순서대로 정렬하고
			Collections.sort(beanProcessors, new OrderComparator());
			for (Iterator it = beanProcessors.iterator(); it.hasNext();) {
				//팩토리에 추가
				getBeanFactory().addBeanPostProcessor((BeanPostProcessor) it.next());
			}
		}
	}
	
	private void initMessageSource() throws BeansException {
		try {
			this.messageSource = (MessageSource) getBean(MESSAGE_SOURCE_BEAN_NAME);
			if (this.parent != null && (this.messageSource instanceof HierarchicalMessageSource) && Arrays.asList(getBeanDefinitionNames()).contains(MESSAGE_SOURCE_BEAN_NAME)) {
				//messageSource 에서도 부모 messageSourceㅍ등록
				((HierarchicalMessageSource) this.messageSource).setParentMessageSource(this.parent);
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			logger.info("No MessageSource found for [" + getDisplayName() + "]: using empty StaticMessageSource");
			this.messageSource = new StaticMessageSource();
		}
	}
	
	protected void onRefresh() throws BeansException {
		// for subclasses: do nothing by default
	}
	
	private void refreshListeners() throws BeansException {
		logger.info("Refreshing listeners");
		//FactoryBeans 아닌거, prototype(1회성???) 객체라도
		Collection listeners = getBeansOfType(ApplicationListener.class, true, false).values();
		logger.debug("Found " + listeners.size() + " listeners in bean factory");
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			ApplicationListener listener = (ApplicationListener) it.next();
			//ApplicationListener(이벤트) 등록 set 이기때문 중복 안된다.
			addListener(listener);
			logger.info("Application listener [" + listener + "] added");
		}
	}
	
	protected void addListener(ApplicationListener listener) {
		this.eventMulticaster.addApplicationListener(listener);
	}
	
	public void close() {
		logger.info("Closing application context [" + getDisplayName() + "]");

		// destroy all cached singletons in this context,
		// invoking DisposableBean.destroy and/or "destroy-method"
		getBeanFactory().destroySingletons();

		// publish respective event
		publishEvent(new ContextClosedEvent(this));
	}


}
