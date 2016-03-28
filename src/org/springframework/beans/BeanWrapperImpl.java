package org.springframework.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.propertyeditors.ClassEditor;
import org.springframework.beans.propertyeditors.FileEditor;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.beans.propertyeditors.URLEditor;
import org.springframework.util.StringUtils;

public class BeanWrapperImpl implements BeanWrapper {
	private static final Log logger = LogFactory.getLog(BeanWrapperImpl.class);
	private static final Map defaultEditors = new HashMap();

	static {
		defaultEditors.put(Class.class, ClassEditor.class);
		defaultEditors.put(File.class, FileEditor.class);
		defaultEditors.put(Locale.class, LocaleEditor.class);
		defaultEditors.put(Properties.class, PropertiesEditor.class);
		defaultEditors.put(String[].class, StringArrayPropertyEditor.class);
		defaultEditors.put(URL.class, URLEditor.class);
	}

	private Object object;
	private String nestedPath = "";
	private Map nestedBeanWrappers;
	private Map customEditors;

	private CachedIntrospectionResults cachedIntrospectionResults;

	// 생성자
	public BeanWrapperImpl() {
	}

	public BeanWrapperImpl(Object object) throws BeansException {
		setWrappedInstance(object);
	}

	public BeanWrapperImpl(Object object, String nestedPath) throws BeansException {
		setWrappedInstance(object);
		this.nestedPath = nestedPath;
	}

	public BeanWrapperImpl(Class clazz) throws BeansException {
		// class 이면 객체 생성해서 주입
		setWrappedInstance(BeanUtils.instantiateClass(clazz));
	}

	// 인터페이스 구현
	public void setWrappedInstance(Object object) throws BeansException {
		if (object == null) {
			throw new FatalBeanException("Cannot set BeanWrapperImpl target to a null object");
		}
		this.object = object;
		this.nestedBeanWrappers = null;
		if (this.cachedIntrospectionResults == null
				|| !this.cachedIntrospectionResults.getBeanClass().equals(object.getClass())) {
			// cache 없으면 객체 생성해서 관리
			this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(object.getClass());
		}
	}

	public Class getWrappedClass() {
		return object.getClass();
	}

	public Object getWrappedInstance() {
		return object;
	}

	public void registerCustomEditor(Class requiredType, PropertyEditor propertyEditor) {
		registerCustomEditor(requiredType, null, propertyEditor);
	}

	public void registerCustomEditor(Class requiredType, String propertyPath, PropertyEditor propertyEditor) {
		if (propertyPath != null) {
			List bws = getBeanWrappersForPropertyPath(propertyPath);
			for (Iterator it = bws.iterator(); it.hasNext();) {
				BeanWrapperImpl bw = (BeanWrapperImpl) it.next();
				bw.doRegisterCustomEditor(requiredType, getFinalPath(propertyPath), propertyEditor);
			}
		} else {
			doRegisterCustomEditor(requiredType, propertyPath, propertyEditor);
		}
	}

	private void doRegisterCustomEditor(Class requiredType, String propertyName, PropertyEditor propertyEditor) {
		if (this.customEditors == null) {
			this.customEditors = new HashMap();
		}
		if (propertyName != null) {
			// consistency check
			PropertyDescriptor descriptor = getPropertyDescriptor(propertyName);
			if (requiredType != null && !descriptor.getPropertyType().isAssignableFrom(requiredType)) {
				throw new IllegalArgumentException("Types do not match: required [" + requiredType.getName()
						+ "], found [" + descriptor.getPropertyType().getName() + "]");
			}
			// 커스텀 한 에디터 따로 관리
			this.customEditors.put(propertyName, propertyEditor);
		} else {
			if (requiredType == null) {
				throw new IllegalArgumentException("No propertyName and no requiredType specified");
			}
			this.customEditors.put(requiredType, propertyEditor);
		}
	}

	public PropertyEditor findCustomEditor(Class requiredType, String propertyPath) {
		if (propertyPath != null) {
			BeanWrapperImpl bw = getBeanWrapperForPropertyPath(propertyPath);
			return bw.doFindCustomEditor(requiredType, getFinalPath(propertyPath));
		} else {
			return doFindCustomEditor(requiredType, propertyPath);
		}
	}

