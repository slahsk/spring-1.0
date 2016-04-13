package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.context.ServletContextAware;

public class ServletContextAwareProcessor implements BeanPostProcessor{
	
	protected final Log logger = LogFactory.getLog(getClass());

	private final ServletContext servletContext;
	
	public ServletContextAwareProcessor(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
	
	//beanFactory 생성전에 서블릿 주입
	public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
		if (bean instanceof ServletContextAware) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking setServletContext on ServletContextAware bean '" + name + "'");
			}
			((ServletContextAware) bean).setServletContext(this.servletContext);
		}
		return bean;
	}
	
	public Object postProcessAfterInitialization(Object bean, String name) {
		return bean;
	}

}
