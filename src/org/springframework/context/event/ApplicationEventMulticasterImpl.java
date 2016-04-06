package org.springframework.context.event;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class ApplicationEventMulticasterImpl implements ApplicationEventMulticaster{
	private Set eventListeners = new HashSet();
	
	public void addApplicationListener(ApplicationListener l) {
		eventListeners.add(l);
	}
	
	public void removeApplicationListener(ApplicationListener l) {
		eventListeners.remove(l);
	}
	
	//이벤트 실행
	public void onApplicationEvent(ApplicationEvent e) {
		Iterator i = eventListeners.iterator();
		while (i.hasNext()) {
			ApplicationListener l = (ApplicationListener) i.next();
			l.onApplicationEvent(e);
		}
	}
	
	public void removeAllListeners() {
		eventListeners.clear();
	}
	
}
