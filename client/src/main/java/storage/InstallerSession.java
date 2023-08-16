package storage;

import database.InstanceInfo;
import org.jetbrains.annotations.NotNull;
import dto.FullPackageInfoDTO;
import transfer.PackageAssembly;
import security.Encryptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.*;
import java.util.ArrayList;
import java.util.EnumSet;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static storage.JournalTransaction.*;
import static storage.PackageStorage.*;
public class InstallerSession extends AbstractSession {
InstallerSession(PackageStorage storage) {
      this.storage = storage;
      this.encryption = Encryptor.Encryption.None;
      setManaged(false);
}

public Encryptor.Encryption getEncryption() {
      getForeignKey().ifPresent(encryption::detachKey);
      return this.encryption;
}

public void setEncryption(@NotNull Encryptor.Encryption type) {
      this.encryption = type;
      if (type.isAsymmetric()) {
	    var pair = Encryptor.generatePair(type);
	    localKey = pair.getPrivate();
	    foreignKey = pair.getPublic();
      }
      if (type.isSymmetric()) {
	    var secret = Encryptor.generateSecret(type);
	    localKey = secret;
	    foreignKey = secret; //the stupid but fast
      }
}

/**
 * Store package in local file system
 * and add package's info to local registry<br>
 * The following methods are fully depends on OS. As rule, the following sample is very primitive and generalized
 */
public void storeLocally(FullPackageInfoDTO dto, byte[] rawAssembly) throws PackageIntegrityException, PackageAssembly.VerificationException {
      assert originUrl != null;
      getLocalKey().ifPresent(encryption::detachKey);
      PackageAssembly assembly = PackageAssembly.deserialize(rawAssembly, encryption);
      PayloadType type = convert(dto.payloadType);
      if (type == PayloadType.Unknown)
	    throw new PackageIntegrityException("Unknown package type");
      try {
	    //the directory where package should be installed -> the method is full safe
	    Path normalized = storage.normalizePath(dto.name, type); //silently remove existing dir
	    if (Files.exists(normalized)) { //old version of package should be retired
		  removeFiles(normalized);
	    }
	    Files.createDirectory(normalized);
	    storePackageInfo(normalized, dto, assembly, originUrl);
	    switch (type) {
		  case Application -> saveBinary(normalized, assembly, dto);
		  case Library -> saveLibrary(normalized, assembly, dto);
	    }
      } catch (IOException e) {
	    throw new PackageIntegrityException("Payload installation error: " + e.getMessage());
      }
}
//Each session should be committed
public void commit(CommitState state) throws PackageIntegrityException {
      if (isManaged())
	    return;
      if (state == CommitState.Success) {
	    try {
		  List<InstanceInfo> dependencies = getTransactions(Type.Install).stream()
							.map(JournalTransaction::getInstance)
							.collect(Collectors.toList());
		  storage.rebuildConfig(dependencies, RebuildMode.Replace);
		  dependencies = dependencies.stream()
				     .filter(d -> !d.getStringPath().equals(centralPath.toString()))
				     .collect(Collectors.toList());
		  storage.linkLibraries(centralPath, dependencies);
		  //storage relink storage
		  //it's known info that some packages are already installed
		  deleteJournal();
	    } catch (IOException e) {
		  eraseSession();
		  throw new PackageIntegrityException("Package linking error" + e.getMessage());
	    }
      } else {
	    eraseSession();
      }
      setManaged(true);
}

/**
 * Save dependency payload path
 */
private void saveLibrary(@NotNull Path normalized, @NotNull PackageAssembly assembly, @NotNull FullPackageInfoDTO dto) throws PackageIntegrityException {
      try {
	    Path libPath = Path.of(normalized.toString(), dto.name + ".ddl");
	    Files.write(libPath, assembly.getPayload());
	    Files.setPosixFilePermissions(libPath, EnumSet.of(PosixFilePermission.OWNER_EXECUTE)); //or mark as shared library
	    appendJournal(assembly.getId(), normalized, dto);
	    List<InstanceInfo> libDependencies = mapDependencies(dto);
	    storage.linkLibraries(normalized, libDependencies);
      } catch (IOException e) {
	    throw new PackageIntegrityException("Library storage error");
      }
}

private void saveBinary(@NotNull Path normalized, @NotNull PackageAssembly assembly, @NotNull FullPackageInfoDTO dto) throws PackageIntegrityException {
      Path binDir = Path.of(normalized.toString(), "bin");
      Path binaryPath = Path.of(normalized.toString(), "bin", dto.name + ".exe");
      try {
	    Files.createDirectory(binDir);
	    Files.write(binaryPath, assembly.getPayload());
	    Files.setPosixFilePermissions(binaryPath, EnumSet.of(PosixFilePermission.OWNER_EXECUTE));
	    appendJournal(assembly.getId(), normalized, dto);
	    //if all is cool, set current instance of session
	    centralPath = normalized;
	    exeFile = centralPath.resolve(Path.of("bin", dto.name + ".exe")).toFile();
      } catch (IOException e) {
	    throw new PackageIntegrityException("Binary file storage error");
      }
}

private void eraseSession() {
      try {
	    if (centralPath != null)
		  removeFiles(centralPath);
	    List<JournalTransaction> transactions = getTransactions();
	    for (var op : transactions) {
		  removeFiles(Path.of(op.getStringPath()));
	    }
	    deleteJournal();
      } catch (IOException ignored) {
      }
}

protected void appendJournal(Integer packageId, Path instancePath, FullPackageInfoDTO dto) {
      String[] aliases = new String[dto.aliases.length + 1];
      aliases[0] = dto.name;
      System.arraycopy(dto.aliases, 0, aliases, 1, dto.aliases.length);
      try {
	    var info = new InstanceInfo(packageId, aliases, instancePath.toAbsolutePath().toString());
	    appendJournal(Type.Install, info);
      } catch (IOException e) {
	    throw new RuntimeException("common.Configuration file is not exists");
      }
}

//convert existing dependencies from PackageInfo to InstanceInfo
private @NotNull List<InstanceInfo> mapDependencies(FullPackageInfoDTO dto) throws PackageIntegrityException {
      List<InstanceInfo> instances = new ArrayList<>(dto.dependencies.length);
      for (var dependency : dto.dependencies) {
	    Optional<InstanceInfo> instance = storage.getInstanceInfo(dependency.getPackageId(), dependency.getLabel());
	    instance.map(instances::add)
		.orElseThrow(() -> new PackageIntegrityException("Library dependency is broken"));
      }
      return instances;
}
public void assignSource(String originUrl) {
      this.originUrl = originUrl;
}
protected Optional<Key> getForeignKey() {
      return Optional.ofNullable(foreignKey);
}

protected Optional<Key> getLocalKey() {
      return Optional.ofNullable(localKey);
}
private String originUrl = null;
private Path centralPath; //central file for which each dependency belongs
private File exeFile;//the executable file
private final PackageStorage storage;
private Encryptor.Encryption encryption;
private Key localKey;
private Key foreignKey;

//the close method should be invoked if all is wrong
}
