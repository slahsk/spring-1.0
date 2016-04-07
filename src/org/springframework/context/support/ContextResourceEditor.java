package org.springframework.context.support;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceEditor;

public class ContextResourceEditor extends ResourceEditor{
	
	private final ApplicationContext applicationContext;
	
	public ContextResourceEditor(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public void setAsText(String text) throws IllegalArgumentException {
		String resolvedPath = resolvePath(text);
		setValue(this.applicationContext.getResource(resolvedPath));
	}

}
