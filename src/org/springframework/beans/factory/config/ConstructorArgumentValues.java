package org.springframework.beans.factory.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstructorArgumentValues {
	//숫자로 조회하는 맵
	private Map indexedArgumentValues = new HashMap();
	
	
	private Set genericArgumentValues = new HashSet();
	
	public void addIndexedArgumentValue(int index, Object value) {
		this.indexedArgumentValues.put(new Integer(index), new ValueHolder(value));
	}
	
	public void addIndexedArgumentValue(int index, Object value, String type) {
		this.indexedArgumentValues.put(new Integer(index), new ValueHolder(value, type));
	}
	
	//requiredType 은 map 에서 가져온 ValueHolder name 하고 같은지 검사하기 위해 사용
	public ValueHolder getIndexedArgumentValue(int index, Class requiredType) {
		ValueHolder valueHolder = (ValueHolder) this.indexedArgumentValues.get(new Integer(index));
		if (valueHolder != null) {
			if (valueHolder.getType() == null || requiredType.getName().equals(valueHolder.getType())) {
				return valueHolder;
			}
		}
		return null;
	}
	
	public Map getIndexedArgumentValues() {
		return indexedArgumentValues;
	}
	
	public void addGenericArgumentValue(Object value) {
		this.genericArgumentValues.add(new ValueHolder(value));
	}
	
	public void addGenericArgumentValue(Object value, String type) {
		this.genericArgumentValues.add(new ValueHolder(value, type));
	}
	
	public ValueHolder getGenericArgumentValue(Class requiredType) {
		for (Iterator it = this.genericArgumentValues.iterator(); it.hasNext();) {
			ValueHolder valueHolder = (ValueHolder) it.next();
			Object value = valueHolder.getValue();
			if (valueHolder.getType() != null) {
				if (valueHolder.getType().equals(requiredType.getName())) {
					return valueHolder;
				}
			}
			else if (requiredType.isInstance(value) || (requiredType.isArray() && List.class.isInstance(value))) {
				return valueHolder;
			}
		}
		return null;
	}
	
	public Set getGenericArgumentValues() {
		return this.genericArgumentValues;
	}
	
	public ValueHolder getArgumentValue(int index, Class requiredType) {
		ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType);
		if (valueHolder == null) {
			valueHolder = getGenericArgumentValue(requiredType);
		}
		return valueHolder;
	}
	
	public int getNrOfArguments() {
		return this.indexedArgumentValues.size() + this.genericArgumentValues.size();
	}
	
	public boolean isEmpty() {
		return this.indexedArgumentValues.isEmpty() && this.genericArgumentValues.isEmpty();
	}
	
	public static class ValueHolder {
		private Object value;
		private String type;

		private ValueHolder(Object value) {
			this.value = value;
		}

		private ValueHolder(Object value, String type) {
			this.value = value;
			this.type = type;
		}
		
		public void setValue(Object value) {
			this.value = value;
		}

		public Object getValue() {
			return value;
		}

		public String getType() {
			return type;
		}
		
	}
}
