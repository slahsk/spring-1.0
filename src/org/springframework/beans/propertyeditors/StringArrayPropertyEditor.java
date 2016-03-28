package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

public class StringArrayPropertyEditor extends PropertyEditorSupport{
	public void setAsText(String s) throws IllegalArgumentException {
		String[] sa = StringUtils.commaDelimitedListToStringArray(s);
		setValue(sa);
	}

	public String getAsText() {
		String[] array = (String[]) this.getValue();
		return StringUtils.arrayToCommaDelimitedString(array);
	}
}
