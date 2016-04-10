package org.springframework.context.support;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.beans.factory.xml.BeansDtdResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.xml.sax.InputSource;

public class ResourceEntityResolver extends BeansDtdResolver{
	
	private final ApplicationContext applicationContext;

	public ResourceEntityResolver(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
	public InputSource resolveEntity(String publicId, String systemId) throws IOException {
		//부모 메소드 실행 결과값 그대로 가져와서 사용
		InputSource source = super.resolveEntity(publicId, systemId);
		if (source == null && systemId != null) {
			String resourcePath = null;
			try {
				String givenUrl = new URL(systemId).toString();
				String systemRootUrl = new File("").toURL().toString();
				if (givenUrl.startsWith(systemRootUrl)) {
					resourcePath = givenUrl.substring(systemRootUrl.length());
				}
			}
			catch (MalformedURLException ex) {
				resourcePath = systemId;
			}
			if (resourcePath != null) {
				logger.debug("Trying to locate entity [" + systemId + "] as application context resource [" + resourcePath + "]");
				Resource resource = this.applicationContext.getResource(resourcePath);
				logger.info("Found entity [" + systemId + "] as application context resource [" + resourcePath + "]");
				source = new InputSource(resource.getInputStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
			}
		}
		return source;
	}
}
