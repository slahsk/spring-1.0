package org.springframework.beans;

public class PropertyValue {
	private String name;
	private Object value;
	
	public PropertyValue(String name, Object value) {
		if (name == null) {
			throw new IllegalArgumentException("Property name cannot be null");
		}
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	
	public Object getValue() {
		return value;
	}
	
	public String toString() {
		return "PropertyValue: name='" + name + "'; value=[" + value + "]";
	}

	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PropertyValue)) {
			return false;
		}
		PropertyValue otherPv = (PropertyValue) other;
		//°ªºñ±³
		return (this.name.equals(otherPv.name) &&
				((this.value == null && otherPv.value == null) || this.value.equals(otherPv.value)));
	}

	public int hashCode() {
		return this.name.hashCode() * 29 + (this.value != null ? this.value.hashCode() : 0);
	}
}
