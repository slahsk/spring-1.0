package org.springframework.context.event;

import org.springframework.context.ApplicationListener;

public interface ApplicationEventMulticaster extends ApplicationListener{
	
	void addApplicationListener(ApplicationListener listener);
	
	void removeApplicationListener(ApplicationListener listener);
	
	void removeAllListeners();

}
