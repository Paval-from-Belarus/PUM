package org.petos.packagemanager.client.storage;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.client.Configuration;
import org.petos.packagemanager.client.database.InstanceInfo;
import org.petos.packagemanager.client.database.PackageInfo;
import org.petos.packagemanager.packages.DependencyInfoDTO;
import org.petos.packagemanager.packages.FullPackageInfoDTO;
import org.petos.packagemanager.packages.PackageAssembly;
import org.petos.packagemanager.packages.ShortPackageInfoDTO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static org.petos.packagemanager.client.database.InstanceInfo.*;

public class PackageStorage {
public enum PayloadType {Binary, Library, Docs, Config, Unknown}

public enum RebuildMode {Remove, Replace}

public enum InstallationState {
      /**
       * <code>Installed</code> ― installed and can be removed<br>
       * <code>Updatable</code> ― installed, can be removed and updatable<br>
       * <code>Frozen</code> ― installed but cannot be changed (other packages reference to it)
       * P.S. To make package not <code>Frozen</code> attempt to update other packages or use force deleting.
       * In case of force deleting broken dependencies appears)
       */
      NotInstalled, Cached, Installed, Updatable, Frozen;
      private boolean locality = false;
      private boolean attachment = false; //mark that package cannot be deleted

      static {
	    Installed.locality = true;
	    Updatable.locality = true;
	    Frozen.locality = true;
	    Frozen.attachment = true;
      }

      public boolean isLocal() {
	    return locality;
      }

      public boolean isRemote() {
	    return !locality;
      }

