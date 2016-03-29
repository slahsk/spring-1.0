package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
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
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
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
			//객체 생성
			Object bean = BeanUtils.instantiateClass(beanClass);
			populateBean(bean.getClass().getName(), bd, new BeanWrapperImpl(bean));
			return bean;
		}
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
		Arrays.sort(constructors, new Comparator() {
			public int compare(Object o1, Object o2) {
				int c1pl = ((Constructor) o1).getParameterTypes().length;
				int c2pl = ((Constructor) o2).getParameterTypes().length;
				return (new Integer(c1pl)).compareTo(new Integer(c2pl)) * -1;
			}
		});

		BeanWrapperImpl bw = new BeanWrapperImpl();
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
						// synchronize if custom editors are registered
						// necessary because PropertyEditors are not thread-safe
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

	protected Object resolveValueIfNecessary(String beanName, RootBeanDefinition mergedBeanDefinition, String argName,Object value) throws BeansException {
		if (value instanceof AbstractBeanDefinition) {
			BeanDefinition bd = (BeanDefinition) value;
			if (bd instanceof AbstractBeanDefinition) {
				((AbstractBeanDefinition) bd).setSingleton(false);
			}
			String innerBeanName = "(inner bean for property '" + beanName + "." + argName + "')";
			Object bean = createBean(innerBeanName, getMergedBeanDefinition(innerBeanName, bd));
			if (bean instanceof DisposableBean) {
				this.disposableInnerBeans.add(bean);
			}
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
