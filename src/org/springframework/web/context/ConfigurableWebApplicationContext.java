package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.ConfigurableApplicationContext;

public interface ConfigurableWebApplicationContext extends WebApplicationContext, ConfigurableApplicationContext{
	
	String CONFIG_LOCATION_DELIMITERS = ",; ";
	
	void setServletContext(ServletContext servletContext);
	
	void setNamespace(String namespace);
	
	void setConfigLocations(String[] configLocations);

}
