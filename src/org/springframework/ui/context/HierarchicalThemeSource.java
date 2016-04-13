package org.springframework.ui.context;

public interface HierarchicalThemeSource extends ThemeSource{
	
	void setParentThemeSource(ThemeSource parent);
	
	ThemeSource getParentThemeSource();

}