	private PropertyEditor doFindCustomEditor(Class requiredType, String propertyName) {
		if (this.customEditors == null) {
			return null;
		}
		if (propertyName != null) {
			PropertyDescriptor descriptor = null;
			try {
				descriptor = getPropertyDescriptor(propertyName);
				PropertyEditor editor = (PropertyEditor) this.customEditors.get(propertyName);
				if (editor != null) {
					if (requiredType != null) {
						if (!descriptor.getPropertyType().isAssignableFrom(requiredType)) {
							throw new IllegalArgumentException("Types do not match: required=" + requiredType.getName()
									+ ", found=" + descriptor.getPropertyType());
						}
					}
					return editor;
				} else {
					if (requiredType == null) {
						requiredType = descriptor.getPropertyType();
					}
				}
			} catch (BeansException ex) {
				requiredType = getPropertyValue(propertyName).getClass();
			}
		}
		return (PropertyEditor) this.customEditors.get(requiredType);
	}

	private boolean isNestedProperty(String path) {
		return path.indexOf(NESTED_PROPERTY_SEPARATOR) != -1;
	}

	// a.b.c 이면 마지막꺼 가져오기
	private String getFinalPath(String nestedPath) {
		String finalPath = nestedPath.substring(nestedPath.lastIndexOf(NESTED_PROPERTY_SEPARATOR) + 1);
		if (logger.isDebugEnabled() && !nestedPath.equals(finalPath)) {
			logger.debug("Final path in nested property value '" + nestedPath + "' is '" + finalPath + "'");
		}
		return finalPath;
	}

	private BeanWrapperImpl getBeanWrapperForPropertyPath(String propertyPath) {
		int pos = propertyPath.indexOf(NESTED_PROPERTY_SEPARATOR);
		if (pos > -1) {
			String nestedProperty = propertyPath.substring(0, pos);
			String nestedPath = propertyPath.substring(pos + 1);
			logger.debug("Navigating to nested property '" + nestedProperty + "' of property path '" + propertyPath
					+ "'");
			BeanWrapperImpl nestedBw = getNestedBeanWrapper(nestedProperty);
			return nestedBw.getBeanWrapperForPropertyPath(nestedPath);
		} else {
			return this;
		}
	}

	private List getBeanWrappersForPropertyPath(String propertyPath) {
		List beanWrappers = new ArrayList();
		int pos = propertyPath.indexOf(NESTED_PROPERTY_SEPARATOR);

		// a.b.c 이러한 . 이 들어 있는 경우
		if (pos > -1) {
			String nestedProperty = propertyPath.substring(0, pos);
			String nestedPath = propertyPath.substring(pos + 1);
			if (nestedProperty.indexOf('[') == -1) {
				Class propertyType = getPropertyDescriptor(nestedProperty).getPropertyType();
				if (propertyType.isArray()) {
					Object[] array = (Object[]) getPropertyValue(nestedProperty);
					for (int i = 0; i < array.length; i++) {
						beanWrappers.addAll(getBeanWrappersForNestedProperty(propertyPath, nestedProperty + "[" + i
								+ "]", nestedPath));
					}
					return beanWrappers;
				} else if (List.class.isAssignableFrom(propertyType)) {
					List list = (List) getPropertyValue(nestedProperty);
					for (int i = 0; i < list.size(); i++) {
						beanWrappers.addAll(getBeanWrappersForNestedProperty(propertyPath, nestedProperty + "[" + i
								+ "]", nestedPath));
					}
					return beanWrappers;
				} else if (Map.class.isAssignableFrom(propertyType)) {
					Map map = (Map) getPropertyValue(nestedProperty);
					for (Iterator it = map.keySet().iterator(); it.hasNext();) {
						beanWrappers.addAll(getBeanWrappersForNestedProperty(propertyPath,
								nestedProperty + "[" + it.next() + "]", nestedPath));
					}
					return beanWrappers;
				}
			}
			beanWrappers.addAll(getBeanWrappersForNestedProperty(propertyPath, nestedProperty, nestedPath));
			return beanWrappers;
		} else {
			beanWrappers.add(this);
			return beanWrappers;
		}
	}

