package org.petos.packagemanager.client.storage;

import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.client.StorageSession;
import org.petos.packagemanager.client.database.InstanceInfo;
import org.petos.packagemanager.client.database.PackageInfo;
import org.petos.packagemanager.packages.FullPackageInfoDTO;
import org.petos.packagemanager.packages.PackageAssembly;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.petos.packagemanager.client.storage.PackageStorage.*;

public class InstallerSession implements StorageSession {
InstallerSession(PackageStorage storage) {
      this.storage = storage;
      isManaged = false;
}

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
	    Path normalized = storage.normalizePath(dto.name, type); //sileietly remove existing dir
	    if (Files.exists(normalized)) { //old version of package should be retired
		  PackageStorage.removeFiles(normalized);
	    }
	    Files.createDirectory(normalized);
	    PackageStorage.storePackageInfo(normalized, dto, assembly);
	    switch (type) {
		  case Binary -> {
			saveBinary(normalized, assembly, dto);
		  }
		  case Library -> {
			saveLibrary(normalized, assembly, dto);
		  }
	    }
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
		  storage.rebuildConfig(dependencies, RebuildMode.Replace);
		  dependencies = dependencies.stream()
				     .filter(d -> !d.getStringPath().equals(centralPath.toString()))
				     .collect(Collectors.toList());
		  storage.linkLibraries(centralPath, dependencies);
		  //storage relink storage
		  //it's known info that some packages are already installed
		  clearSession();
	    } catch (IOException e) {
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
	    List<InstanceInfo> libDependencies = mapDependencies(dto);
	    storage.linkLibraries(normalized, libDependencies);
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

//convert existing dependencies from PackageInfo to InstanceInfo
private @NotNull List<InstanceInfo> mapDependencies(FullPackageInfoDTO dto) throws PackageIntegrityException {
      List<InstanceInfo> instances = new ArrayList<>(dto.dependencies.length);
      for (var dependency : dto.dependencies) {
	    Optional<InstanceInfo> instance = storage.getInstanceInfo(dependency.packageId(), dependency.label());
	    instance.map(instances::add)
		.orElseThrow(() -> new PackageIntegrityException("Library dependency is broken"));
      }
      return instances;
}

private Path centralPath; //central file for which each dependency belongs
private File configFile;
private File exeFile;//the executable file
private boolean isManaged;
private final PackageStorage storage;

InstallerSession setConfigFile(File file) {
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
