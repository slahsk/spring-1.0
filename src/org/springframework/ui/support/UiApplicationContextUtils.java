package org.springframework.ui.support;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.ThemeSource;

public class UiApplicationContextUtils {
	
	public static final String THEME_SOURCE_BEAN_NAME = "themeSource";

	private static final Log logger = LogFactory.getLog(UiApplicationContextUtils.class);
	
	public static ThemeSource initThemeSource(ApplicationContext context) {
		
		ThemeSource themeSource;
		try {
			//테마소스 객체 가져오기
			themeSource = (ThemeSource) context.getBean(THEME_SOURCE_BEAN_NAME);
			
			if (context.getParent() instanceof ThemeSource && themeSource instanceof HierarchicalThemeSource &&
					Arrays.asList(context.getBeanDefinitionNames()).contains(THEME_SOURCE_BEAN_NAME)) {//definition 에서도 있는지 검사
				((HierarchicalThemeSource) themeSource).setParentThemeSource((ThemeSource) context.getParent());
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			logger.info("No ThemeSource found for [" + context.getDisplayName() + "]: using ResourceBundleThemeSource");
			themeSource = new ResourceBundleThemeSource();
		}
		return themeSource;
	}


}
