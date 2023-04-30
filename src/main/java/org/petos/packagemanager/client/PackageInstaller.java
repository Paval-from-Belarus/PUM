package org.petos.packagemanager.client;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.packages.FullPackageInfoDTO;
import org.petos.packagemanager.packages.PackageAssembly;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The current issue: if something go wrong, all package (even existing version) will be removed
 */
public class PackageInstaller {
private static final Logger logger = LogManager.getLogger(PackageInstaller.class);

public static class PackageIntegrityException extends Exception {
      PackageIntegrityException(String msg) {
	    super(msg);
      }
}

public static class ManagerConfigurationException extends Exception {
      ManagerConfigurationException(String msg) {
	    super(msg);
      }
}

public enum PayloadType {Binary, Library, Docs, Config, Unknown}

public enum CommitState {Failed, Success}

;
private Configuration config;

public PackageInstaller(Configuration config) {
      this.config = config;
}

public void initSession() {
      centralPath = null;
      dependencies = new ArrayList<>();
      try {
	    tempConfig = Files.createTempFile(Path.of(getTempPath()), "config", "pum");
      } catch (IOException e) {
	    //really interesting case...
	    throw new RuntimeException(e);
      }
}

public void commitSession(CommitState state) throws PackageIntegrityException {
      checkSession();
      if (centralPath == null)
	    throw new PackageIntegrityException("Package without binary payload");
      if (state == CommitState.Success) {
	    try {
		  byte[] configBytes = Files.readAllBytes(tempConfig);
		  Path configPath = Path.of(getPackagesConfig());
		  Files.write(configPath, configBytes, StandardOpenOption.APPEND);
		  Path linkPath = Path.of(centralPath.toString(), "bin", "ddl.conf");
		  linkLibraries(linkPath, dependencies);
		  clearSession();
	    } catch (IOException e) {
		  logger.warn("Package linking error: " + centralPath);
		  eraseSession();
		  throw new PackageIntegrityException("Error during package linking");
	    }
      } else {
	    eraseSession();
      }
}

private void clearSession() throws IOException {
      assert tempConfig != null;
      Files.deleteIfExists(tempConfig);
      centralPath = null;
      dependencies = null;
      tempConfig = null;
}

private void eraseSession() {
      try {
	    if (centralPath != null)
		  removeFiles(centralPath);
	    if (dependencies != null) {
		  for (Path path : dependencies) {
			removeFiles(path);
		  }
	    }
	    clearSession();
      } catch (IOException ignored) {
	    logger.warn("Failed to remove broken package");
      }
}

private void saveConfiguration(@NotNull Path installation, @NotNull FullPackageInfoDTO dto) {
      assert dto.aliases != null;
      String[] aliases = new String[dto.aliases.length + 1];
      System.arraycopy(dto.aliases, 0, aliases, 0, dto.aliases.length);
      aliases[dto.aliases.length] = dto.name;
      appendConfig(tempConfig, aliases, installation);

}

/**
 * Save dependency payload path
 */
private void addDependency(@NotNull Path installation, @NotNull FullPackageInfoDTO dto) throws PackageIntegrityException {
      checkSession();
      dependencies.add(installation);
      saveConfiguration(installation, dto);
}

private void addBinary(@NotNull Path packagePath, @NotNull FullPackageInfoDTO dto) throws PackageIntegrityException {
      checkSession();
      centralPath = packagePath;
      saveConfiguration(packagePath, dto);
}

private void checkSession() throws PackageIntegrityException {
      if (dependencies == null || tempConfig == null)
	    throw new PackageIntegrityException("Session is not started");
}

private Path centralPath; //where binary file is storing
private Path tempConfig;
private List<Path> dependencies;


/**
 * Store package in local file system
 * and add package's info to local registry<br>
 * The following methods are fully depends on OS. As rule, the following sample is very primitive and generalized
 */
public void storeLocally(FullPackageInfoDTO info, PackageAssembly assembly) throws PackageIntegrityException {
      PayloadType type = convert(info.payloadType);
      if (type == PayloadType.Unknown)
	    throw new PackageIntegrityException("Unknown package typ");
      try {
	    //the directory where package should be installed -> the method is full safe
	    Path normalized = normalizePath(info.name, type);
	    if (Files.exists(normalized)) { //old version of package should be retired
		  removeFiles(normalized);
		  Files.createDirectory(normalized);
	    }
	    switch (type) {
		  case Binary -> {
			Path binDir = Path.of(normalized.toString(), "bin");
			Path binaryPath = Path.of(normalized.toString(), "bin", info.name + ".exe");
			Files.createDirectory(binDir);
			Files.write(binaryPath, assembly.getPayload());
			Files.setAttribute(binaryPath, "unix:+x", true);
			addBinary(normalized, info);
		  }
		  case Library -> {
			Path libPath = Path.of(normalized.toString(), info.name + ".ddl");
			Files.write(libPath, assembly.getPayload());
			Files.setAttribute(normalized, "unix:+x", true);//or mark as shared library
			addDependency(normalized, info);//save installation to regist
		  }
	    }
	    Path infoPath = Path.of(normalized.toString(), "conf.pum");
	    byte[] bytesInfo = toByteFormat(assembly, info);
	    Files.write(infoPath, bytesInfo);
      } catch (IOException e) {
	    throw new PackageIntegrityException("Impossible to install package");
      }
}

private byte[] toByteFormat(PackageAssembly assembly, FullPackageInfoDTO dto) {
      String result = String.valueOf(assembly.getId()) + assembly.getVersion() +
			  new Gson().toJson(dto);
      return result.getBytes(StandardCharsets.US_ASCII);
}

private @NotNull Path normalizePath(String name, PayloadType type) {
      Path destPath = null;
      switch (type) {
	    case Binary -> {
		  destPath = Path.of(getProgramsPath(), name);
	    }
	    case Library -> {
		  destPath = Path.of(getLibrariesPath(), name);
	    }
	    case Docs, Config, Unknown ->
		throw new IllegalStateException("The client doesn't support such functionality");
      }
      return destPath;
}

private void removeFiles(@NotNull Path filePath) throws IOException {
      File rootFile = filePath.toFile();
      if (!rootFile.exists())
	    return;
      if (rootFile.isDirectory()) {
	    File[] files = rootFile.listFiles();
	    assert files != null;
	    for (File file : files) {
		  if (file.isDirectory()) {
			removeFiles(file.toPath());
		  }
		  Files.delete(file.toPath());
	    }
      }
      Files.delete(filePath);
}

public static void appendConfig(Path configPath, String[] aliases, Path instancePath) {
      try {
	    var info = new InstanceInfo(aliases, instancePath.toAbsolutePath().toString());
	    Files.writeString(configPath, info.toString(), StandardOpenOption.APPEND);
      } catch (IOException e) {
	    throw new RuntimeException("Configuration file is not exists");
      }
}

//OS relative functionality
private static void linkLibraries(@NotNull Path destFile, @NotNull List<Path> dependencies) throws IOException {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(destFile.toFile()))) {
	    for (var path : dependencies) {
		  writer.write(path.toAbsolutePath().toString());
	    }
      }
}

private static PayloadType convert(@NotNull String type) {
      PayloadType result = PayloadType.Unknown;
      try {
	    result = PayloadType.valueOf(type);
      } catch (IllegalArgumentException ignored) {
      }
      return result;
}

private String getProgramsPath() {
      return config.programs;
}

private String getLibrariesPath() {
      return config.libraries;
}

private String getPackagesConfig() {
      return config.infoPath;
}

//only one package
private String getTempPath() {
      return config.temp;
}

}
