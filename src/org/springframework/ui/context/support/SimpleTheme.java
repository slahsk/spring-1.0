package org.springframework.ui.context.support;

import org.springframework.context.MessageSource;
import org.springframework.ui.context.Theme;

public class SimpleTheme implements Theme{
	
	private String name;

	private MessageSource messageSource;

	public SimpleTheme(String name, MessageSource messageSource) {
		this.name = name;
		this.messageSource = messageSource;
	}

	public String getName() {
		return name;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

}
