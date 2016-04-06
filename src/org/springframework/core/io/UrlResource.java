package org.springframework.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

public class UrlResource extends AbstractResource{
	public static final String PROTOCOL_FILE = "file";

	private final URL url;
	
	public UrlResource(URL url) {
		this.url = url;
	}
	
	public UrlResource(String path) throws MalformedURLException {
		this.url = new URL(path);
	}
	
	public InputStream getInputStream() throws IOException {
		return this.url.openStream();
	}
	
	public File getFile() throws IOException {
		if (PROTOCOL_FILE.equals(this.url.getProtocol())) {
			return new File(URLDecoder.decode(this.url.getFile()));
		}
		else {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path - " +
																			"no 'file:' protocol");
		}
	}
	
	public String getDescription() {
		return "URL [" + this.url + "]";
	}
}
