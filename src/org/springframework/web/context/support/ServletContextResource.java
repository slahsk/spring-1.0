package org.springframework.web.context.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletContext;

import org.springframework.core.io.AbstractResource;

public class ServletContextResource extends AbstractResource{
	
	private final ServletContext servletContext;
	
	private final String path;
	
	public ServletContextResource(ServletContext servletContext, String path) {
		this.servletContext = servletContext;
		this.path = path;
	}
	
	public InputStream getInputStream() throws IOException {
		InputStream is = this.servletContext.getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException("Could not open " + getDescription());
		}
		return is;
	}
	
	public URL getURL() throws IOException {
		URL url = this.servletContext.getResource(this.path);
		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL " +
																			"because it does not exist");
		}
		return url;
	}
	
	public File getFile() throws IOException {
		String realPath = this.servletContext.getRealPath(this.path);
		if (realPath == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path - " +
																			"web application archive not expanded?");
		}
		return new File(realPath);
	}

	public String getDescription() {
		return "resource [" + this.path + "] of ServletContext";
	}


}
