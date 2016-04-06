package org.springframework.context;

import java.util.EventListener;

public interface ApplicationListener extends EventListener{
	void onApplicationEvent(ApplicationEvent e);
}
