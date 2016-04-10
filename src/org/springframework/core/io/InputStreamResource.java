package org.springframework.core.io;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamResource extends AbstractResource{
	
	private InputStream inputStream;

	private final String description;
	
	public InputStreamResource(InputStream inputStream, String description) {
		if (inputStream == null) {
			throw new IllegalArgumentException("inputStream must not be null");
		}
		this.inputStream = inputStream;
		this.description = description;
	}
	
	public boolean exists() {
		return true;
	}

	public boolean isOpen() {
		return true;
	}
	
	public InputStream getInputStream() throws IOException, IllegalStateException {
		if (this.inputStream == null) {
			throw new IllegalStateException("InputStream has already been read - " +
			                                "do not use InputStreamResource if a stream needs to be read multiple times");
		}
		InputStream result = this.inputStream;
		this.inputStream = null;
		return result;
	}

	public String getDescription() {
		return description;
	}

}
