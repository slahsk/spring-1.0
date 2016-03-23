package org.springframework.beans;

public class FatalBeanException extends BeansException{
	
	public FatalBeanException(String msg) {
		super(msg);
	}
	
	public FatalBeanException(String msg, Throwable ex) {
		super(msg, ex);
	}

}
