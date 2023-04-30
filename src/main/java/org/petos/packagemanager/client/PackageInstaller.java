package org.petos.packagemanager.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.packages.FullPackageInfoDTO;
import org.petos.packagemanager.packages.PackageAssembly;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
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

private final Configuration config;

public PackageInstaller(Configuration config) {
      this.config = config;
}

public void initSession() {
      centralPath = null;
      dependencies = new ArrayList<>();
      try {
	    tempConfig = Path.of(getTempPath(), "config.pum");
	    Files.deleteIfExists(tempConfig);
	    Files.createFile(tempConfig);
      } catch (IOException e) {
	    //really interesting case...
	    throw new RuntimeException(e);
      }
}

/**
 * Store package in local file system
 * and add package's info to local registry<br>
 * The following methods are fully depends on OS. As rule, the following sample is very primitive and generalized
 */
public void storeLocally(FullPackageInfoDTO dto, PackageAssembly assembly) throws PackageIntegrityException {
      PayloadType type = convert(dto.payloadType);
      if (type == PayloadType.Unknown)
	    throw new PackageIntegrityException("Unknown package typ");
      try {
	    //the directory where package should be installed -> the method is full safe
	    Path normalized = normalizePath(dto.name, type);
	    if (Files.exists(normalized)) { //old version of package should be retired
		  removeFiles(normalized);
	    }
	    Files.createDirectory(normalized);
	    switch (type) {
		  case Binary -> {
			Path binDir = Path.of(normalized.toString(), "bin");
			Path binaryPath = Path.of(normalized.toString(), "bin", dto.name + ".exe");
			Files.createDirectory(binDir);
			Files.write(binaryPath, assembly.getPayload());
			Files.setPosixFilePermissions(binaryPath, EnumSet.of(PosixFilePermission.OWNER_EXECUTE));
			addBinary(normalized, assembly.getId(), dto);
		  }
		  case Library -> {
			Path libPath = Path.of(normalized.toString(), dto.name + ".ddl");
			Files.write(libPath, assembly.getPayload());
			Files.setPosixFilePermissions(libPath, EnumSet.of(PosixFilePermission.OWNER_EXECUTE)); //or mark as shared library
			addLibrary(normalized, assembly.getId(), dto);//save installation to regist
		  }
	    }
	    PackageInfo info = PackageStorage.toLocalFormat(assembly, dto);
	    PackageStorage.storePackageInfo(normalized, info);
      } catch (IOException e) {
	    throw new PackageIntegrityException("Payload installation error: " + e.getMessage());
      }
}
public void commitSession(CommitState state) throws PackageIntegrityException {
      checkSession();
      if (centralPath == null)
	    throw new PackageIntegrityException("Package without binary payload");
      if (state == CommitState.Success) {
	    try {
		  String config = Files.readString(tempConfig);
		  PackageStorage.rebuildConfig(InstanceInfo.valueOf(config));
		  Path linkPath = Path.of(centralPath.toString(), "bin", "ddl.conf");
		  linkLibraries(linkPath, exeFile, dependencies);
		  clearSession();
	    } catch (IOException e) {
		  logger.warn("Package linking error: " + e.getMessage());
		  eraseSession();
		  throw new PackageIntegrityException("Package linking error" + e.getMessage());
	    }
      } else {
	    eraseSession();
      }
}

private void clearSession() throws IOException {
      if (tempConfig != null)
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
	    logger.warn("Failed to remove broken package: " + ignored.getMessage());
      }
}

private void saveConfiguration(@NotNull Path installation, @NotNull Integer packageId, @NotNull FullPackageInfoDTO dto) {
      assert dto.aliases != null;
      String[] aliases = new String[dto.aliases.length + 1];
      System.arraycopy(dto.aliases, 0, aliases, 0, dto.aliases.length);
      aliases[dto.aliases.length] = dto.name;
      appendTempConfig(packageId, aliases, installation);
}
private void appendTempConfig(Integer packageId, String[] aliases, Path instancePath) {
      try {
	    var info = new InstanceInfo(packageId, aliases, instancePath.toAbsolutePath().toString());
	    Files.writeString(tempConfig, info.toString(), StandardOpenOption.APPEND);
      } catch (IOException e) {
	    throw new RuntimeException("Configuration file is not exists");
      }
}

/**
 * Save dependency payload path
 */
private void addLibrary(@NotNull Path installation, @NotNull Integer id, @NotNull FullPackageInfoDTO dto) throws PackageIntegrityException {
      checkSession();
      dependencies.add(installation);
      saveConfiguration(installation, id, dto);
}

private void addBinary(@NotNull Path packagePath,@NotNull Integer id, @NotNull FullPackageInfoDTO dto) throws PackageIntegrityException {
      checkSession();
      centralPath = packagePath;
      exeFile = centralPath.resolve(Path.of("bin", dto.name + ".exe")).toFile();
      saveConfiguration(packagePath, id, dto);
}

private void checkSession() throws PackageIntegrityException {
      if (dependencies == null || tempConfig == null)
	    throw new PackageIntegrityException("Session is not started");
}
private File exeFile;
private Path centralPath; //where binary file is storing
private Path tempConfig;
private List<Path> dependencies;

private @NotNull Path normalizePath(String name, PayloadType type) {
      Path destPath = null;
      switch (type) {
	    case Binary -> {
		  destPath = getProgramsPath().resolve(name);
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
      Files.walkFileTree(filePath, new PackageRemover());
}

private static class PackageRemover extends SimpleFileVisitor<Path> {
      @Override
      public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
	    Files.delete(filePath);
	    return FileVisitResult.CONTINUE;
      }
      @Override
      public FileVisitResult postVisitDirectory(Path filePath, IOException exc) throws IOException {
	    Files.delete(filePath);
	    return FileVisitResult.CONTINUE;
      }

}


//OS relative functionality
private static void linkLibraries(@NotNull Path linkPath,@NotNull File executable, @NotNull List<Path> dependencies) throws IOException {
      if (dependencies.size() != 0){
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(linkPath.toFile()))) {
		  for (var path : dependencies) {
			writer.write(path.toAbsolutePath() + "\r\n");
		  }
	    }
      }//also store linkPath in executable
}

private static PayloadType convert(@NotNull String type) {
      PayloadType result = PayloadType.Unknown;
      try {
	    result = PayloadType.valueOf(type);
      } catch (IllegalArgumentException ignored) {
      }
      return result;
}

private Path getProgramsPath() {
      return Path.of(config.programs);
}

private String getLibrariesPath() {
      return config.libraries;
}

private String getPackagesConfigPath() {
      return config.infoPath;
}

//only one package
private String getTempPath() {
      return config.temp;
}

}
