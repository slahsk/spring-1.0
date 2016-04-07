package org.springframework.context;

import org.springframework.beans.BeansException;

public interface ApplicationContextAware {
	void setApplicationContext(ApplicationContext context) throws BeansException;

}