	private List getBeanWrappersForNestedProperty(String propertyPath, String nestedProperty, String nestedPath) {
		logger.debug("Navigating to nested property '" + nestedProperty + "' of property path '" + propertyPath + "'");
		BeanWrapperImpl nestedBw = getNestedBeanWrapper(nestedProperty);
		return nestedBw.getBeanWrappersForPropertyPath(nestedPath);
	}

	private BeanWrapperImpl getNestedBeanWrapper(String nestedProperty) {
		if (this.nestedBeanWrappers == null) {
			this.nestedBeanWrappers = new HashMap();
		}
		String[] tokens = getPropertyNameTokens(nestedProperty);
		Object propertyValue = getPropertyValue(tokens[0], tokens[1], tokens[2]);
		String canonicalName = tokens[0];
		if (propertyValue == null) {
			throw new NullValueInNestedPathException(getWrappedClass(), canonicalName);
		}

		BeanWrapperImpl nestedBw = (BeanWrapperImpl) this.nestedBeanWrappers.get(canonicalName);
		if (nestedBw == null) {
			logger.debug("Creating new nested BeanWrapper for property '" + canonicalName + "'");
			nestedBw = new BeanWrapperImpl(propertyValue, this.nestedPath + canonicalName + NESTED_PROPERTY_SEPARATOR);
			if (this.customEditors != null) {
				for (Iterator it = this.customEditors.keySet().iterator(); it.hasNext();) {
					Object key = it.next();
					if (key instanceof Class) {
						Class requiredType = (Class) key;
						PropertyEditor propertyEditor = (PropertyEditor) this.customEditors.get(key);
						nestedBw.registerCustomEditor(requiredType, null, propertyEditor);
					}
				}
			}
			this.nestedBeanWrappers.put(canonicalName, nestedBw);
		} else {
			logger.debug("Using cached nested BeanWrapper for property '" + canonicalName + "'");
		}
		return nestedBw;
	}

	private String[] getPropertyNameTokens(String propertyName) {
		String actualName = propertyName;
		String key = null;
		int keyStart = propertyName.indexOf('[');
		if (keyStart != -1 && propertyName.endsWith("]")) {
			actualName = propertyName.substring(0, keyStart);
			key = propertyName.substring(keyStart + 1, propertyName.length() - 1);
			if (key.startsWith("'") && key.endsWith("'")) {
				key = key.substring(1, key.length() - 1);
			} else if (key.startsWith("\"") && key.endsWith("\"")) {
				key = key.substring(1, key.length() - 1);
			}
		}
		String canonicalName = actualName;
		if (key != null) {
			canonicalName += "[" + key + "]";
		}
		return new String[] { canonicalName, actualName, key };
	}

	public Object getPropertyValue(String propertyName) throws BeansException {
		if (isNestedProperty(propertyName)) {
			BeanWrapper nestedBw = getBeanWrapperForPropertyPath(propertyName);
			return nestedBw.getPropertyValue(getFinalPath(propertyName));
		}
		String[] tokens = getPropertyNameTokens(propertyName);
		return getPropertyValue(tokens[0], tokens[1], tokens[2]);
	}

