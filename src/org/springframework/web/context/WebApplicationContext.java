package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.ui.context.ThemeSource;

public interface WebApplicationContext extends ApplicationContext, ThemeSource{
	
	String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class + ".ROOT";
	
	ServletContext getServletContext();

}
