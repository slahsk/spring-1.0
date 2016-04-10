package org.springframework.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class FileSystemResource extends AbstractResource{
	
	private final File file;
	
	public FileSystemResource(File file) {
		this.file = file;
	}
	
	public FileSystemResource(String path) {
		this.file = new File(path);
	}
	
	public boolean exists() {
		return this.file.exists();
	}
	
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(this.file);
	}
	
	public URL getURL() throws IOException {
		return new URL(URL_PROTOCOL_FILE + ":" + this.file.getAbsolutePath());
	}
	
	public File getFile() {
		return file;
	}
	
	public String getDescription() {
		return "file [" + this.file.getAbsolutePath() + "]";
	}

}