	// propertyName 은 Exception 메세지 작성에서만 사용 됨
	// actualName의 메소드 이름을 실행시켜나온 값에 대해서 key 가지고 찾는다
	private Object getPropertyValue(String propertyName, String actualName, String key) {
		PropertyDescriptor pd = getPropertyDescriptor(actualName);
		Method readMethod = pd.getReadMethod();
		if (readMethod == null) {
			throw new FatalBeanException("Cannot get property '" + actualName + "': not readable", null);
		}
		if (logger.isDebugEnabled())
			logger.debug("About to invoke read method [" + readMethod + "] on object of class ["
					+ this.object.getClass().getName() + "]");
		try {
			Object value = readMethod.invoke(this.object, null);
			if (key != null) {
				if (value == null) {
					throw new FatalBeanException(
							"Cannot access indexed value in property referenced in indexed property path '"
									+ propertyName + "': returned null");
				} else if (value.getClass().isArray()) {
					Object[] array = (Object[]) value;
					return array[Integer.parseInt(key)];
				} else if (value instanceof List) {
					List list = (List) value;
					return list.get(Integer.parseInt(key));
				} else if (value instanceof Set) {
					// apply index to Iterator in case of a Set
					Set set = (Set) value;
					int index = Integer.parseInt(key);
					Iterator it = set.iterator();
					for (int i = 0; it.hasNext(); i++) {
						Object elem = it.next();
						if (i == index) {
							return elem;
						}
					}
					throw new FatalBeanException("Cannot get element with index " + index + " from Set of size "
							+ set.size() + ", accessed using property path '" + propertyName + "'");
				} else if (value instanceof Map) {
					Map map = (Map) value;
					return map.get(key);
				} else {
					throw new FatalBeanException("Property referenced in indexed property path '" + propertyName
							+ "' is neither an array nor a List nor a Map; returned value was [" + value + "]");
				}
			} else {
				return value;
			}
		} catch (InvocationTargetException ex) {
			throw new FatalBeanException("Getter for property '" + actualName + "' threw exception", ex);
		} catch (IllegalAccessException ex) {
			throw new FatalBeanException("Illegal attempt to get property '" + actualName + "' threw exception", ex);
		} catch (IndexOutOfBoundsException ex) {
			throw new FatalBeanException("Index of out of bounds in property path '" + propertyName + "'", ex);
		} catch (NumberFormatException ex) {
			throw new FatalBeanException("Invalid index in property path '" + propertyName + "'");
		}
	}

	public void setPropertyValue(String propertyName, Object value) throws BeansException {
		if (isNestedProperty(propertyName)) {
			try {
				BeanWrapper nestedBw = getBeanWrapperForPropertyPath(propertyName);
				nestedBw.setPropertyValue(new PropertyValue(getFinalPath(propertyName), value));
				return;
			} catch (NullValueInNestedPathException ex) {
				throw ex;
			} catch (FatalBeanException ex) {
				throw new NotWritablePropertyException(propertyName, getWrappedClass(), ex);
			}
		}
		String[] tokens = getPropertyNameTokens(propertyName);
		setPropertyValue(tokens[0], tokens[1], tokens[2], value);
	}

	private void setPropertyValue(String propertyName, String actualName, String key, Object value)
			throws BeansException {
		if (key != null) {
			Object propValue = getPropertyValue(actualName);
			if (propValue == null) {
				throw new FatalBeanException(
						"Cannot access indexed value in property referenced in indexed property path '" + propertyName
								+ "': returned null");
			}
			// 배열
			else if (propValue.getClass().isArray()) {
				Object[] array = (Object[]) propValue;
				array[Integer.parseInt(key)] = value;
			}
			// 리스틑
			else if (propValue instanceof List) {
				List list = (List) propValue;
				int index = Integer.parseInt(key);
				if (index < list.size()) {
					list.set(index, value);
				} else if (index >= list.size()) {
					for (int i = list.size(); i < index; i++) {
						try {
							list.add(null);
						} catch (NullPointerException ex) {
							throw new FatalBeanException("Cannot set element with index " + index + " in List of size "
									+ list.size() + ", accessed using property path '" + propertyName
									+ "': List does not support filling up gaps with null elements");
						}
					}
					list.add(value);
				}
			}
			// 맵
			else if (propValue instanceof Map) {
				Map map = (Map) propValue;
				map.put(key, value);
			} else {
				throw new FatalBeanException("Property referenced in indexed property path '" + propertyName
						+ "' is neither an array nor a List nor a Map; returned value was [" + value + "]");
			}
		} else {// key null
			if (!isWritableProperty(propertyName)) {
				throw new NotWritablePropertyException(propertyName, getWrappedClass());
			}
			PropertyDescriptor pd = getPropertyDescriptor(propertyName);
			Method writeMethod = pd.getWriteMethod();
			Object newValue = null;
			try {
				newValue = doTypeConversionIfNecessary(propertyName, propertyName, null, value, pd.getPropertyType());

				if (pd.getPropertyType().isPrimitive() && (newValue == null || "".equals(newValue))) {
					throw new IllegalArgumentException("Invalid value [" + value + "] for property '" + pd.getName()
							+ "' of primitive type [" + pd.getPropertyType() + "]");
				}

				if (logger.isDebugEnabled()) {
					logger.debug("About to invoke write method [" + writeMethod + "] on object of class ["
							+ object.getClass().getName() + "]");
				}
				writeMethod.invoke(this.object, new Object[] { newValue });
				if (logger.isDebugEnabled()) {
					String msg = "Invoked write method [" + writeMethod + "] with value ";
					if (newValue == null || BeanUtils.isSimpleProperty(pd.getPropertyType())) {
						logger.debug(msg + "[" + newValue + "]");
					} else {
						logger.debug(msg + "of type [" + pd.getPropertyType().getName() + "]");
					}
				}
			} catch (InvocationTargetException ex) {
				PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(this.object, this.nestedPath
						+ propertyName, null, newValue);
				if (ex.getTargetException() instanceof ClassCastException) {
					throw new TypeMismatchException(propertyChangeEvent, pd.getPropertyType(), ex.getTargetException());
				} else {
					throw new MethodInvocationException(ex.getTargetException(), propertyChangeEvent);
				}
			} catch (IllegalAccessException ex) {
				throw new FatalBeanException("Illegal attempt to set property [" + value + "] threw exception", ex);
			} catch (IllegalArgumentException ex) {
				PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(this.object, this.nestedPath
						+ propertyName, null, newValue);
				throw new TypeMismatchException(propertyChangeEvent, pd.getPropertyType(), ex);
			}
		}
	}

