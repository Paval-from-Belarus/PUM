package storage;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import common.Configuration;
import database.CachedInfo;
import database.InstanceInfo;
import database.PackageInfo;
import database.RepositoryInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import dto.*;
import security.Author;
import transfer.PackageAssembly;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;


public class PackageStorage {
public enum PayloadType {Application, Library, Docs, Config, Unknown}

public enum LicenceType {MIT, GNU, Apache, Bear}

public enum RebuildMode {
      Remove, Replace, SilentReplace;
      private boolean silent = false;

      static {
	    SilentReplace.silent = true;
      }

      public boolean isSilent() {
	    return silent;
      }
}

public enum InstallationState {
      /**
       * <code>Installed</code> ― installed and can be removed<br>
       * <code>Updatable</code> ― installed, can be removed and updatable<br>
       * <code>Frozen</code> ― installed but cannot be changed (other packages reference to it)
       * P.S. To make package not <code>Frozen</code> attempt to update other packages or use force deleting.
       * In case of force deleting broken dependencies appears)
       */
      NotInstalled, Cached, Installed, Updatable, Frozen, MultiVersion;
      private boolean locality = false;
      private boolean attachment = false; //mark that package cannot be deleted

      static {
	    Installed.locality = true;
	    Updatable.locality = true;
	    Frozen.locality = true;
	    Frozen.attachment = true;
	    MultiVersion.locality = true;
	    MultiVersion.attachment = true;
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

      public PackageIntegrityException(Throwable t) {
	    super(t);
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
	  InstanceInfo.valueOf(content).parallelStream()
	      .collect(Collectors.toMap(InstanceInfo::getId, info -> info));
      cachedRepositories = new HashMap<>();
      initRepositories();
      initPublishers();

}

private void initRepositories() throws IOException {
      String content = Files.readString(Path.of(config.repositories));
      RepositoryInfo[] repositories = fromJson(content, RepositoryInfo[].class);
      if (repositories == null)
	    repositories = new RepositoryInfo[0];
      this.repositories = new ArrayList<>();
      this.repositories.addAll(Arrays.asList(repositories));
}

private void initPublishers() throws IOException {
      localPublishers = new HashMap<>();
      String content = Files.readString(Path.of(config.publishers));
      String[] publishers = content.split("\n");
      for (String piece : publishers) {
	    var optional = Author.valueOf(piece);
	    optional.ifPresent(author -> localPublishers.put(author.name(), author));
      }
      if (publishers.length != localPublishers.size()) {
	    StringBuilder strText = new StringBuilder();
	    localPublishers.values().forEach(author -> strText.append(author.stringify()));
	    Files.writeString(Path.of(config.publishers), strText.toString());
      }
}

private List<CachedInfo> cachedInfo;
private List<InstanceInfo> brokenPackages; //the list of packages that cannot be resolved
private OperationStatus lastStatus;

public OperationStatus getLastStatus() {
      return lastStatus;
}

public Optional<Integer> mapPackage(String name) {
      Optional<InstanceInfo> instance = getInstanceInfo(name);
      return instance.map(InstanceInfo::getId);
}

/**
 * @param id ― local Package name
 */
public Optional<RepositoryInfo> getRepository(Integer id) {
      assert repositories != null && cachedRepositories != null;
      Optional<RepositoryInfo> repoInfo = Optional.ofNullable(cachedRepositories.get(id));
      if (repoInfo.isEmpty()) {
	    Optional<String> repoUrl = getPackageInfo(id).map(p -> p.repoUrl);
	    if (repoUrl.isPresent()) {
		  String url = repoUrl.get();
		  repoInfo = repositories.stream()
				 .filter(RepositoryInfo::isEnabled)
				 .filter(info -> info.getBaseUrl().equalsIgnoreCase(url))
				 .findAny();
	    }
	    repoInfo.ifPresent(repositoryInfo -> cachedRepositories.put(id, repositoryInfo));
      }
      return repoInfo;
}

public Optional<RepositoryInfo> getRepository(String repoName) {
      assert repositories != null;
      return repositories.stream()
		 .filter(RepositoryInfo::isEnabled)
		 .filter(info -> info.getName().equalsIgnoreCase(repoName))
		 .findAny();
}

Optional<InstanceInfo> getInstanceInfo(@NotNull Integer id) {
      return installed.values().stream()
		 .filter(info -> info.getId().equals(id))
		 .findAny();
}

Optional<PackageInfo> getPackageInfo(Integer id) {
      var instance = getInstanceInfo(id);
      return instance.flatMap(this::getPackageInfo);
}

public List<RepositoryInfo> getRepositoryAll() {
      return repositories;
//      return repositories.stream().filter(RepositoryInfo::isEnabled).collect(Collectors.toList());
}

public void updateRepository(String baseUrl, RepoInfoDTO dto) throws IOException {
      RepositoryInfo young = toLocalFormat(baseUrl, dto);
      boolean wasFound = false;
      int index;
      for (index = 0; !wasFound && index < repositories.size(); index++) {
	    var any = repositories.get(index);
	    wasFound = any.getName().equals(dto.getName()) &&
			   (any.getBaseUrl().equals(baseUrl) || Arrays.asList(any.getMirrors()).contains(baseUrl));
      }
      if (wasFound) {
	    repositories.set(index - 1, young);
      } else {
	    repositories.add(young);
      }
      Files.writeString(Path.of(config.repositories), toJson(repositories));
}

private RepositoryInfo toLocalFormat(String baseUrl, RepoInfoDTO dto) {
      RepositoryInfo local = new RepositoryInfo();
      local.setName(dto.getName()).setBaseUrl(baseUrl).setLastUpdate(Clock.systemUTC().millis());
      local.setMirrors(dto.getMirrors()).setPublicKey(dto.getPublicKey()).setTimeout(dto.getTimeout());
      local.setStatus(RepositoryInfo.Status.Enabled);
      return local;
}

public void setRepoMapping(Integer packageId, String repoUrl) {
      assert repositories != null && cachedRepositories != null;
      Optional<RepositoryInfo> repoInfo =
	  repositories.stream().filter(RepositoryInfo::isEnabled).filter(repo -> {
		boolean result = repo.getBaseUrl().equals(repoUrl);
		if (!result && repo.getMirrors() != null) {
		      result = Arrays.asList(repo.getMirrors()).contains(repoUrl);
		}

		return result;
	  }).findAny();
      repoInfo.ifPresent(repo -> cachedRepositories.put(packageId, repo));
}

public void updateCache(Integer id, String name, String... aliases) {
      if (cachedInfo == null) {
	    cachedInfo = new ArrayList<>();
      }
      CachedInfo cached = new CachedInfo(id, name, aliases);
      int index = cachedInfo.indexOf(cached);
      if (index != -1) {
	    cachedInfo.set(index, cached);
      } else {
	    cachedInfo.add(cached);
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

public InstallationState getPackageState(String name) {
      InstallationState state = InstallationState.NotInstalled;
      var instance = getInstanceInfo(name);
      if (instance.isPresent()) {
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
      var packageInfo = getPackageInfo(id, version);
      if (packageInfo.isPresent()) {
	    var instance = getInstanceInfo(id, version);
	    if (instance.isPresent()) {
		  state = getInstanceState(instance.get());
	    }
      } else {
	    var cached = getCachedInfo(id);
	    if (cached.isPresent())
		  state = InstallationState.Cached;
      }
      return state;
}

public @NotNull <T> T initSession(Class<T> classType) throws ConfigurationException {
      T session = null;
      try {
	    if (classType.equals(RemovableSession.class)) {
		  Path removables = Files.createTempFile(Path.of(config.temp), "remove", ".pum");
		  var remover = new RemovableSession(this);
		  remover.setJournalPath(removables);
		  session = classType.cast(remover);
	    }
	    if (classType.equals(InstallerSession.class)) {
		  Path tempConfig = Files.createTempFile(Path.of(config.temp), "installation", ".pum");
		  var installer = new InstallerSession(this);
		  installer.setJournalPath(tempConfig);
		  session = classType.cast(installer);
	    }
	    if (classType.equals(ModifierSession.class)) {
		  Path tempJournal = Files.createTempFile(Path.of(config.temp), "modification", ".pum");
		  Path cacheDir = Path.of(config.temp).resolve("cache");
		  if (Files.exists(cacheDir) && !Files.isDirectory(cacheDir)) {
			Files.delete(cacheDir);
		  }
		  if (!Files.exists(cacheDir)) {
			Files.createDirectory(cacheDir);
		  }
		  var modifier = new ModifierSession(this);
		  modifier.setJournalPath(tempJournal);
		  modifier.setCacheDirectory(cacheDir);
		  session = classType.cast(modifier);
	    }
      } catch (Exception e) {
	    throw new ConfigurationException("Impossible to create session instance");
      }
      if (session == null)
	    throw new ConfigurationException("Illegal session class");
      return session;
}

public Optional<PublishInstanceDTO> collectPublishInstance(Integer id, Publisher entity) {
      DependencyInfoDTO[] dependencies = new DependencyInfoDTO[entity.dependencies.length];
      PublishInstanceDTO dto = null;
      int index = 0;
      for (PublisherDependency info : entity.dependencies) {
	    Optional<Integer> depId = mapPackage(info.name());
	    if (depId.isPresent()) {
		  dependencies[index++] = new DependencyInfoDTO(depId.get(), info.version());
	    } else {
		  break;
	    }
      }
      File payload = Path.of(entity.exePath).toFile();
      if (index == dependencies.length && payload.exists()) {
	    dto = new PublishInstanceDTO(id, entity.version, dependencies);
	    dto.setLicense(entity.licence.toString());
	    dto.setPayloadSize((int) payload.length()); //the current limitation is payload size
      }
      return Optional.ofNullable(dto);
}

public Optional<FullPackageInfoDTO> getFullInfo(Integer id, String version) {
      var localInfo = getPackageInfo(id, version);
      Optional<FullPackageInfoDTO> fullInfo = Optional.empty();
      if (localInfo.isPresent()) {
	    fullInfo = localInfo.flatMap(this::toExternalFormat);
      }
      return fullInfo;
}

public Optional<FullPackageInfoDTO> getFullInfo(String name) {
      var localInfo = getPackageInfo(name);
      return localInfo.flatMap(this::toExternalFormat);
}

public Optional<Integer> getPackageId(String name) {
      return getInstanceInfo(name).map(InstanceInfo::getId);
}

public Optional<VersionInfoDTO> getVersionInfo(String name) {
      var dto = getFullInfo(name);
      var instance = getInstanceInfo(name);
      Optional<VersionInfoDTO> version = Optional.empty();
      if (dto.isPresent() && instance.isPresent()) {
	    version = Optional.of(getVersionInfo(dto.get(), instance.get()));
      }
      return version;
}

private VersionInfoDTO getVersionInfo(FullPackageInfoDTO dto, InstanceInfo instance) {
      return new VersionInfoDTO(instance.getId(), dto.version);
}

public Optional<FullPackageInfoDTO> completeFullInfo(ShortPackageInfoDTO dto) {
      var info = getPackageInfo(dto.id(), dto.version());
      return info.flatMap(this::toExternalFormat);
}

static void storePackageInfo(Path configDir, FullPackageInfoDTO dto, PackageAssembly assembly, String repoUrl) throws IOException {
      PackageInfo info = toLocalFormat(assembly, dto);
      info.repoUrl = repoUrl;
      String jsonInfo = toJson(info);
      Files.writeString(configDir.resolve("conf.pum"), jsonInfo);
}

private static PackageInfo toLocalFormat(PackageAssembly assembly, FullPackageInfoDTO dto) {
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
		  if (payload == PayloadType.Application)
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
	    case Replace, SilentReplace -> instances.forEach(info -> installed.put(info.getId(), info));
      }
      if (!mode.isSilent()) {
	    StringBuilder output = new StringBuilder();
	    installed.values().forEach(output::append);//invoke to string for each item
	    Files.writeString(packagesInfo, output.toString());
      }
}

void unlinkLibraries(InstanceInfo central) throws PackageIntegrityException, IOException {
      var packageInfo = getPackageInfo(central);
      List<InstanceInfo> instanceList = new ArrayList<>();
      if (packageInfo.isPresent()) {
	    for (var depInfo : packageInfo.get().dependencies) {
		  Optional<InstanceInfo> depInstance = getInstanceInfo(depInfo.packageId(), depInfo.label());
		  if (depInstance.isPresent()) {
			depInstance.get().updateLinksCnt(InstanceInfo.LinkState.Remove);
			instanceList.add(depInstance.get());
		  } else {
			throw new PackageIntegrityException("Broken dependency");
		  }
	    }
	    rebuildConfig(instanceList, RebuildMode.SilentReplace);
      } else {
	    throw new PackageIntegrityException("Package is already removed");
      }
}

public Optional<Author> getSavedAuthor(String authorName) {
      return Optional.ofNullable(localPublishers.get(authorName));
}

public void remember(Author author) throws IOException {
      if (!localPublishers.containsKey(author.name())) {
	    File publishers = Path.of(config.publishers).toFile();
	    if (!publishers.exists()) {
		  Files.createFile(publishers.toPath());
	    }
	    Files.writeString(publishers.toPath(), author.stringify() + "\n", StandardOpenOption.APPEND);
	    localPublishers.put(author.name(), author);
      }

}

//each packages also holds info about self in package directory as conf.pum
//At this moment, in central path stores configaration file
//The configuration file has <code>Local format</code>
//The result of thi function is OS relative form of ddl
//todo: link installed dependencies too (ie, if we link central package, link and closed dependencies)
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
			dependency.updateLinksCnt(InstanceInfo.LinkState.Add);
		  }
		  rebuildConfig(instances, RebuildMode.Replace);
	    }
      }//also store linkPath in executable
}

@NotNull
Path normalizePath(String name, PayloadType type) {
      Path destPath = null;
      switch (type) {
	    case Application -> {
		  destPath = Path.of(config.programs).resolve(name);
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

private @NotNull List<CachedInfo> getCachedList() {
      final Path cachePath = getCachePath();
      List<CachedInfo> cachedInfo;
      if (this.cachedInfo != null) {
	    cachedInfo = this.cachedInfo;//already was invoked to memory)
      } else {
	    cachedInfo = new ArrayList<>();
	    try {
		  String content = Files.readString(cachePath);
		  CachedInfo[] items = fromJson(content, CachedInfo[].class);
		  cachedInfo.addAll(Arrays.asList(items));
		  this.cachedInfo = cachedInfo;
	    } catch (IOException | JsonSyntaxException ignored) {
	    }
      }
      return cachedInfo;
}

public Optional<CachedInfo> getCachedInfo(@NotNull String name) {
      var list = getCachedList();
      return list.stream()
		 .filter(info -> info.similar(name))
		 .findAny();
}

public Optional<CachedInfo> getCachedInfo(@NotNull Integer id) {
      var list = getCachedList();
      return list.stream()
		 .filter(info -> info.id().equals(id))
		 .findAny();
}


Optional<PackageInfo> getPackageInfo(@NotNull String name) {
      Optional<InstanceInfo> instance = getInstanceInfo(name);
      return instance.flatMap(this::getPackageInfo);
}


Optional<PackageInfo> getPackageInfo(@NotNull Integer id, @NotNull String version) {
      var instance = getInstanceInfo(id, version);
      return instance.flatMap(this::getPackageInfo);
}

//Check around
Optional<InstanceInfo> getInstanceInfo(@NotNull String name) {
      return installed.values().stream()
		 .filter(info -> info.similar(name))
		 .findAny();
}

Optional<InstanceInfo> getInstanceInfo(@NotNull Integer id, @NotNull String version) {
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
	    if (!filePath.endsWith("pum")) {
		  payloadSize += filePath.toFile().length();
	    }
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
private Map<String, Author> localPublishers;
private Map<Integer, InstanceInfo> installed;
private Map<Integer, RepositoryInfo> cachedRepositories;
private List<RepositoryInfo> repositories;
private static final Gson gson = new Gson();
private final String CACHE_FILE_NAME = "pum.cache";
}
