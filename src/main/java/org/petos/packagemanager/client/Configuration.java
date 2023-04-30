package org.petos.packagemanager.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;

public class Configuration {
public static final String PACKAGES_INFO_FILE = "info.pum";
public String programs;
public String libraries;
public String temp;
public String packages;
public String infoPath;

public void init() {
      var list = List.of(programs, libraries, temp, packages);
      try {
	    for (String path : list) {
		  File dir = new File(path);
		  if (!dir.exists()) {
			Files.createDirectory(dir.toPath());
		  }
	    }
	    Path infoPath = Path.of(packages, PACKAGES_INFO_FILE);
	    if (!Files.exists(infoPath)){
		  Files.createFile(infoPath);
	    }
	    this.infoPath = infoPath.toAbsolutePath().toString();
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }
}
}
