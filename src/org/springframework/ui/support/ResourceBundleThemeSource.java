package org.springframework.ui.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.SimpleTheme;

public class ResourceBundleThemeSource implements HierarchicalThemeSource{
	
	protected final Log logger = LogFactory.getLog(getClass());

	private ThemeSource parentThemeSource;

	private String basenamePrefix = "";
	
	private Map themes = new HashMap();

	public void setParentThemeSource(ThemeSource parent) {
		this.parentThemeSource = parent;
		Iterator it = this.themes.values().iterator();
		while (it.hasNext()) {
			initParent((Theme) it.next());
		}
	}

	public ThemeSource getParentThemeSource() {
		return parentThemeSource;
	}
	
	public void setBasenamePrefix(String basenamePrefix) {
		this.basenamePrefix = (basenamePrefix != null) ? basenamePrefix : "";
	}

	public Theme getTheme(String themeName) {
		if (themeName == null) {
			return null;
		}
		Theme theme = (Theme) this.themes.get(themeName);
		//테마가 없으면 테마생성
		if (theme == null) {
			ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
			logger.info("Theme created: name=" + themeName + ", baseName=" + this.basenamePrefix + themeName);
			messageSource.setBasename(this.basenamePrefix + themeName);
			theme = new SimpleTheme(themeName, messageSource);
			//새로 생성된 테마를 부모에도 주입
			initParent(theme);
			this.themes.put(themeName, theme);
		}
		return theme;
	}
	
	protected void initParent(Theme theme) {
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) theme.getMessageSource();
		//부모객체가 있으면
		if (this.parentThemeSource != null) {
			Theme parentTheme = this.parentThemeSource.getTheme(theme.getName());
			if (parentTheme != null) {
				//부모 messsageSource 주입
				messageSource.setParentMessageSource(parentTheme.getMessageSource());
			}
		}
		else {
			messageSource.setParentMessageSource(null);
		}
	}

}
