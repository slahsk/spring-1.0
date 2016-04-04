package org.springframework.beans.factory.access;

import org.springframework.beans.FatalBeanException;

public class BootstrapException extends FatalBeanException{
	
	public BootstrapException(String msg) {
		super(msg);
	}

	public BootstrapException(String msg, Throwable ex) {
		super(msg, ex);
	}
}
