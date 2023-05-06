package common;

import org.jetbrains.annotations.NotNull;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Configuration {
public static final String PACKAGES_INFO_FILE = "info.pum";
public String programs;
public String libraries;
public String temp;
public String packagesInfo;
public String infoPath;

public void init() {
      var list = List.of(programs, libraries, temp, packagesInfo);

      try {
	    for (String path : list) {
		  File dir = new File(path);
		  if (!dir.exists()) {
			Files.createDirectory(dir.toPath());
		  }
	    }
	    Path infoPath = Path.of(packagesInfo, PACKAGES_INFO_FILE);
	    if (!Files.exists(infoPath)){
		  Files.createFile(infoPath);
	    }
	    this.infoPath = infoPath.toAbsolutePath().toString();
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }
}

//todo :implements to any config files (such as InstanceInfo, Publisher)
public static Map<String, String[]> mapOf(@NotNull String content) {
 	if (content.length() <= 2)
	       return Map.of();
	String[] lines = content.split("\n");
	for (String line : lines) {
	      String name = "";
	      String[] values;
	      String[] parts = line.split("=");
	      if (line.contains("=")){
		    name = parts[0];
		    parts = Arrays.copyOfRange(parts, 1, parts.length);
	      }
	}
	return Map.of();
}
}