	public void setPropertyValue(PropertyValue pv) throws BeansException {
		setPropertyValue(pv.getName(), pv.getValue());
	}

	public void setPropertyValues(Map map) throws BeansException {
		setPropertyValues(new MutablePropertyValues(map));
	}

	public void setPropertyValues(PropertyValues pvs) throws BeansException {
		setPropertyValues(pvs, false);
	}

	public void setPropertyValues(PropertyValues propertyValues, boolean ignoreUnknown) throws BeansException {
		List propertyAccessExceptions = new ArrayList();
		PropertyValue[] pvs = propertyValues.getPropertyValues();
		for (int i = 0; i < pvs.length; i++) {
			try {
				setPropertyValue(pvs[i]);
			} catch (NotWritablePropertyException ex) {
				if (!ignoreUnknown) {
					throw ex;
				}
			} catch (TypeMismatchException ex) {
				propertyAccessExceptions.add(ex);
			} catch (MethodInvocationException ex) {
				propertyAccessExceptions.add(ex);
			}
		}

		if (!propertyAccessExceptions.isEmpty()) {
			Object[] paeArray = propertyAccessExceptions.toArray(new PropertyAccessException[propertyAccessExceptions
					.size()]);
			throw new PropertyAccessExceptionsException(this, (PropertyAccessException[]) paeArray);
		}
	}

	private PropertyChangeEvent createPropertyChangeEvent(String propertyName, Object oldValue, Object newValue)
			throws BeansException {
		return new PropertyChangeEvent((this.object != null ? this.object : "constructor"),
				(propertyName != null ? this.nestedPath + propertyName : null), oldValue, newValue);
	}

	public Object doTypeConversionIfNecessary(Object newValue, Class requiredType) throws BeansException {
		return doTypeConversionIfNecessary(null, null, null, newValue, requiredType);
	}

