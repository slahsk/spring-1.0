package org.springframework.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class AbstractResource implements Resource{
	protected static final String URL_PROTOCOL_FILE = "file";
	
	public boolean exists() {
		try {
			return getFile().exists();
		}
		catch (IOException ex) {
			try {
				//파일이 열려 있으면 true??
				InputStream is = getInputStream();
				is.close();
				return true;
			}
			catch (IOException ex2) {
				return false;
			}
		}
	}
	
	public boolean isOpen() {
		return false;
	}
	
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}
	
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}
	
	public String toString() {
		return getDescription();
	}
	
	public boolean equals(Object obj) {
		return (obj instanceof Resource && ((Resource) obj).getDescription().equals(getDescription()));
	}
	
	public int hashCode() {
		return getDescription().hashCode();
	}
}
