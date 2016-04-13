package org.springframework.context.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.springframework.util.StringUtils;

public class ResourceBundleMessageSource extends AbstractMessageSource{
	private String[] basenames;
	
	private final Map cachedMessageFormats = new HashMap();
	
	public void setBasename(String basename) {
		setBasenames(new String[] {basename});
	}
	
	public void setBasenames(String[] basenames)  {
		this.basenames = basenames;
	}
	
	protected final MessageFormat resolveCode(String code, Locale locale) {
		MessageFormat messageFormat = null;
		for (int i = 0; messageFormat == null && i < this.basenames.length; i++) {
			messageFormat = resolve(this.basenames[i], code, locale);
		}
		return messageFormat;
	}
	
	protected MessageFormat resolve(String basename, String code, Locale locale) {
		try {
			ResourceBundle bundle = ResourceBundle.getBundle(basename, locale, Thread.currentThread().getContextClassLoader());
			try {
				return getMessageFormat(bundle, code);
			}
			catch (MissingResourceException ex) {
				return null;
			}
		}
		catch (MissingResourceException ex) {
			logger.warn("ResourceBundle [" + basename + "] not found for MessageSource: " + ex.getMessage());
			return null;
		}
	}
	
	protected MessageFormat getMessageFormat(ResourceBundle bundle, String code) throws MissingResourceException {
		synchronized (this.cachedMessageFormats) {
			Map codeMap = (Map) this.cachedMessageFormats.get(bundle);
			if (codeMap != null) {
				MessageFormat result = (MessageFormat) codeMap.get(code);
				if (result != null) {
					return result;
				}
			}
			String msg = bundle.getString(code);
			if (msg != null) {
				MessageFormat result = new MessageFormat(msg);
				if (codeMap != null) {
					codeMap.put(code, result);
				}
				else {
					codeMap = new HashMap();
					codeMap.put(code, result);
					this.cachedMessageFormats.put(bundle, codeMap);
				}
				return result;
			}
			return null;
		}
	}
	
	public String toString() {
		return getClass().getName() + " with basenames [" + StringUtils.arrayToCommaDelimitedString(this.basenames) + "]";
	}

}
