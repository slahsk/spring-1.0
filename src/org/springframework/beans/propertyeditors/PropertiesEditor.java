package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class PropertiesEditor extends PropertyEditorSupport{
	public final static String COMMENT_MARKERS = "#!";
	
	public void setAsText(String text) throws IllegalArgumentException {
		if (text == null) {
			throw new IllegalArgumentException("Cannot set Properties to null");
		}
		Properties props = new Properties();
		try {
			props.load(new ByteArrayInputStream(text.getBytes()));
			dropComments(props);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to parse [" + text + "] into Properties");
		}
		setValue(props);
	}
	
	//주석제거 #!
	private void dropComments(Properties props) {
		Iterator keys = props.keySet().iterator();
		List commentKeys = new ArrayList();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			if (key.length() > 0 && COMMENT_MARKERS.indexOf(key.charAt(0)) != -1) {
				commentKeys.add(key);
			}
		}
		for (Iterator it = commentKeys.iterator(); it.hasNext();) {
			String key = (String) it.next();
			props.remove(key);
		}
	}
}
