package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class FileSystemXmlApplicationContext extends AbstractXmlApplicationContext{
	
	private String[] configLocations;
	
	public FileSystemXmlApplicationContext(String configLocation) throws BeansException {
		this.configLocations = new String[] {configLocation};
		refresh();
	}
	
	public FileSystemXmlApplicationContext(String[] configLocations) throws BeansException {
		this.configLocations = configLocations;
		refresh();
	}
	
	public FileSystemXmlApplicationContext(String[] configLocations, ApplicationContext parent)
			throws BeansException {
		super(parent);
		this.configLocations = configLocations;
		refresh();
	}

	protected String[] getConfigLocations() {
		return this.configLocations;
	}
	
	//AbstractXmlApplicationContext하고 유일한 차이점이 이메소드 하나이다
	protected Resource getResourceByPath(String path) {
		if (path != null && path.startsWith("/")) {
			path = path.substring(1);
		}
		return new FileSystemResource(path);
	}

}