      public boolean isRemovable() {
	    return locality && !attachment;
      }
}

public enum RemovableType {Soft /* only attempt*/, Force}

private static final Logger logger = LogManager.getLogger(PackageStorage.class);

public enum OperationStatus {Failed, Success, No}

public static class PackageIntegrityException extends Exception {
      public PackageIntegrityException(String msg) {
	    super(msg);
      }
}

public static class ConfigurationException extends Exception {
      ConfigurationException(String msg) {
	    super(msg);
      }
}

public PackageStorage(Configuration config) throws IOException {
      lastStatus = OperationStatus.No;
      cachedInfo = null;
      PackageStorage.config = config;
      String content = Files.readString(Path.of(config.infoPath));
      installed =
	  valueOf(content).parallelStream()
	      .collect(Collectors.toMap(InstanceInfo::getId, info -> info));
}


private List<ShortPackageInfoDTO> cachedInfo;
private List<InstanceInfo> brokenPackages; //the list of packages that cannot be resolved
private OperationStatus lastStatus;

public OperationStatus getLastStatus() {
      return lastStatus;
}


public void replaceCacheInfo(ShortPackageInfoDTO[] shortInfo) throws ConfigurationException {
      if (cachedInfo != null) {
	    cachedInfo.clear();
      } else {
	    cachedInfo = new ArrayList<>();
      }
      cachedInfo.addAll(Arrays.asList(shortInfo));
      final Path cachePath = getCachePath();
      String content = toJson(cachedInfo.toArray(ShortPackageInfoDTO[]::new));
      try {
	    Files.writeString(cachePath, content);
      } catch (IOException e) {
	    throw new ConfigurationException("Invalid cache replacing");
      }
}

public void clearCache() throws ConfigurationException {
      if (cachedInfo != null)
	    cachedInfo.clear();
      Path cachePath = Path.of(config.temp).resolve(CACHE_FILE_NAME);
      try {
	    Files.deleteIfExists(cachePath);
      } catch (IOException e) {
	    throw new ConfigurationException("Invalid cache cleaning");
      }
}

//todo: on each getPackageState command update cache?
public InstallationState getPackageState(String name) {
      InstallationState state = InstallationState.NotInstalled;
      var instance = getInstanceInfo(name);
      if (instance.isPresent() && instance.get().similar(name)) {
	    state = getInstanceState(instance.get());
      } else {
	    var cached = getCachedInfo(name);
	    if (cached.isPresent())
		  state = InstallationState.Cached;
      }
      return state;
}

InstallationState getInstanceState(InstanceInfo info) {
      InstallationState state = InstallationState.Installed;
      if (info.getLinksCnt() != 0)
	    state = InstallationState.Frozen;
      return state;
}

public InstallationState getPackageState(@NotNull Integer id, @NotNull String version) {
      InstallationState state = InstallationState.NotInstalled;
      var packageInfo = getPackageInfo(id);
      if (packageInfo.isPresent() && packageInfo.get().equals(id, version)) {
	    var instance = getInstanceInfo(id);
	    if (instance.isPresent()) {
		  state = getInstanceState(instance.get());
	    }
      } else {
	    var cached = getCachedInfo(id, version);
	    if (cached.isPresent())
		  state = InstallationState.Cached;
      }
      return state;
}

public @NotNull <T> T initSession(Class<T> classType) throws ConfigurationException {
      T session = null;
      try {
	    if (classType.equals(RemovableSession.class)) {
		  Path removables = Files.createTempFile(Path.of(config.temp), "removables", ".pum");
		  var remover = new RemovableSession(this);
		  remover.setConfig(removables.toFile());
		  session = classType.cast(remover);
	    }
	    if (classType.equals(InstallerSession.class)) {
		  Path tempConfig = Files.createTempFile(Path.of(config.temp), "config", ".pum");
		  var installer = new InstallerSession(this);
		  installer.setConfigFile(tempConfig.toFile());
		  session = classType.cast(installer);
	    }
      } catch (Exception e) {
	    throw new ConfigurationException("Impossible to create session instance");
      }
      if (session == null)
	    throw new ConfigurationException("Illegal session class");
      return session;
}

public Optional<FullPackageInfoDTO> getFullInfo(Integer id, String version) {
      var localInfo = getPackageInfo(id);
      Optional<FullPackageInfoDTO> fullInfo = Optional.empty();
      if (localInfo.isPresent() && localInfo.get().version.equals(version)) {
	    fullInfo = localInfo.flatMap(this::toExternalFormat);
      }
      return fullInfo;
}

public Optional<FullPackageInfoDTO> getFullInfo(String name) {
      var localInfo = getPackageInfo(name);
      return localInfo.flatMap(this::toExternalFormat);
}

public Optional<FullPackageInfoDTO> completeFullInfo(ShortPackageInfoDTO dto) {
      var info = getPackageInfo(dto.id());
      return info.flatMap(this::toExternalFormat);
}

public static void storePackageInfo(Path configDir, PackageInfo info) throws IOException {
      String jsonInfo = toJson(info);
      Files.writeString(configDir.resolve("conf.pum"), jsonInfo);
}

static PackageInfo toLocalFormat(PackageAssembly assembly, FullPackageInfoDTO dto) {
      var info = new PackageInfo();
      info.packageId = assembly.getId();
      info.versionId = assembly.getVersion();
      info.license = dto.licenseType;
      info.payload = dto.payloadType;
      info.version = dto.version;
      info.dependencies = dto.dependencies;
      return info;
}

//The only one version of each program can be installed locally
private Optional<FullPackageInfoDTO> toExternalFormat(PackageInfo info) {
      FullPackageInfoDTO dto = null;
      var optional = installed.values().stream()
			 .filter(i -> info.packageId.equals(i.getId()))
			 .findAny();
      if (optional.isPresent()) {
	    InstanceInfo instance = optional.get();
	    dto = new FullPackageInfoDTO();
	    dto.payloadType = info.payload;
	    dto.licenseType = info.license;
	    dto.dependencies = info.dependencies;
	    dto.version = info.version;
	    String[] commonNames = instance.getAliases();
	    assert commonNames.length > 0;
	    dto.name = commonNames[0];
	    dto.aliases = new String[commonNames.length - 1];
	    System.arraycopy(commonNames, 1, dto.aliases, 0, dto.aliases.length);
	    Path directory = Path.of(instance.getStringPath());
	    PayloadType payload = convert(info.payload);
	    if (directory.toFile().isDirectory() && payload != PayloadType.Unknown) {
		  Long packageSize;
		  PackageWalker walker = new PackageWalker();
		  if (payload == PayloadType.Binary)
			directory = directory.resolve("bin");
		  try {
			Files.walkFileTree(directory, walker);
			packageSize = walker.getCommonSize();
			dto.payloadSize = packageSize.intValue();
		  } catch (IOException e) { //if so, the size of package equals zero
			logger.warn("The package size is not available: " + e.getMessage());
		  }
	    }
      }
      return Optional.ofNullable(dto);
}

//the following methods only for members of storage package

/**
 * The great assumption of this method... Never use application during its installation<br>
 * Instances are all packages (libraries nor binary) that was installed in last InstallerSession <br>
 * Update the info about installed packages (it's true that these packages are storing in local FS). If instances
 * are already exists, they will be replaced by new values
 */
void rebuildConfig(List<InstanceInfo> instances, RebuildMode mode) throws IOException {
      Path packagesInfo = Path.of(config.infoPath);
      switch (mode) {
	    case Remove -> instances.forEach(info -> installed.remove(info.getId()));
	    case Replace -> instances.forEach(info -> installed.put(info.getId(), info));
      }
      StringBuilder output = new StringBuilder();
      installed.values().forEach(output::append);//invoke to string for each item
      Files.writeString(packagesInfo, output.toString());
}

//each packages also holds info about self in package directory as conf.pum
//At this moment, in central path stores configaration file
//The configuration file has <code>Local format</code>
//The result of thi function is OS relative form of ddl
@NotNull
void linkLibraries(Path central, @NotNull List<InstanceInfo> instances) throws IOException {
      assert installed != null;
      Path configPath = central.resolve("conf.pum");
      Path linkPath = Path.of(central.toString(), "bin", "ddl.conf");
      if (instances.size() != 0) {
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(linkPath.toFile()))) {
		  String jsonInfo = Files.readString(configPath);
		  PackageInfo info = fromJson(jsonInfo, PackageInfo.class);
		  for (DependencyInfoDTO depLink : info.dependencies) {
			InstanceInfo depInfo = installed.get(depLink.packageId());
			if (depInfo != null && !instances.contains(depInfo)) {
			      instances.add(depInfo);
			}
		  }
		  for (var dependency : instances) {
			writer.write(dependency.getStringPath() + "\r\n");
			dependency.updateLinksCnt(LinkState.Add);
		  }
		  rebuildConfig(instances, RebuildMode.Replace);
	    }
      }//also store linkPath in executable
}

