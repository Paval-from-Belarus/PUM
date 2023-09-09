package common;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.petos.pum.networks.transfer.BinaryObjectProperties;
import org.petos.pum.networks.transfer.BinaryObjectMapper;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Configuration {
public static final String TRANSFER_RESOURCE = "transfer-packages.xml";
public static final String PACKAGES_INFO_FILE = "info.pum";
public static final String PUBLISHER_CONFIG = "author.pum";
public String programs;
public String libraries;
public String temp;
public String packagesInfo;
public String infoPath;
public String publishers;
public String repositories;

public void init() {
      var list = List.of(programs, libraries, temp, packagesInfo, publishers);
      try {
	    for (String path : list) {
		  File dir = new File(path);
		  if (!dir.exists()) {
			Files.createDirectory(dir.toPath());
		  }
	    }
	    Path infoPath = Path.of(packagesInfo, PACKAGES_INFO_FILE);
	    Path publisherPath = Path.of(publishers).resolve(PUBLISHER_CONFIG);
	    Path repoPath = Path.of(repositories);
	    if (!Files.exists(infoPath)) {
		  Files.createFile(infoPath);
	    }
	    if (!Files.exists(publisherPath)) {
		  Files.createFile(publisherPath);
	    }
	    if (!Files.exists(repoPath)) {
		  Files.createFile(repoPath);
	    }
	    this.publishers = publisherPath.toAbsolutePath().toString();
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
	    if (line.contains("=")) {
		  name = parts[0];
		  parts = Arrays.copyOfRange(parts, 1, parts.length);
	    }
      }
      return Map.of();
}

public void setSerializer(BinaryObjectMapper binaryObjectMapper) {
      Document document;
      ClassLoader loader = Client.class.getClassLoader();
      URL resource = loader.getResource(TRANSFER_RESOURCE);
      assert resource != null; //the resource cannot be null
      BinaryObjectProperties transfer;
      try {
	    transfer = new BinaryObjectProperties(binaryObjectMapper);
	    transfer.configure(resource.toURI());
      } catch (ClassNotFoundException e) {
	    throw new IllegalStateException("Class in config file is not specified");
      } catch (URISyntaxException e) {
	    throw new IllegalStateException("Impossible to configure the serializer");
      }
}

}
