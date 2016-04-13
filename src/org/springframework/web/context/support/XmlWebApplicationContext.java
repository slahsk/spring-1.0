package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.support.UiApplicationContextUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;

public class XmlWebApplicationContext extends AbstractXmlApplicationContext implements ConfigurableWebApplicationContext{
	
	public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml";
	
	public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";
	
	public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".xml";
	
	private ServletContext servletContext;
	
	private String namespace = null;
	
	private String[] configLocations;
	
	private ThemeSource themeSource;
	
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public ServletContext getServletContext() {
		return this.servletContext;
	}
	
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	protected String getNamespace() {
		return this.namespace;
	}
	
	public void setConfigLocations(String[] configLocations) {
		this.configLocations = configLocations;
	}

	protected String[] getConfigLocations() {
		return this.configLocations;
	}
	
	public void refresh() throws BeansException {
		//xml 파일 이름 사용자 지정시
		if (this.namespace != null) {
			setDisplayName("XmlWebApplicationContext for namespace '" + this.namespace + "'");
			if (this.configLocations == null || this.configLocations.length == 0) {
				this.configLocations = new String[] {DEFAULT_CONFIG_LOCATION_PREFIX + this.namespace +
						DEFAULT_CONFIG_LOCATION_SUFFIX};
			}
		}
		else {
			setDisplayName("Root XmlWebApplicationContext");
			if (this.configLocations == null || this.configLocations.length == 0) {
				this.configLocations = new String[] {DEFAULT_CONFIG_LOCATION};
			}
		}
		super.refresh();
	}
	
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
		//서블릿은 의존주입 제외
		beanFactory.ignoreDependencyType(ServletContext.class);
	}
	
	//file , url, inputStream 제공해주는 객체 새로 생성
	protected Resource getResourceByPath(String path) {
		if (path != null && !path.startsWith("/")) {
			path = "/" + path;
		}
		return new ServletContextResource(this.servletContext, path);
	}
	
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}

}
