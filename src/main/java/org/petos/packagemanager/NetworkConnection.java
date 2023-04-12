package org.petos.packagemanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NetworkConnection {
	private final InputStream input;
	private final OutputStream output;
	NetworkConnection(InputStream input, OutputStream output){
	      this.input = input;
	      this.output = output;
	}
	public void close() throws IOException {
	      input.close();
	      output.close();
	}
}
