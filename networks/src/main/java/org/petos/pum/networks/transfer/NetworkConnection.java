package org.petos.pum.networks.transfer;

import java.io.*;

public class NetworkConnection {
	private final InputStream input;
	private final ByteArrayOutputStream output;
	public NetworkConnection(InputStream input){
	      this.input = input;
	      this.output = new ByteArrayOutputStream();
	}
	public InputStream getInput(){
	      return input;
	}
	public OutputStream getOutput(){
	      return output;
	}
	public byte[] collect(){
	      return output.toByteArray();
	}
	public void close() throws IOException {
	      input.close();
	      output.close();
	}
}
