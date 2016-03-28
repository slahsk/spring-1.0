package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.File;

public class FileEditor extends PropertyEditorSupport{
	
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(new File(text));
	}

	public String getAsText() {
		return ((File) getValue()).getAbsolutePath();
	}

}
