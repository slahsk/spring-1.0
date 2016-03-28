package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.net.MalformedURLException;
import java.net.URL;

public class URLEditor extends PropertyEditorSupport{
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			setValue(new URL(text));
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("Malformed URL: " + ex.getMessage());
		}
	}

	public String getAsText() {
		return ((URL) getValue()).toExternalForm();
	}
}
