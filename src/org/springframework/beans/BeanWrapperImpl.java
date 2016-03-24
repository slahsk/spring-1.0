package org.springframework.beans;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BeanWrapperImpl implements BeanWrapper{
	private static final Log logger = LogFactory.getLog(BeanWrapperImpl.class);
	private static final Map defaultEditors = new HashMap();
	
	static {
		defaultEditors.put(Class.class, ClassEditor.class);
		defaultEditors.put(File.class, FileEditor.class);
		defaultEditors.put(Locale.class, LocaleEditor.class);
		defaultEditors.put(Properties.class, PropertiesEditor.class);
		defaultEditors.put(String[].class, StringArrayPropertyEditor.class);
		defaultEditors.put(URL.class, URLEditor.class);
	}
	
	private Object object;
	private String nestedPath = "";
	private Map nestedBeanWrappers;
	private Map customEditors;
	
	private CachedIntrospectionResults cachedIntrospectionResults;
	
	
}