	protected Object doTypeConversionIfNecessary(String propertyName, String propertyDescriptor, Object oldValue, Object newValue, Class requiredType) throws BeansException {
		if (newValue != null) {

			if (requiredType.isArray()) {
				Class componentType = requiredType.getComponentType();
				if (newValue instanceof List) {
					List list = (List) newValue;
					Object result = Array.newInstance(componentType, list.size());
					for (int i = 0; i < list.size(); i++) {
						Object value = doTypeConversionIfNecessary(propertyName, propertyName + "[" + i + "]", null,
								list.get(i), componentType);
						Array.set(result, i, value);
					}
					return result;
				} else if (newValue instanceof Object[]) {
					Object[] array = (Object[]) newValue;
					Object result = Array.newInstance(componentType, array.length);
					for (int i = 0; i < array.length; i++) {
						Object value = doTypeConversionIfNecessary(propertyName, propertyName + "[" + i + "]", null,
								array[i], componentType);
						Array.set(result, i, value);
					}
					return result;
				}
			}

			PropertyEditor pe = findCustomEditor(requiredType, propertyName);

			if (pe != null || !requiredType.isAssignableFrom(newValue.getClass())) {

				if (newValue instanceof String[]) {
					if (logger.isDebugEnabled()) {
						logger.debug("Converting String array to comma-delimited String [" + newValue + "]");
					}
					newValue = StringUtils.arrayToCommaDelimitedString((String[]) newValue);
				}

				if (newValue instanceof String) {
					if (pe == null) {
						pe = findDefaultEditor(requiredType);
						if (pe == null) {
							pe = PropertyEditorManager.findEditor(requiredType);
						}
					}
					if (pe != null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Converting String to [" + requiredType + "] using property editor [" + pe + "]");
						}
						try {
							pe.setAsText((String) newValue);
							newValue = pe.getValue();
						} catch (IllegalArgumentException ex) {
							throw new TypeMismatchException(createPropertyChangeEvent(propertyDescriptor, oldValue,
									newValue), requiredType, ex);
						}
					} else {
						throw new TypeMismatchException(createPropertyChangeEvent(propertyDescriptor, oldValue,
								newValue), requiredType);
					}
				}

				else if (pe != null) {
					try {
						pe.setValue(newValue);
						newValue = pe.getValue();
					} catch (IllegalArgumentException ex) {
						throw new TypeMismatchException(createPropertyChangeEvent(propertyDescriptor, oldValue,
								newValue), requiredType, ex);
					}
				}
			}

			if (requiredType.isArray() && !newValue.getClass().isArray()) {
				Class componentType = requiredType.getComponentType();
				Object result = Array.newInstance(componentType, 1);
				Object val = doTypeConversionIfNecessary(propertyName, propertyName + "[" + 0 + "]", null, newValue, componentType);
				Array.set(result, 0, val);
				return result;
			}
		}

		return newValue;
	}
	
	private PropertyEditor findDefaultEditor(Class type) {
		Class editorClass = (Class) defaultEditors.get(type);
		if (editorClass != null) {
			return (PropertyEditor) BeanUtils.instantiateClass(editorClass);
		}
		else {
			return null;
		}
	}
	
	public PropertyDescriptor[] getPropertyDescriptors() {
		return this.cachedIntrospectionResults.getBeanInfo().getPropertyDescriptors();
	}
	
	public PropertyDescriptor getPropertyDescriptor(String propertyName) throws BeansException {
		if (propertyName == null) {
			throw new FatalBeanException("Can't find property descriptor for null property");
		}
		if (isNestedProperty(propertyName)) {
			BeanWrapper nestedBw = getBeanWrapperForPropertyPath(propertyName);
			return nestedBw.getPropertyDescriptor(getFinalPath(propertyName));
		}
		return this.cachedIntrospectionResults.getPropertyDescriptor(propertyName);
	}
	
	public boolean isReadableProperty(String propertyName) {
		if (propertyName == null) {
			throw new FatalBeanException("Can't find readability status for null property");
		}
		try {
			return getPropertyDescriptor(propertyName).getReadMethod() != null;
		}
		catch (BeansException ex) {
			return false;
		}
	}
	
	public boolean isWritableProperty(String propertyName) {
		if (propertyName == null) {
			throw new FatalBeanException("Can't find writability status for null property");
		}
		try {
			return getPropertyDescriptor(propertyName).getWriteMethod() != null;
		}
		catch (BeansException ex) {
			return false;
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		try {
			sb.append("BeanWrapperImpl:"
								+ " wrapping class [" + getWrappedInstance().getClass().getName() + "]; ");
			PropertyDescriptor pds[] = getPropertyDescriptors();
			if (pds != null) {
				for (int i = 0; i < pds.length; i++) {
					Object val = getPropertyValue(pds[i].getName());
					String valStr = (val != null) ? val.toString() : "null";
					sb.append(pds[i].getName() + "={" + valStr + "}");
				}
			}
		}
		catch (Exception ex) {
			sb.append("exception encountered: " + ex);
		}
		return sb.toString();
	}

}
