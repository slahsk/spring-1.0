package org.springframework.beans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

public class MutablePropertyValues implements PropertyValues{
	private List propertyValuesList;
	
	public MutablePropertyValues() {
		this.propertyValuesList = new ArrayList(10);
	}
	
	public MutablePropertyValues(PropertyValues other) {
		this();
		if (other != null) {
			PropertyValue[] pvs = other.getPropertyValues();
			this.propertyValuesList = new ArrayList(pvs.length);
			//array -> list
			for (int i = 0; i < pvs.length; i++) {
				addPropertyValue(new PropertyValue(pvs[i].getName(), pvs[i].getValue()));
			}
		}
	}
	
	public MutablePropertyValues(Map map) {
		Set keys = map.keySet();
		this.propertyValuesList = new ArrayList(keys.size());
		Iterator itr = keys.iterator();
		while (itr.hasNext()) {
			String key = (String) itr.next();
			addPropertyValue(new PropertyValue(key, map.get(key)));
		}
	}
	
	public void addPropertyValue(PropertyValue pv) {
		for (int i = 0; i < this.propertyValuesList.size(); i++) {
			PropertyValue currentPv = (PropertyValue) this.propertyValuesList.get(i);
			//같은 이름 있으면 set 없으면 add
			if (currentPv.getName().equals(pv.getName())) {
				this.propertyValuesList.set(i, pv);
				return;
			}
		}
		this.propertyValuesList.add(pv);
	}
	
	public void addPropertyValue(String propertyName, Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
	}
	
	public void removePropertyValue(PropertyValue pv) {
		this.propertyValuesList.remove(pv);
	}
	
	public void removePropertyValue(String propertyName) {
		removePropertyValue(getPropertyValue(propertyName));
	}
	
	public void setPropertyValueAt(PropertyValue pv, int i) {
		this.propertyValuesList.set(i, pv);
	}
	
	public PropertyValue[] getPropertyValues() {
		return (PropertyValue[]) this.propertyValuesList.toArray(new PropertyValue[0]);
	}
	
	public PropertyValue getPropertyValue(String propertyName) {
		for (int i = 0; i < this.propertyValuesList.size(); i++) {
			PropertyValue pv = (PropertyValue) this.propertyValuesList.get(i);
			if (pv.getName().equals(propertyName)) {
				return pv;
			}
		}
		return null;
	}
	
	public boolean contains(String propertyName) {
		return getPropertyValue(propertyName) != null;
	}
	
	
	//같음 이름 객체를 변경
	public PropertyValues changesSince(PropertyValues old) {
		MutablePropertyValues changes = new MutablePropertyValues();
		if (old == this)
			return changes;

		for (int i = 0; i < this.propertyValuesList.size(); i++) {
			PropertyValue newPv = (PropertyValue) this.propertyValuesList.get(i);
			PropertyValue pvOld = old.getPropertyValue(newPv.getName());
			
			//old 에 없어도
			if (pvOld == null) {
				changes.addPropertyValue(newPv);
			}
			//old 에 있지만  다른 객체라면 
			else if (!pvOld.equals(newPv)) {
				// It's changed
				changes.addPropertyValue(newPv);
			}
		}
		return changes;
	}
	
	public String toString() {
		PropertyValue[] pvs = getPropertyValues();
		StringBuffer sb = new StringBuffer("MutablePropertyValues: length=" + pvs.length + "; ");
		sb.append(StringUtils.arrayToDelimitedString(pvs, ","));
		return sb.toString();
	}


}

