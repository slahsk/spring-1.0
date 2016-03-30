package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements
		AutowireCapableBeanFactory {

	static {
		DisposableBean.class.getName();
	}

	private final Set disposableInnerBeans = Collections.synchronizedSet(new HashSet());

	public AbstractAutowireCapableBeanFactory() {
	}

	public AbstractAutowireCapableBeanFactory(BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}

	public Object autowire(Class beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
		RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
		if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
			return autowireConstructor(beanClass.getName(), bd).getWrappedInstance();
		} else {
			//객체 생성
			Object bean = BeanUtils.instantiateClass(beanClass);
			populateBean(bean.getClass().getName(), bd, new BeanWrapperImpl(bean));
			return bean;
		}
	}
	
	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException {
		//이름, 타입이 오토와이어가 아니면
		if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
			throw new IllegalArgumentException("Just constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE allowed");
		}
		
		RootBeanDefinition bd = new RootBeanDefinition(existingBean.getClass(), autowireMode, dependencyCheck);
		populateBean(existingBean.getClass().getName(), bd, new BeanWrapperImpl(existingBean));
	}
	
	//빈 실행전
	public Object applyBeanPostProcessorsBeforeInitialization(Object bean, String name) throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking BeanPostProcessors before initialization of bean '" + name + "'");
		}
		Object result = bean;
		for (Iterator it = getBeanPostProcessors().iterator(); it.hasNext();) {
			BeanPostProcessor beanProcessor = (BeanPostProcessor) it.next();
			result = beanProcessor.postProcessBeforeInitialization(result, name);
		}
		return result;
	}
	
	//빈 실행후
	public Object applyBeanPostProcessorsAfterInitialization(Object bean, String name) throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking BeanPostProcessors after initialization of bean '" + name + "'");
		}
		Object result = bean;
		for (Iterator it = getBeanPostProcessors().iterator(); it.hasNext();) {
			BeanPostProcessor beanProcessor = (BeanPostProcessor) it.next();
			result = beanProcessor.postProcessAfterInitialization(result, name);
		}
		return result;
	}
	
	protected Object createBean(String beanName, RootBeanDefinition mergedBeanDefinition) throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating instance of bean '" + beanName + "' with merged definition [" + mergedBeanDefinition + "]");
		}

		if (mergedBeanDefinition.getDependsOn() != null) {
			for (int i = 0; i < mergedBeanDefinition.getDependsOn().length; i++) {
				//빈객체 생성??
				getBean(mergedBeanDefinition.getDependsOn()[i]);
			}
		}

		BeanWrapper instanceWrapper = null;
		
		if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR ||
				mergedBeanDefinition.hasConstructorArgumentValues()) {
			instanceWrapper = autowireConstructor(beanName, mergedBeanDefinition);
		}
		else {
			instanceWrapper = new BeanWrapperImpl(mergedBeanDefinition.getBeanClass());
			initBeanWrapper(instanceWrapper);
		}
		Object bean = instanceWrapper.getWrappedInstance();

		if (mergedBeanDefinition.isSingleton()) {
			addSingleton(beanName, bean);
		}

		populateBean(beanName, mergedBeanDefinition, instanceWrapper);

		try {
			//bean == mergedBeanDefinition 에 있는 bean
			if (bean instanceof BeanNameAware) {
				if (logger.isDebugEnabled()) {
					logger.debug("Invoking setBeanName() on BeanNameAware bean '" + beanName + "'");
				}
				((BeanNameAware) bean).setBeanName(beanName);
			}

			if (bean instanceof BeanFactoryAware) {
				if (logger.isDebugEnabled()) {
					logger.debug("Invoking setBeanFactory() on BeanFactoryAware bean '" + beanName + "'");
				}
				((BeanFactoryAware) bean).setBeanFactory(this);
			}


			bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
			invokeInitMethods(bean, beanName, mergedBeanDefinition);
			bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
		}
		catch (InvocationTargetException ex) {
			throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
																			"Initialization of bean failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
																			"Initialization of bean failed", ex);
		}
		return bean;
	}
	
	protected BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mergedBeanDefinition)
			throws BeansException {

		ConstructorArgumentValues cargs = mergedBeanDefinition.getConstructorArgumentValues();
		ConstructorArgumentValues resolvedValues = new ConstructorArgumentValues();

		int minNrOfArgs = 0;
		if (cargs != null) {
			minNrOfArgs = cargs.getNrOfArguments();
			for (Iterator it = cargs.getIndexedArgumentValues().entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				int index = ((Integer) entry.getKey()).intValue();
				if (index < 0) {
					throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,"Invalid constructor argument index: " + index);
				}
				if (index > minNrOfArgs) {
					minNrOfArgs = index + 1;
				}
				String argName = "constructor argument with index " + index;
				ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry.getValue();
				
				// cargs -> resolvedValues 복사??
				Object resolvedValue = resolveValueIfNecessary(beanName, mergedBeanDefinition, argName,valueHolder.getValue());
				
				resolvedValues.addIndexedArgumentValue(index, resolvedValue, valueHolder.getType());
			}
			
			for (Iterator it = cargs.getGenericArgumentValues().iterator(); it.hasNext();) {
				ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) it.next();
				String argName = "constructor argument";
				Object resolvedValue = resolveValueIfNecessary(beanName, mergedBeanDefinition, argName,valueHolder.getValue());
				resolvedValues.addGenericArgumentValue(resolvedValue, valueHolder.getType());
			}
		}

		Constructor[] constructors = mergedBeanDefinition.getBeanClass().getConstructors();
		
		//생성자 파라미터 개수에 맞게 정렬
		Arrays.sort(constructors, new Comparator() {
			public int compare(Object o1, Object o2) {
				int c1pl = ((Constructor) o1).getParameterTypes().length;
				int c2pl = ((Constructor) o2).getParameterTypes().length;
				return (new Integer(c1pl)).compareTo(new Integer(c2pl)) * -1;
			}
		});

		BeanWrapperImpl bw = new BeanWrapperImpl();
		//bw 객체에 beanFactory 객체에 있는 CustomEditor 저장
		initBeanWrapper(bw);
		
		Constructor constructorToUse = null;
		Object[] argsToUse = null;
		int minTypeDiffWeight = Integer.MAX_VALUE;
		for (int i = 0; i < constructors.length; i++) {
			try {
				Constructor constructor = constructors[i];
				if (constructor.getParameterTypes().length < minNrOfArgs) {
					throw new BeanCreationException(
							mergedBeanDefinition.getResourceDescription(),
							beanName,
							minNrOfArgs
									+ " constructor arguments specified but no matching constructor found in bean '"
									+ beanName
									+ "' (hint: specify index arguments for simple parameters to avoid type ambiguities)");
				}
				Class[] argTypes = constructor.getParameterTypes();
				Object[] args = new Object[argTypes.length];
				for (int j = 0; j < argTypes.length; j++) {
					ConstructorArgumentValues.ValueHolder valueHolder = resolvedValues.getArgumentValue(j, argTypes[j]);
					if (valueHolder != null) {
						//customEditor이 있으면
						if(!getCustomEditors().isEmpty()) {
							synchronized (this) {
								args[j] = bw.doTypeConversionIfNecessary(valueHolder.getValue(), argTypes[j]);
							}
						}else {
							args[j] = bw.doTypeConversionIfNecessary(valueHolder.getValue(), argTypes[j]);
						}
					} else {
						if (mergedBeanDefinition.getResolvedAutowireMode() != RootBeanDefinition.AUTOWIRE_CONSTRUCTOR) {
							throw new UnsatisfiedDependencyException(beanName, j, argTypes[j],
									"Did you specify the correct bean references as generic constructor arguments?");
						}
						Map matchingBeans = findMatchingBeans(argTypes[j]);
						if (matchingBeans == null || matchingBeans.size() != 1) {
							throw new UnsatisfiedDependencyException(beanName, j, argTypes[j], "There are "
									+ matchingBeans.size() + " beans of type [" + argTypes[j]
									+ "] for autowiring constructor. "
									+ "There should have been 1 to be able to autowire constructor of bean '"
									+ beanName + "'.");
						}
						args[j] = matchingBeans.values().iterator().next();
						logger.info("Autowiring by type from bean name '" + beanName
								+ "' via constructor to bean named '" + matchingBeans.keySet().iterator().next() + "'");
					}
				}
				int typeDiffWeight = getTypeDifferenceWeight(argTypes, args);
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = constructor;
					argsToUse = args;
					minTypeDiffWeight = typeDiffWeight;
				}
			} catch (BeansException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring constructor [" + constructors[i] + "] of bean '" + beanName
							+ "': could not satisfy dependencies. Detail: " + ex.getMessage());
				}
				if (i == constructors.length - 1 && constructorToUse == null) {
					// all constructors tried
					throw ex;
				} else {
					// swallow and try next constructor
				}
			}
		}

		if (constructorToUse == null) {
			throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
					"Could not resolve matching constructor");
		}
		bw.setWrappedInstance(BeanUtils.instantiateClass(constructorToUse, argsToUse));
		logger.info("Bean '" + beanName + "' instantiated via constructor [" + constructorToUse + "]");
		return bw;
	}
	
	//클레스 몇개의 개층 구조로 되어 있는지 검사??
	private int getTypeDifferenceWeight(Class[] argTypes, Object[] args) {
		int result = 0;
		for (int i = 0; i < argTypes.length; i++) {
			//같은 타입이면
			if (!BeanUtils.isAssignable(argTypes[i], args[i])) {
				return Integer.MAX_VALUE;
			}
			
			if (args[i] != null) {
				Class superClass = args[i].getClass().getSuperclass();
				while (superClass != null) {
					if (argTypes[i].isAssignableFrom(superClass)) {
						result++;
						superClass = superClass.getSuperclass();
					}
					else {
						superClass = null;
					}
				}
			}
		}
		return result;
	}
	
	protected void populateBean(String beanName, RootBeanDefinition mergedBeanDefinition, BeanWrapper bw) {
		PropertyValues pvs = mergedBeanDefinition.getPropertyValues();
		
		//타입, 이름
		if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME || mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
			MutablePropertyValues mpvs = new MutablePropertyValues(pvs);
			
			//이름
			if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mergedBeanDefinition, bw, mpvs);
			}
			
			//타입
			if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mergedBeanDefinition, bw, mpvs);
			}

			pvs = mpvs;
		}

		dependencyCheck(beanName, mergedBeanDefinition, bw, pvs);
		applyPropertyValues(beanName, mergedBeanDefinition, bw, pvs);
	}
	
	protected Object resolveValueIfNecessary(String beanName, RootBeanDefinition mergedBeanDefinition, String argName,Object value) throws BeansException {
		if (value instanceof AbstractBeanDefinition) {
			BeanDefinition bd = (BeanDefinition) value;
			if (bd instanceof AbstractBeanDefinition) {
				((AbstractBeanDefinition) bd).setSingleton(false);
			}
			String innerBeanName = "(inner bean for property '" + beanName + "." + argName + "')";
			//빈 생성   
			//getMergedBeanDefinition 에서 2번재 인자가 RootBeanDefinition 바로 리턴 왜사용하는 모르겠음 바로 mergedBeanDefinition 넣어도 될거 같은데
			Object bean = createBean(innerBeanName, getMergedBeanDefinition(innerBeanName, bd));
			if (bean instanceof DisposableBean) {
				this.disposableInnerBeans.add(bean);
			}
			//빈에 있는 실객체 가져오기
			return getObjectForSharedInstance(innerBeanName, bean);
		} else if (value instanceof RuntimeBeanReference) {
			RuntimeBeanReference ref = (RuntimeBeanReference) value;
			return resolveReference(mergedBeanDefinition, beanName, argName, ref);
		} else if (value instanceof ManagedList) {
			return resolveManagedList(beanName, mergedBeanDefinition, argName, (ManagedList) value);
		} else if (value instanceof ManagedSet) {
			return resolveManagedSet(beanName, mergedBeanDefinition, argName, (ManagedSet) value);
		} else if (value instanceof ManagedMap) {
			ManagedMap mm = (ManagedMap) value;
			return resolveManagedMap(beanName, mergedBeanDefinition, argName, mm);
		} else {
			return value;
		}
	}

}
