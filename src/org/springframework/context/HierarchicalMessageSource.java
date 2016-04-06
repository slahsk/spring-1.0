package org.springframework.context;

public interface HierarchicalMessageSource extends MessageSource{
	
	void setParentMessageSource(MessageSource parent);
	
	MessageSource getParentMessageSource();

}
