package org.springframework.beans.factory.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;

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
			// 객체 생성
			Object bean = BeanUtils.instantiateClass(beanClass);
			populateBean(bean.getClass().getName(), bd, new BeanWrapperImpl(bean));
			return bean;
		}
	}

	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException {
		// 이름, 타입이 오토와이어가 아니면
		if (autowireMode != AUTOWIRE_BY_NAME && autowireMode != AUTOWIRE_BY_TYPE) {
			throw new IllegalArgumentException("Just constants AUTOWIRE_BY_NAME and AUTOWIRE_BY_TYPE allowed");
		}

		RootBeanDefinition bd = new RootBeanDefinition(existingBean.getClass(), autowireMode, dependencyCheck);
		populateBean(existingBean.getClass().getName(), bd, new BeanWrapperImpl(existingBean));
	}

	// 빈 실행전
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

	// 빈 실행후
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
			logger.debug("Creating instance of bean '" + beanName + "' with merged definition [" + mergedBeanDefinition
					+ "]");
		}

		if (mergedBeanDefinition.getDependsOn() != null) {
			for (int i = 0; i < mergedBeanDefinition.getDependsOn().length; i++) {
				// 빈객체 생성??
				getBean(mergedBeanDefinition.getDependsOn()[i]);
			}
		}

		BeanWrapper instanceWrapper = null;

		if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR
				|| mergedBeanDefinition.hasConstructorArgumentValues()) {
			instanceWrapper = autowireConstructor(beanName, mergedBeanDefinition);
		} else {
			instanceWrapper = new BeanWrapperImpl(mergedBeanDefinition.getBeanClass());
			initBeanWrapper(instanceWrapper);
		}
		Object bean = instanceWrapper.getWrappedInstance();

		if (mergedBeanDefinition.isSingleton()) {
			addSingleton(beanName, bean);
		}

		populateBean(beanName, mergedBeanDefinition, instanceWrapper);

		try {
			// bean == mergedBeanDefinition 에 있는 bean
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
		} catch (InvocationTargetException ex) {
			throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
					"Initialization of bean failed", ex.getTargetException());
		} catch (Exception ex) {
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
					throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
							"Invalid constructor argument index: " + index);
				}
				if (index > minNrOfArgs) {
					minNrOfArgs = index + 1;
				}
				String argName = "constructor argument with index " + index;
				ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) entry
						.getValue();

				// cargs -> resolvedValues 복사??
				Object resolvedValue = resolveValueIfNecessary(beanName, mergedBeanDefinition, argName,
						valueHolder.getValue());

				resolvedValues.addIndexedArgumentValue(index, resolvedValue, valueHolder.getType());
			}

			for (Iterator it = cargs.getGenericArgumentValues().iterator(); it.hasNext();) {
				ConstructorArgumentValues.ValueHolder valueHolder = (ConstructorArgumentValues.ValueHolder) it.next();
				String argName = "constructor argument";
				Object resolvedValue = resolveValueIfNecessary(beanName, mergedBeanDefinition, argName,
						valueHolder.getValue());
				resolvedValues.addGenericArgumentValue(resolvedValue, valueHolder.getType());
			}
		}

		Constructor[] constructors = mergedBeanDefinition.getBeanClass().getConstructors();

		// 생성자 파라미터 개수에 맞게 정렬
		Arrays.sort(constructors, new Comparator() {
			public int compare(Object o1, Object o2) {
				int c1pl = ((Constructor) o1).getParameterTypes().length;
				int c2pl = ((Constructor) o2).getParameterTypes().length;
				return (new Integer(c1pl)).compareTo(new Integer(c2pl)) * -1;
			}
		});

		BeanWrapperImpl bw = new BeanWrapperImpl();
		// bw 객체에 beanFactory 객체에 있는 CustomEditor 저장
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
						// customEditor이 있으면
						if (!getCustomEditors().isEmpty()) {
							synchronized (this) {
								args[j] = bw.doTypeConversionIfNecessary(valueHolder.getValue(), argTypes[j]);
							}
						} else {
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

	// 클레스 몇개의 개층 구조로 되어 있는지 검사??
	private int getTypeDifferenceWeight(Class[] argTypes, Object[] args) {
		int result = 0;
		for (int i = 0; i < argTypes.length; i++) {
			// 같은 타입이면
			if (!BeanUtils.isAssignable(argTypes[i], args[i])) {
				return Integer.MAX_VALUE;
			}

			if (args[i] != null) {
				Class superClass = args[i].getClass().getSuperclass();
				while (superClass != null) {
					if (argTypes[i].isAssignableFrom(superClass)) {
						result++;
						superClass = superClass.getSuperclass();
					} else {
						superClass = null;
					}
				}
			}
		}
		return result;
	}

	// BeanWrapper 의 bean 에서 property, values
	protected void populateBean(String beanName, RootBeanDefinition mergedBeanDefinition, BeanWrapper bw) {
		PropertyValues pvs = mergedBeanDefinition.getPropertyValues();

		// 타입, 이름
		if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME
				|| mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
			MutablePropertyValues mpvs = new MutablePropertyValues(pvs);

			// 이름
			if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mergedBeanDefinition, bw, mpvs);
			}

			// 타입
			if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mergedBeanDefinition, bw, mpvs);
			}

			pvs = mpvs;
		}

		dependencyCheck(beanName, mergedBeanDefinition, bw, pvs);
		applyPropertyValues(beanName, mergedBeanDefinition, bw, pvs);
	}

	protected void autowireByName(String beanName, RootBeanDefinition mergedBeanDefinition, BeanWrapper bw,
			MutablePropertyValues pvs) {
		String[] propertyNames = unsatisfiedObjectProperties(mergedBeanDefinition, bw);
		for (int i = 0; i < propertyNames.length; i++) {
			String propertyName = propertyNames[i];
			// factory 에 속성 이름하고 같은게 있으면
			if (containsBean(propertyName)) {
				Object bean = getBean(propertyName);
				pvs.addPropertyValue(propertyName, bean);
				if (logger.isDebugEnabled()) {
					logger.debug("Added autowiring by name from bean name '" + beanName + "' via property '"
							+ propertyName + "' to bean named '" + propertyName + "'");
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("Not autowiring property '" + propertyName + "' of bean '" + beanName
							+ "' by name: no matching bean found");
				}
			}
		}
	}

	protected void autowireByType(String beanName, RootBeanDefinition mergedBeanDefinition, BeanWrapper bw,
			MutablePropertyValues pvs) {
		String[] propertyNames = unsatisfiedObjectProperties(mergedBeanDefinition, bw);
		for (int i = 0; i < propertyNames.length; i++) {
			String propertyName = propertyNames[i];
			// 클레스 타입 가져오기
			Class requiredType = bw.getPropertyDescriptor(propertyName).getPropertyType();
			Map matchingBeans = findMatchingBeans(requiredType);

			// 같은게 1개라면
			if (matchingBeans != null && matchingBeans.size() == 1) {
				pvs.addPropertyValue(propertyName, matchingBeans.values().iterator().next());
				if (logger.isDebugEnabled()) {
					logger.debug("Autowiring by type from bean name '" + beanName + "' via property '" + propertyName
							+ "' to bean named '" + matchingBeans.keySet().iterator().next() + "'");
				}
			} else if (matchingBeans != null && matchingBeans.size() > 1) {
				throw new UnsatisfiedDependencyException(beanName, propertyName, "There are " + matchingBeans.size()
						+ " beans of type [" + requiredType + "] for autowire by type. "
						+ "There should have been 1 to be able to autowire property '" + propertyName + "' of bean '"
						+ beanName + "'.");
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("Not autowiring property '" + propertyName + "' of bean '" + beanName
							+ "' by type: no matching bean found");
				}
			}
		}
	}

	// 의존주입 해야 하는거 검사하기
	protected void dependencyCheck(String beanName, RootBeanDefinition mergedBeanDefinition, BeanWrapper bw,
			PropertyValues pvs) throws UnsatisfiedDependencyException {
		int dependencyCheck = mergedBeanDefinition.getDependencyCheck();
		if (dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_NONE)
			return;

		Set ignoreTypes = getIgnoredDependencyTypes();
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		for (int i = 0; i < pds.length; i++) {

			// ignored 에 있는거 제외 하고
			if (pds[i].getWriteMethod() != null && !ignoreTypes.contains(pds[i].getPropertyType())
					&& pvs.getPropertyValue(pds[i].getName()) == null) {
				boolean isSimple = BeanUtils.isSimpleProperty(pds[i].getPropertyType());
				boolean unsatisfied = (dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_ALL)
						|| (isSimple && dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_SIMPLE)
						|| (!isSimple && dependencyCheck == RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
				if (unsatisfied) {
					throw new UnsatisfiedDependencyException(beanName, pds[i].getName(), null);
				}
			}
		}
	}

	protected String[] unsatisfiedObjectProperties(RootBeanDefinition mergedBeanDefinition, BeanWrapper bw) {
		Set result = new TreeSet();
		Set ignoreTypes = getIgnoredDependencyTypes();
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		for (int i = 0; i < pds.length; i++) {
			String name = pds[i].getName();
			if (pds[i].getWriteMethod() != null && !BeanUtils.isSimpleProperty(pds[i].getPropertyType())
					&& !ignoreTypes.contains(pds[i].getPropertyType())
					&& mergedBeanDefinition.getPropertyValues().getPropertyValue(name) == null) {
				result.add(name);
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	protected void applyPropertyValues(String beanName, RootBeanDefinition mergedBeanDefinition, BeanWrapper bw,
			PropertyValues pvs) throws BeansException {
		if (pvs == null) {
			return;
		}
		MutablePropertyValues deepCopy = new MutablePropertyValues(pvs);
		PropertyValue[] pvals = deepCopy.getPropertyValues();
		for (int i = 0; i < pvals.length; i++) {
			Object value = resolveValueIfNecessary(beanName, mergedBeanDefinition, pvals[i].getName(),
					pvals[i].getValue());
			PropertyValue pv = new PropertyValue(pvals[i].getName(), value);
			deepCopy.setPropertyValueAt(pv, i);
		}
		try {
			// 있으면 동기?
			if (!getCustomEditors().isEmpty()) {
				synchronized (this) {
					bw.setPropertyValues(deepCopy);
				}
			} else {
				bw.setPropertyValues(deepCopy);
			}
		} catch (BeansException ex) {
			throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
					"Error setting property values", ex);
		}
	}

	protected Object resolveValueIfNecessary(String beanName, RootBeanDefinition mergedBeanDefinition, String argName,
			Object value) throws BeansException {
		if (value instanceof AbstractBeanDefinition) {
			BeanDefinition bd = (BeanDefinition) value;
			if (bd instanceof AbstractBeanDefinition) {
				((AbstractBeanDefinition) bd).setSingleton(false);
			}
			String innerBeanName = "(inner bean for property '" + beanName + "." + argName + "')";
			// 빈 생성
			// getMergedBeanDefinition 에서 2번재 인자가 RootBeanDefinition 바로 리턴 왜사용하는
			// 모르겠음 바로 mergedBeanDefinition 넣어도 될거 같은데
			Object bean = createBean(innerBeanName, getMergedBeanDefinition(innerBeanName, bd));
			if (bean instanceof DisposableBean) {
				this.disposableInnerBeans.add(bean);
			}
			// 빈에 있는 실객체 가져오기
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

	// 빈 가져오기
	protected Object resolveReference(RootBeanDefinition mergedBeanDefinition, String beanName, String argName,
			RuntimeBeanReference ref) throws BeansException {
		try {
			logger.debug("Resolving reference from property '" + argName + "' in bean '" + beanName + "' to bean '"
					+ ref.getBeanName() + "'");
			return getBean(ref.getBeanName());
		} catch (BeansException ex) {
			throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
					"Can't resolve reference to bean '" + ref.getBeanName() + "' while setting property '" + argName
							+ "'", ex);
		}
	}

	protected List resolveManagedList(String beanName, RootBeanDefinition mergedBeanDefinition, String argName,
			ManagedList ml) throws BeansException {
		List resolved = new ArrayList();
		for (int i = 0; i < ml.size(); i++) {
			resolved.add(resolveValueIfNecessary(beanName, mergedBeanDefinition, argName + "[" + i + "]", ml.get(i)));
		}
		return resolved;
	}

	protected Set resolveManagedSet(String beanName, RootBeanDefinition mergedBeanDefinition, String argName,
			ManagedSet ms) throws BeansException {
		Set resolved = new HashSet();
		for (Iterator it = ms.iterator(); it.hasNext();) {
			resolved.add(resolveValueIfNecessary(beanName, mergedBeanDefinition, argName + "[(set-element)]", it.next()));
		}
		return resolved;
	}

	protected Map resolveManagedMap(String beanName, RootBeanDefinition mergedBeanDefinition, String argName,
			ManagedMap mm) throws BeansException {
		Map resolved = new HashMap();
		Iterator keys = mm.keySet().iterator();
		while (keys.hasNext()) {
			Object key = keys.next();
			resolved.put(key,
					resolveValueIfNecessary(beanName, mergedBeanDefinition, argName + "[" + key + "]", mm.get(key)));
		}
		return resolved;
	}

	protected void invokeInitMethods(Object bean, String beanName, RootBeanDefinition mergedBeanDefinition)
			throws Exception {
		if (bean instanceof InitializingBean) {
			logger.debug("Calling afterPropertiesSet() on bean with beanName '" + beanName + "'");
			((InitializingBean) bean).afterPropertiesSet();
		}

		if (mergedBeanDefinition.getInitMethodName() != null) {
			logger.debug("Calling custom init method '" + mergedBeanDefinition.getInitMethodName()
					+ "' on bean with beanName '" + beanName + "'");
			// init 메서드 실행
			bean.getClass().getMethod(mergedBeanDefinition.getInitMethodName(), null).invoke(bean, null);
		}
	}

	public void destroySingletons() {
		super.destroySingletons();
		synchronized (this.disposableInnerBeans) {
			// 1회용 beans 제거
			for (Iterator it = this.disposableInnerBeans.iterator(); it.hasNext();) {
				Object bean = it.next();
				it.remove();
				destroyBean("(inner bean of type " + bean.getClass().getName() + ")", bean);
			}
		}
	}

	protected void destroyBean(String beanName, Object bean) {
		logger.debug("Retrieving depending beans for bean '" + beanName + "'");
		String[] dependingBeans = getDependingBeanNames(beanName);
		if (dependingBeans != null) {
			for (int i = 0; i < dependingBeans.length; i++) {
				destroySingleton(dependingBeans[i]);
			}
		}

		if (bean instanceof DisposableBean) {
			logger.debug("Calling destroy() on bean with name '" + beanName + "'");
			try {
				((DisposableBean) bean).destroy();
			} catch (Exception ex) {
				logger.error("destroy() on bean with name '" + beanName + "' threw an exception", ex);
			}
		}

		try {
			RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
			if (bd.getDestroyMethodName() != null) {
				logger.debug("Calling custom destroy method '" + bd.getDestroyMethodName() + "' on bean with name '"
						+ beanName + "'");
				invokeCustomDestroyMethod(beanName, bean, bd.getDestroyMethodName());
			}
		} catch (NoSuchBeanDefinitionException ex) {
		}
	}

	protected void invokeCustomDestroyMethod(String beanName, Object bean, String destroyMethodName) {
		Method[] methods = bean.getClass().getMethods();
		Method targetMethod = null;
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(destroyMethodName)) {
				// 처음에는 null 이니깐 무건 실행후
				// 파라미터 타입 개수 작은거 (기본 생성자 찾기?)
				if (targetMethod == null
						|| methods[i].getParameterTypes().length < targetMethod.getParameterTypes().length) {
					targetMethod = methods[i];
				}
			}
		}
		if (targetMethod == null) {
			logger.error("Couldn't find a method named '" + destroyMethodName + "' on bean with name '" + beanName
					+ "'");
		} else {
			Class[] paramTypes = targetMethod.getParameterTypes();
			if (paramTypes.length > 1) {
				logger.error("Method '" + destroyMethodName + "' of bean '" + beanName
						+ "' has more than one parameter - not supported as destroy method");
			} else if (paramTypes.length == 1 && !paramTypes[0].equals(boolean.class)) {
				logger.error("Method '" + destroyMethodName + "' of bean '" + beanName
						+ "' has a non-boolean parameter - not supported as destroy method");
			} else {
				Object[] args = new Object[paramTypes.length];
				if (paramTypes.length == 1) {
					args[0] = Boolean.TRUE;
				}
				try {
					targetMethod.invoke(bean, args);
				} catch (InvocationTargetException ex) {
					logger.error("Couldn't invoke destroy method '" + destroyMethodName + "' of bean with name '"
							+ beanName + "'", ex.getTargetException());
				} catch (Exception ex) {
					logger.error("Couldn't invoke destroy method '" + destroyMethodName + "' of bean with name '"
							+ beanName + "'", ex);
				}
			}
		}
	}

	protected abstract Map findMatchingBeans(Class requiredType) throws BeansException;

	protected abstract String[] getDependingBeanNames(String beanName) throws BeansException;

}
