package org.petos.packagemanager.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.client.database.InstanceInfo;
import org.petos.packagemanager.client.database.PackageInfo;
import org.petos.packagemanager.packages.FullPackageInfoDTO;
import org.petos.packagemanager.packages.PackageAssembly;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;

import static org.petos.packagemanager.client.PackageStorage.PackageIntegrityException;

/**
 * The current issue: if something go wrong, all package (even existing version) will be removed<br>
 * todo: Replace the logic of current class to Client's PackageStorage class
 */
public class PackageInstaller {
private static final Logger logger = LogManager.getLogger(PackageInstaller.class);

public enum PayloadType {Binary, Library, Docs, Config, Unknown}

public enum CommitState {Failed, Success}

public class InstallerSession implements AutoCloseable{
      private Path centralPath; //central file for which each dependency belongs
      private File configFile;
      private File exeFile;//the executable file
      private boolean isManaged;

      /**
       * Store package in local file system
       * and add package's info to local registry<br>
       * The following methods are fully depends on OS. As rule, the following sample is very primitive and generalized
       */
      public void storeLocally(FullPackageInfoDTO dto, PackageAssembly assembly) throws PackageIntegrityException {
	    checkSession();
	    PayloadType type = convert(dto.payloadType);
	    if (type == PayloadType.Unknown)
		  throw new PackageIntegrityException("Unknown package type");
	    try {
		  //the directory where package should be installed -> the method is full safe
		  Path normalized = normalizePath(dto.name, type); //sileietly remove existing dir
		  if (Files.exists(normalized)) { //old version of package should be retired
			removeFiles(normalized);
		  }
		  Files.createDirectory(normalized);
		  switch (type) {
			case Binary -> {
			      saveBinary(normalized, assembly, dto);
			}
			case Library -> {
			      saveLibrary(normalized, assembly, dto);
			}
		  }
		  PackageInfo info = PackageStorage.toLocalFormat(assembly, dto);
		  PackageStorage.storePackageInfo(normalized, info);
	    } catch (IOException e) {
		  throw new PackageIntegrityException("Payload installation error: " + e.getMessage());
	    }
      }

      //Each session should be commit
      public void commit(CommitState state) throws PackageIntegrityException {
	    if (centralPath == null) //remove in prod
		  throw new PackageIntegrityException("Package without binary payload");
	    checkSession();
	    if (state == CommitState.Success) {
		  try {
			String config = Files.readString(configFile.toPath());
			List<InstanceInfo> dependencies = InstanceInfo.valueOf(config);
			PackageStorage.rebuildConfig(dependencies);
			var libPath = PackageStorage.linkLibraries(centralPath, dependencies);
			linkLibraries(exeFile, libPath);
			//storage relink storage
			//it's known info that some packages are already installed
			clearSession();
		  } catch (IOException e) {
			logger.warn("Package linking error: " + e.getMessage());
			eraseSession();
			throw new PackageIntegrityException("Package linking error" + e.getMessage());
		  }
	    } else {
		  eraseSession();
	    }
	    isManaged = true;
      }

      private void checkSession() throws PackageIntegrityException {
	    if (configFile == null)
		  throw new PackageIntegrityException("Session is not started");
      }

      /**
       * Save dependency payload path
       */
      private void saveLibrary(@NotNull Path normalized, @NotNull PackageAssembly assembly, @NotNull FullPackageInfoDTO dto) throws PackageIntegrityException {
	    checkSession();
	    try {
		  Path libPath = Path.of(normalized.toString(), dto.name + ".ddl");
		  Files.write(libPath, assembly.getPayload());
		  Files.setPosixFilePermissions(libPath, EnumSet.of(PosixFilePermission.OWNER_EXECUTE)); //or mark as shared library
		  appendConfig(assembly.getId(), normalized, dto);
	    } catch (IOException e) {
		  throw new PackageIntegrityException("Library storage error");
	    }
      }

      private void saveBinary(@NotNull Path normalized, @NotNull PackageAssembly assembly, @NotNull FullPackageInfoDTO dto) throws PackageIntegrityException {
	    checkSession();
	    Path binDir = Path.of(normalized.toString(), "bin");
	    Path binaryPath = Path.of(normalized.toString(), "bin", dto.name + ".exe");
	    try {
		  Files.createDirectory(binDir);
		  Files.write(binaryPath, assembly.getPayload());
		  Files.setPosixFilePermissions(binaryPath, EnumSet.of(PosixFilePermission.OWNER_EXECUTE));
		  appendConfig(assembly.getId(), normalized, dto);
		  //if all is cool, set current instance of session
		  centralPath = normalized;
		  exeFile = centralPath.resolve(Path.of("bin", dto.name + ".exe")).toFile();
	    } catch (IOException e) {
		  throw new PackageIntegrityException("Binary file storage error");
	    }
      }

      private void clearSession() throws IOException {
	    Files.deleteIfExists(configFile.toPath());
      }

      private void eraseSession() {
	    try {
		  assert configFile != null;
		  if (centralPath != null)
			removeFiles(centralPath);
		  String info = Files.readString(configFile.toPath());
		  List<InstanceInfo> instances = InstanceInfo.valueOf(info);
		  for (var path : instances) {
			removeFiles(Path.of(path.getStringPath()));
		  }
		  clearSession();
	    } catch (IOException ignored) {
		  logger.warn("Failed to remove broken package: " + ignored.getMessage());
	    }
      }

      private void appendConfig(Integer packageId, Path instancePath, FullPackageInfoDTO dto) {
	    String[] aliases = new String[dto.aliases.length + 1];
	    aliases[0] = dto.name;
	    System.arraycopy(dto.aliases, 0, aliases, 1, dto.aliases.length);
	    try {
		  var info = new InstanceInfo(packageId, aliases, instancePath.toAbsolutePath().toString());
		  Files.writeString(configFile.toPath(), info.toString(), StandardOpenOption.APPEND);
	    } catch (IOException e) {
		  throw new RuntimeException("Configuration file is not exists");
	    }
      }

      private InstallerSession() {
      		isManaged = false;
      }

      private InstallerSession setConfigFile(File file) {
	    this.configFile = file;
	    return this;
      }
      //the close method should be invoked if all is wrong
      @Override
      public void close() throws PackageIntegrityException {
	    if (!isManaged)
		  commit(CommitState.Failed);//if Session was not closed even wrong ― wrong case)
      }
}

private final Configuration config;

public PackageInstaller(Configuration config) {
      this.config = config;
}

public InstallerSession initSession() {
      InstallerSession session = new InstallerSession();
      try {
	    Path tempConfig = Files.createTempFile(Path.of(getTempDirectory()), "config", ".pum");//
	    session.setConfigFile(tempConfig.toFile());
      } catch (IOException e) {
	    //really interesting case...
	    throw new RuntimeException(e);
      }
      return session;
}

//Utilities methods depends on the instance of Parent class

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
private static void linkLibraries(@NotNull File executable, @NotNull Path linkPath) throws PackageIntegrityException {

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
private String getTempDirectory() {
      return config.temp;
}

}
