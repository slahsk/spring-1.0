package org.springframework.core.io;

import java.beans.PropertyEditorSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ResourceEditor extends PropertyEditorSupport{
	
	protected static final Log logger = LogFactory.getLog(ResourceEditor.class);
	
	public static final String PLACEHOLDER_PREFIX = "${";

	public static final String PLACEHOLDER_SUFFIX = "}";

	public void setAsText(String text) {
		setValue(getResourceLoader().getResource(resolvePath(text)));
	}
	
	protected String resolvePath(String path) {
		int startIndex = path.indexOf(PLACEHOLDER_PREFIX);
		if (startIndex != -1) {
			int endIndex = path.indexOf(PLACEHOLDER_SUFFIX, startIndex + PLACEHOLDER_PREFIX.length());
			if (endIndex != -1) {
				String placeholder = path.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
				String propVal = System.getProperty(placeholder);
				if (propVal != null) {
					return path.substring(0, startIndex) + propVal + path.substring(endIndex+1);
				}
				else {
					logger.warn("Could not resolve placeholder '" + placeholder +
					            "' in file path [" + path + "] as system property");
				}
			}
		}
		return path;
	}
	
	protected ResourceLoader getResourceLoader() {
		return new DefaultResourceLoader();
	}

}