@NotNull
Path normalizePath(String name, PayloadType type) {
      Path destPath = null;
      switch (type) {
	    case Binary -> {
		  destPath = Path.of(config.packages).resolve(name);
	    }
	    case Library -> {
		  destPath = Path.of(config.libraries).resolve(name);
	    }
	    case Docs, Config, Unknown ->
		throw new IllegalStateException("The client doesn't support such functionality");
      }
      return destPath;
}


//The following methods are candidates to be written on database connection
public @NotNull List<FullPackageInfoDTO> localPackages() {
      List<FullPackageInfoDTO> list = new ArrayList<>();
      OperationStatus status = OperationStatus.Success;
      for (var entry : installed.entrySet()) {
	    Optional<FullPackageInfoDTO> dto = getPackageInfo(entry.getValue())
						   .flatMap(this::toExternalFormat);
	    if (dto.isPresent()) {
		  list.add(dto.get());
	    } else {
		  status = OperationStatus.Failed;
		  logger.warn("Some packages are not accessible: " + Arrays.toString(entry.getValue().getAliases()));
	    }
      }
      setLastStatus(status);
      return list;
}

private @NotNull List<ShortPackageInfoDTO> getCachedList() {
      final Path cachePath = getCachePath();
      List<ShortPackageInfoDTO> cachedInfo;
      if (this.cachedInfo != null) {
	    cachedInfo = this.cachedInfo;//already was invoked to memory)
      } else {
	    cachedInfo = new ArrayList<>();
	    try {
		  String content = Files.readString(cachePath);
		  ShortPackageInfoDTO[] items = fromJson(content, ShortPackageInfoDTO[].class);
		  cachedInfo.addAll(Arrays.asList(items));
		  this.cachedInfo = cachedInfo;
	    } catch (IOException | JsonSyntaxException ignored) {
	    }
      }
      return cachedInfo;
}

public Optional<ShortPackageInfoDTO> getCachedInfo(@NotNull String name) {
      var list = getCachedList();
      return list.stream()
		 .filter(dto -> dto.similar(name))
		 .findAny();
}

public Optional<ShortPackageInfoDTO> getCachedInfo(@NotNull Integer id, @NotNull String version) {
      var list = getCachedList();
      return list.stream()
		 .filter(dto -> dto.id().equals(id) && dto.version().equals(version))
		 .findAny();
}

Optional<PackageInfo> getPackageInfo(@NotNull String name) {
      Optional<InstanceInfo> instance = getInstanceInfo(name);
      return instance.flatMap(this::getPackageInfo);
}

Optional<PackageInfo> getPackageInfo(@NotNull Integer id) {
      var instance = getInstanceInfo(id);
      return instance.flatMap(this::getPackageInfo);
}

Optional<InstanceInfo> getInstanceInfo(@NotNull String name) {
      return installed.values().stream()
		 .filter(info -> info.similar(name))
		 .findAny();
}

Optional<InstanceInfo> getInstanceInfo(@NotNull Integer id) {
      return Optional.ofNullable(installed.get(id));
}

private Optional<PackageInfo> getPackageInfo(InstanceInfo instance) {
      PackageInfo info = null;
      try {
	    Path centralPath = Path.of(instance.getStringPath()).resolve("conf.pum");
	    String jsonInfo = Files.readString(centralPath);
	    info = fromJson(jsonInfo, PackageInfo.class);
      } catch (IOException ignored) {
	    logger.warn("No installed package");
      }
      return Optional.ofNullable(info);
}

public static <T> T fromJson(String source, Class<T> classType) {
      return gson.fromJson(source, classType);
}

public static <T> String toJson(T source) {
      return gson.toJson(source);
}

//return the size of all files in walked directory
private static class PackageWalker extends SimpleFileVisitor<Path> {
      long payloadSize;

      public Long getCommonSize() {
	    return payloadSize;
      }

      @Override
      public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
	    payloadSize += filePath.toFile().length();
	    return FileVisitResult.CONTINUE;
      }
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

private void setLastStatus(OperationStatus status) {
      lastStatus = status;
}

static void removeFiles(@NotNull Path filePath) throws IOException {
      Files.walkFileTree(filePath, new PackageRemover());
}

public static PayloadType convert(@NotNull String type) {
      PayloadType result = PayloadType.Unknown;
      try {
	    result = PayloadType.valueOf(type);
      } catch (IllegalArgumentException ignored) {
      }
      return result;
}

private Path getCachePath() {
      return Path.of(config.temp).resolve(CACHE_FILE_NAME);
}

private static Configuration config;
private Map<Integer, InstanceInfo> installed;
private static final Gson gson = new Gson();
private final String CACHE_FILE_NAME = "pum.cache";
}