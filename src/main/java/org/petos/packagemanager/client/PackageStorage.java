package org.petos.packagemanager.client;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
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

public class PackageStorage {
private final String CACHE_FILE_NAME = "pum.cache";

public enum PayloadType {Binary, Library, Docs, Config, Unknown}

public enum InstallationState {NotInstalled, Cached, Installed, Updatable}

private static final Logger logger = LogManager.getLogger(PackageStorage.class);

public enum OperationStatus {Failed, Success, No}

public static class PackageIntegrityException extends Exception {
      PackageIntegrityException(String msg) {
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
      PackageStorage.installed =
	  InstanceInfo.valueOf(content).parallelStream()
	      .collect(Collectors.toMap(InstanceInfo::getId, info -> info));
}

private @NotNull List<ShortPackageInfoDTO> getCachedInfo() {
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

private List<ShortPackageInfoDTO> cachedInfo;
private List<InstanceInfo> brokenPackages; //the list of packages that cannot be resolved
private OperationStatus lastStatus;

public OperationStatus getLastStatus() {
      return lastStatus;
}

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

public Optional<ShortPackageInfoDTO> getCached(String name) {
      var cachedList = getCachedInfo();
      return cachedList.stream()
		 .filter(dto -> dto.similar(name))
		 .findAny();
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
      assert installed != null;
      InstallationState state = InstallationState.NotInstalled;
      boolean isFound = installed.values().stream()
			    .anyMatch(instance -> instance.similar(name));
      if (!isFound) {
	    List<ShortPackageInfoDTO> cachedInfo = getCachedInfo();
	    isFound = cachedInfo.stream()
			  .anyMatch(dto -> dto.similar(name));
	    if (isFound)
		  state = InstallationState.Cached;
      } else {

	    state = InstallationState.Installed;
      }
      return state;
}

public InstallationState getPackageState(@NotNull Integer id, @NotNull String version) {
      InstallationState state = InstallationState.NotInstalled;
      var packageInfo = getPackageInfo(id);
      Optional<Object> equality =
	  packageInfo.map(info -> id.equals(info.packageId) && version.equals(info.version));
      if (equality.isEmpty()) {
	    var cachedInfo = getCachedInfo();
	    boolean isCached = cachedInfo.stream()
				   .anyMatch(dto -> dto.id().equals(id) && dto.version().equals(version));
	    if (isCached)
		  state = InstallationState.Cached;
      } else {
	    state = InstallationState.Installed;
      }
      return state;
}

public void removePackage(String name) {

}


public static void storePackageInfo(Path configDir, PackageInfo info) throws IOException {
      String jsonInfo = toJson(info);
      Files.writeString(configDir.resolve("conf.pum"), jsonInfo);
}

public static PackageInfo toLocalFormat(PackageAssembly assembly, FullPackageInfoDTO dto) {
      var info = new PackageInfo();
      info.packageId = assembly.getId();
      info.versionId = assembly.getVersion();
      info.license = dto.licenseType;
      info.payload = dto.payloadType;
      info.version = dto.version;
      info.dependencies = dto.dependencies;
      return info;
}
public Optional<FullPackageInfoDTO> completeFullInfo(ShortPackageInfoDTO dto){
      var info = getPackageInfo(dto.id());
      return info.flatMap(this::toExternalFormat);
}
//The only one version of each program can be installed locally
private Optional<FullPackageInfoDTO> toExternalFormat(PackageInfo info) {
      assert installed != null;
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

//The great assumption of this method... Never use application during its installation
public static void rebuildConfig(List<InstanceInfo> instances) throws IOException {
      assert config != null && installed != null;
      Path packagesInfo = Path.of(config.infoPath);
      instances.forEach(i -> installed.put(i.getId(), i));
      StringBuilder output = new StringBuilder();
      for (var instance : installed.values()) {
	    output.append(instance);
      }
      Files.writeString(packagesInfo, output.toString());
}


//At this moment, in central path stores configaration file
//The configuration file has <code>Local format</code>
//The result of thi function is OS relative form of ddl
public static @NotNull Path linkLibraries(Path central, @NotNull List<InstanceInfo> instances) throws IOException {
      assert installed != null;
      Path configPath = central.resolve("conf.pum");
      Path linkPath = Path.of(central.toString(), "bin", "ddl.conf");
      if (instances.size() != 0) {
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(linkPath.toFile()))) {
		  String jsonInfo = Files.readString(configPath);
		  PackageInfo info = fromJson(jsonInfo, PackageInfo.class);
		  for (DependencyInfoDTO depLink : info.dependencies) {
			InstanceInfo depInfo = installed.get(depLink.packageId());
			if (depInfo != null && !instances.contains(depInfo))
			      instances.add(depInfo);
		  }

		  for (var path : instances) {
			writer.write(path.getStringPath() + "\r\n");
		  }
	    }
      }//also store linkPath in executable
      return linkPath;
}
private Optional<PackageInfo> getPackageInfo(@NotNull Integer id){
      assert installed != null;
      var instance = installed.get(id);
      return getPackageInfo(instance);
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

private Optional<PackageInfo> getLocalInfo(InstanceInfo instance) {
      return Optional.empty();
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

private void setLastStatus(OperationStatus status) {
      lastStatus = status;
}

private static PayloadType convert(@NotNull String type) {
      PayloadType result = PayloadType.Unknown;
      try {
	    result = PayloadType.valueOf(type);
      } catch (IllegalArgumentException ignored) {
      }
      return result;
}

private final Path getCachePath() {
      return Path.of(config.temp).resolve(CACHE_FILE_NAME);
}

private static Configuration config;
private static Map<Integer, InstanceInfo> installed;
private static Gson gson = new Gson();
}
