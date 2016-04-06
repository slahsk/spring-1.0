package org.springframework.core.io;

import java.net.MalformedURLException;
import java.net.URL;

public class DefaultResourceLoader implements ResourceLoader{
	
	public Resource getResource(String location) {
		//claspath 가 포함 되어 있으면
		if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()));
		}
		else {
			try {
				URL url = new URL(location);
				return new UrlResource(url);
			}
			catch (MalformedURLException ex) {
				return getResourceByPath(location);
			}
		}
	}
	
	protected Resource getResourceByPath(String path) {
		return new ClassPathResource(path);
	}
}
