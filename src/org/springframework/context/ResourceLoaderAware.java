package org.springframework.context;

import org.springframework.core.io.ResourceLoader;

public interface ResourceLoaderAware {
	void setResourceLoader(ResourceLoader resourceLoader);
}
