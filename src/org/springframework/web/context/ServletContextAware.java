package org.springframework.web.context;

import javax.servlet.ServletContext;

public interface ServletContextAware {
	
	void setServletContext(ServletContext servletContext);

}
