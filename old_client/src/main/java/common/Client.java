package common;

import com.google.gson.Gson;
import database.CachedInfo;
import database.RepositoryInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.petos.pum.networks.dto.*;
import org.petos.pum.networks.requests.*;
import org.petos.pum.networks.transfer.*;
import org.petos.pum.networks.security.Author;
import org.petos.pum.networks.security.Encryptor;
import storage.*;
import storage.ModifierSession.Rank;
import storage.ModifierSession.VersionPredicate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static common.InputProcessor.*;
import static common.OutputProcessor.QuestionResponse;
import static common.OutputProcessor.QuestionType;
import static org.petos.pum.networks.security.Encryptor.Encryption;
import static storage.PackageStorage.*;
import static storage.StorageSession.CommitState;
import static org.petos.pum.networks.transfer.NetworkExchange.*;
import static org.petos.pum.networks.transfer.SimplexService.ServerAccessException;


public class Client {
/**
 * <code>Local</code> mode is used to collect all available local dependencies<br>
 * <code>Complete</code> mode is used to complete all dependencies whereas installed or not
 */
public enum ResolutionMode {
      Remote, Local, Removable;
      private PackageStorage storage = null;

      public static ResolutionMode valueOf(ResolutionMode mode, PackageStorage storage) {
	    mode.storage = storage;
	    return mode;
      }

      public boolean isSuitable(DependencyInfoDTO dto) {
	    if (storage == null)
		  return false;
	    InstallationState state = storage.getPackageState(dto.getPackageId(), dto.getLabel());
	    boolean response = false;
	    switch (this) {
		  case Remote -> response = state.isRemote();
		  case Local -> response = state.isLocal();
		  case Removable -> response = state.isRemovable();
	    }
	    return response;
      }
}

private enum UpdateMode {
      Release, Version;
      public static final int LATEST = 0;
}

private enum DowngradeMode {
      Highest, Lowest, Level;
      private int level;

      static {
	    Highest.level = 1; //not the zero
	    Lowest.level = -1;
	    Level.level = 1;
      }

      public void setLevel(int level) {
	    if (this == Level) {
		  level = Math.max(-1, level);
		  this.level = Math.min(level, 10);
	    }
      }

      public int level() {
	    return level;
      }
}

Logger logger = LogManager.getLogger(Client.class);
private final int port;
private Thread listenThread;
private Thread userThread;
private Configuration config;
private PackageStorage storage;
private InputProcessor input;
private OutputProcessor output;
private Encryptor.Encryption encryption = Encryption.Des;
private PackageAssembly.ArchiveType archive = PackageAssembly.ArchiveType.Brotli;
private final BinaryObjectMapper binaryObjectMapper;

public Client(int port) throws ConfigFormatException {
      this.port = port;
      input = new InputProcessor();
      output = new OutputProcessor();
      this.binaryObjectMapper = new BinaryObjectMapper();
      initConfig();
}

private static final String CONFIG_PATH = "config.json";

private void initConfig() {
      try {
	    Path configPath = Path.of(CONFIG_PATH);
	    String content = Files.readString(configPath);
	    config = new Gson().fromJson(content, Configuration.class);
	    config.init();
	    config.setSerializer(this.binaryObjectMapper);
	    storage = new PackageStorage(this.config);//throws
      } catch (IOException e) {
	    throw new RuntimeException("Impossible to proceed client without config file");
      }
}

public void start() {
      InputGroup input;
      while ((input = this.input.nextGroup()).type() != UserInput.Exit) {
	    dispatchInputGroup(input);
      }
      System.out.println("Thanks for working with us!");
}

private void dispatchInputGroup(@NotNull InputGroup group) {
      try {
	    ParameterMap params = group.params();
	    switch (group.type()) {
		  case List -> onListCommand(params);
		  case Install -> onInstallCommand(params);
		  case Remove -> onRemoveCommand(params);
		  case Repository -> onRepositoryCommand(params);
		  case Publish -> onPublishCommand(params);
		  case Upgrade -> onUpdateCommand(params);
		  case Downgrade -> onDowngradeCommand(params);
	    }
      } catch (Exception e) {
	    defaultErrorHandler(e);
      }
}

private UrlInfo toUrlInfo(RepositoryInfo info) {
      UrlInfo[] mirrors = new UrlInfo[0];
      if (info.getMirrors() != null) {
	    String[] urls = info.getMirrors();
	    mirrors = Arrays.stream(urls).map(url -> UrlInfo.valueOf(url, port)).toArray(UrlInfo[]::new);
      }
      return new UrlInfo(info.getBaseUrl(), port, mirrors);
}

private MultiplexService repositoryService() throws ServerAccessException {
      List<RepositoryInfo> list = storage.getRepositoryAll();
      List<UrlInfo> urls = list.stream().map(this::toUrlInfo).collect(Collectors.toList());
      MultiplexService service = new MultiplexService(urls);
      service.setExceptionHandler(this::defaultErrorHandler);
      return service;
}

private SimplexService packageService(Integer packageId) throws ServerAccessException {
      Optional<RepositoryInfo> info = storage.getRepository(packageId);
      SimplexService service;
      if (info.isPresent()) {
	    service = new SimplexService(toUrlInfo(info.get()));
	    service.setExceptionHandler(this::defaultErrorHandler);
      } else {
	    throw new ServerAccessException("The package id is doesn't match to any local repositories");
      }
      return service;
}

private SimplexService urlService(String repoUrl) throws ServerAccessException {
      SimplexService service = new SimplexService(UrlInfo.valueOf(repoUrl, port));
      service.setExceptionHandler(this::defaultErrorHandler);
      return service;
}

//Multithreading intrinsic)
private void dispatchService(SimplexService service) {
      service.run();
}

//this method is supposed to be multithreading
private void dispatchTask(Runnable task) {
      task.run();
}

private void onUpdateCommand(ParameterMap params) {
      List<InputParameter> raw = params.get(ParameterType.Raw);
      List<InputParameter> verbose = params.get(ParameterType.Verbose);
      Wrapper<UpdateMode> mode = new Wrapper<>(UpdateMode.Version);
      if (verbose.size() == 1 && verbose.get(0).self().equals("release")) {
	    mode.set(UpdateMode.Release);
      }
      if (raw.size() == 0) {
	    output.sendMessage("", "Init updating all package");
	    storage.localPackages().forEach(dto -> updatePackageByMode(dto.name, mode.get()));
      } else {
	    raw.forEach(r -> updatePackageByMode(r.self(), mode.get()));
      }
}

private void onDowngradeCommand(ParameterMap params) {
      List<InputParameter> raw = params.get(ParameterType.Raw);
      List<InputParameter> verbose = params.get(ParameterType.Verbose);
      List<InputParameter> shorts = params.get(ParameterType.Shorts);
      InputParameter level = null;
      Wrapper<DowngradeMode> wrapper = new Wrapper<>(DowngradeMode.Highest);
      if (verbose.size() == 1 && verbose.get(0).self().equals("level")) {
	    level = verbose.get(0);
      }
      if (level == null && shorts.size() == 1 && shorts.get(0).self().equals("l")) {
	    level = shorts.get(0);
      }
      if (level != null) {
	    try {
		  int value = Integer.parseInt(level.value());
		  DowngradeMode mode = value < 0 ? DowngradeMode.Lowest : DowngradeMode.Level;
		  mode.setLevel(value); //setValue method is safe
		  wrapper.set(mode);
	    } catch (NumberFormatException e) {
		  throw new IllegalStateException("Illegal parameter value");
	    }
      }
      if (raw.size() != 0) {
	    raw.forEach(r -> downgradePackageByMode(r.self(), wrapper.get()));
      } else {
	    throw new IllegalStateException("The package is not specified");
      }
}

private void downgradePackageByMode(String name, @NotNull DowngradeMode mode) {
      InstallationState state = storage.getPackageState(name);
      try (ModifierSession session = storage.initSession(ModifierSession.class)) {
	    if (state.isRemovable()) {
		  final VersionPredicate predicate = this::changeVersion; //impossible to restore old release -> UB
		  final VersionFunction function = (id, dto) -> getVersionByLevel(id, mode.level());
		  changeInstalledPackage(session, name, predicate, function);
	    } else {
		  output.sendError("", "The package cannot be changed");
	    }
      } catch (PackageIntegrityException | ConfigurationException e) {
	    output.sendError("Configuration error", e.getMessage());
      }
}


private void updatePackageByMode(String name, @NotNull UpdateMode mode) {
      InstallationState state = storage.getPackageState(name);
      try (ModifierSession session = storage.initSession(ModifierSession.class)) {
	    if (state.isRemovable()) { //it can be removed then it can be updated

		  final VersionPredicate predicate;
		  final VersionFunction function;
		  predicate = switch (mode) {
			case Release -> this::changeRelease;
			case Version -> this::changeVersion;
		  };
		  function = switch (mode) {
			case Release -> this::getLatestRelease;
			case Version -> (id, dto) -> getVersionByLevel(id, UpdateMode.LATEST);
		  };
		  changeInstalledPackage(session, name, predicate, function);
	    } else {
		  output.sendError("", "Package is not found or is not deletable");
	    }
      } catch (PackageIntegrityException | ConfigurationException e) { //on close
	    output.sendError("Configuration error", e.getMessage());
      }
}

private boolean changeRelease(VersionInfoDTO old, VersionInfoDTO last) {
      return ModifierSession.compare(old, last) == Rank.Silent;
}

private boolean changeVersion(VersionInfoDTO old, VersionInfoDTO last) {
      return ModifierSession.compare(old, last) == Rank.Jumping;
}

@FunctionalInterface
private interface VersionFunction {
      Optional<VersionInfoDTO> getVersion(Integer id, VersionInfoDTO local);
}

//the function attempt to check only
private void changeInstalledPackage(ModifierSession session, String name, VersionPredicate predicate, VersionFunction function) {
      Optional<Integer> packageId = storage.getPackageId(name);
      Optional<VersionInfoDTO> local = storage.getVersionInfo(name);
      Optional<VersionInfoDTO> remote;
      if (packageId.isPresent() && local.isPresent()) {
	    remote = function.getVersion(packageId.get(), local.get());
	    if (remote.isPresent() && predicate.check(local.get(), remote.get())) {
		  var lastInfo = getFullInfo(packageId.get(), remote.get().getLabel());
		  var oldInfo = storage.getFullInfo(packageId.get(), local.get().getLabel());
		  if (lastInfo.isPresent() && oldInfo.isPresent()) {
			try {
			      removeRetired(session, packageId.get(), oldInfo.get());
			      installAdvanced(session, packageId.get(), lastInfo.get());
			} catch (PackageIntegrityException e) {
			      output.sendError("Integrity error", e.getMessage());
			}
		  } else {
			if (oldInfo.isEmpty())
			      output.sendError("Internal error", "The local db corrupted");
		  }
	    } else {
		  if (remote.isPresent()) { //the current level doesn't process network error
			//it's obligatory for low-level
			output.sendMessage("", "All is up to date");
		  }
	    }
      } else {
	    output.sendMessage("Not found", "Package not found");
      }
}

private void removeRetired(ModifierSession parent, Integer id, FullPackageInfoDTO dto) throws PackageIntegrityException {
      var session = parent.getRemover();//don't close
      output.sendMessage("", "Attempt to remove old application version");
      session.removeLocally(dto);
      output.sendMessage("", "Attempt to remove old dependencies");
      ResolutionMode mode = ResolutionMode.valueOf(ResolutionMode.Removable, storage);
      Map<Integer, FullPackageInfoDTO> dependencies = resolveDependencies(dto, mode);
      if (dependencies.values().size() < dto.dependencies.length)
	    output.sendMessage("Warning", "There are some global dependencies");
      for (var dependency : dependencies.values()) {
	    session.removeLocally(dependency);
      }
      output.sendMessage("Completed", "Package was retired");
      session.commit(CommitState.Success);

}

private void installAdvanced(ModifierSession parent, Integer id, FullPackageInfoDTO dto)
    throws PackageIntegrityException {
      try {
	    var session = parent.getInstaller();
	    Optional<RepositoryInfo> repoInfo = storage.getRepository(id);
	    assert repoInfo.isPresent();
	    session.assignSource(repoInfo.get().getBaseUrl());
	    ResolutionMode mode = ResolutionMode.valueOf(ResolutionMode.Remote, storage);
	    output.sendMessage("", "Attempt to resolve dependencies");
	    Map<Integer, FullPackageInfoDTO> dependencies = resolveDependencies(dto, mode);
	    if (hasUserAgreement(dto, dependencies.values())) {
		  startInstallation(session, id, dto, dependencies);
		  parent.commit(CommitState.Success);
		  output.sendMessage("", "Updating successfully!");
	    } else {
		  output.sendMessage("", "The installation is aborted by user");
		  output.sendMessage("", "Attempt to roll back changes");
		  parent.commit(CommitState.Failed);
		  output.sendMessage("Completed", "Package was restored");
	    }
      } catch (PackageIntegrityException | ConfigurationException e) {
	    throw new PackageIntegrityException(e);
      } catch (PackageAssembly.VerificationException e) {
	    throw new PackageIntegrityException("The download package is damaged: " + e);
      }
}

private Optional<VersionInfoDTO> getVersionByLevel(Integer packageId, int level) {
      Wrapper<VersionInfoDTO> wrapper = new Wrapper<>();
      try (NetworkService service = packageService(packageId)) {
	    NetworkPacket packet = new NetworkPacket(RequestType.GetVersion, RequestCode.RESPONSE_COMPACT_FORMAT | RequestCode.TRANSFER_ATTACHED_FORMAT);
	    VersionRequest request = new VersionRequest(packageId, level);
	    packet.setPayload(serialize(request));
	    service.setRequest(packet).setResponseHandler((NetworkPacket response) -> onVersionResponse(wrapper, response));
	    service.run();
      } catch (Exception ignored) {
      }
      return Optional.ofNullable(wrapper.get());
}

private Optional<VersionInfoDTO> getLatestRelease(Integer id, VersionInfoDTO local) {
      Wrapper<VersionInfoDTO> wrapper = new Wrapper<>();
      try (NetworkService service = packageService(id)) {
	    NetworkPacket packet = new NetworkPacket(RequestType.GetVersion, RequestCode.RESPONSE_VERBOSE_FORMAT | RequestCode.TRANSFER_ATTACHED_FORMAT);
	    VersionRequest request = new VersionRequest(id, local.getLabel());
	    packet.setPayload(serialize(request));
	    service.setRequest(packet).setResponseHandler((NetworkPacket response) -> onVersionResponse(wrapper, response));
	    service.run();
      } catch (Exception ignored) {
      }
      return Optional.ofNullable(wrapper.get());
}

private void onVersionResponse(Wrapper<VersionInfoDTO> wrapper, NetworkPacket response) {
      VersionInfoDTO dto;
      if (response.type(ResponseType.class) == ResponseType.Approve) {
	    dto = construct(VersionInfoDTO.class, response.data());
	    wrapper.set(dto);
      }
}

private void onPublishCommand(ParameterMap params) {
      List<InputParameter> raws = params.get(ParameterType.Raw);
      if (raws.size() != 1) {
	    throw new IllegalStateException("Invalid count of publish file");
      }
      publishPackage(raws.get(0).self());

}

private Optional<Integer> getAuthorId(Publisher publisher, Author author) {
      if (author.token().length() < Author.PREF_TOKEN_LENGTH) {
	    output.sendError("Authentication error", "The token is too small");
      }
      Wrapper<Integer> id = new Wrapper<>();
      try (NetworkService service = urlService(publisher.origin)) { //todo: add mirror checking
	    NetworkPacket packet = new NetworkPacket(RequestType.Authorize, RequestCode.RESPONSE_VERBOSE_FORMAT | RequestCode.TRANSFER_ATTACHED_FORMAT);
	    packet.setPayload(author.stringify());
	    service.setRequest(packet).setExceptionHandler(this::handleAuthException);
	    service.setResponseHandler((NetworkPacket p) -> id.set(onAuthorizeResponse(p)));
	    service.run();
      } catch (ServerAccessException e) {
	    output.sendError("Network error", "Server doesn't response");
      } catch (Exception ignored) {
      }
      return Optional.ofNullable(id.get());
}

private void handleAuthException(Exception e) {
      if (e instanceof ServerAccessException) {
	    output.sendError("Server says", e.getMessage());
      } else if (e instanceof IOException) {
	    output.sendError("IO Error", e.getMessage());
      } else {
	    output.sendError("Internal error", e.getMessage());
      }
}

private @NotNull Integer onAuthorizeResponse(NetworkPacket response) throws ServerAccessException {
      int result;
      if (response.type(ResponseType.class) == ResponseType.Approve && response.payloadSize() == 4) {
	    result = ByteBuffer.wrap(response.data()).getInt();
      } else {
	    if (response.containsCode(ResponseCode.FORBIDDEN)) {
		  throw new ServerAccessException("Invalid credentials");
	    }
	    throw new IllegalStateException("Server declines");
      }
      return result;
}

//todo: rewrite whole function
//especially part with getting foreign packageName
private void publishPackage(@NotNull String entityPath) {
      Publisher publisher = fromConfigFile(entityPath);
      Author author;
      if (storage.getSavedAuthor(publisher.author).isEmpty()) {
	    String token = (String) output.sendQuestion("Enter your token", QuestionType.Input).value();
	    author = new Author(publisher.author, publisher.email, token);
      } else {
	    author = storage.getSavedAuthor(publisher.author).get();
      }
      Optional<Integer> authorId = getAuthorId(publisher, author); //authorId is temporary value
      if (authorId.isPresent() && storage.getSavedAuthor(author.name()).isEmpty()) {
	    var response = output.sendQuestion("Remember publisher?", QuestionType.YesNo);
	    if (response.value(Boolean.class)) {
		  try {
			storage.remember(author);
		  } catch (IOException ignored) {
		  }
	    }
      }
      if (authorId.isEmpty()) {
	    return; // the last method cannot be failed
      }
      Optional<Integer> packageId = getPackageIdByUrl(publisher.name, publisher.origin);
      if (packageId.isEmpty()) {
	    output.sendMessage("", "Attempt to publish common package info");
	    packageId = authorId.flatMap(id -> publishPackageHat(id, publisher));
      }
      if (packageId.isPresent()) { //server save some additional info
	    try (FileInputStream payloadStream = new FileInputStream(Path.of(publisher.exePath).toFile())) {
		  Optional<PublishInstanceDTO> dto = storage.collectPublishInstance(packageId.get(), publisher);
		  if (dto.isPresent()) {
			publishPayload(publisher.origin, authorId.get(), dto.get(), payloadStream);
			output.sendMessage("", "Success");
		  } else {
			output.sendError("Broken dependencies", "PUM cannot resolve mentioned dependencies");
		  }
	    } catch (IOException e) {
		  output.sendError("", "Payload is not found");
	    }
      } else {
	    output.sendMessage("Format error", "Aliases (or package's name) are busy. Licence is not correct");
      }
}

private Optional<Integer> publishPackageHat(Integer authorId, Publisher entity) {
      Wrapper<Integer> response = new Wrapper<>();//the default value is null
      try (NetworkService service = urlService(entity.origin)) {
	    PublishInfoDTO dto = extractPublishHat(entity);
	    var packet = new NetworkPacket(RequestType.PublishInfo, RequestCode.RESPONSE_VERBOSE_FORMAT | RequestCode.TRANSFER_ATTACHED_FORMAT);
	    var request = new PublishInfoRequest(authorId, dto);
	    packet.setPayload(serialize(request));
	    service.setRequest(packet)
		.setResponseHandler((NetworkPacket p) -> response.set(onPublishResponse(p)))
		.setExceptionHandler(this::onPublishException);
	    service.run();
      } catch (Exception e) {
	    output.sendError("Network error", e.getMessage());
      }
      return Optional.ofNullable(response.get());
}

private Optional<Integer> publishPayload(String repoUrl, Integer authorId, PublishInstanceDTO dto, FileInputStream fileStream) {
      Wrapper<Integer> version = new Wrapper<>();
      try (NetworkService service = urlService(repoUrl)) {
	    var packet = new NetworkPacket(RequestType.PublishPayload, RequestCode.RESPONSE_VERBOSE_FORMAT | RequestCode.TRANSFER_ATTACHED_FORMAT | RequestCode.TRANSFER_POLLED_FORMAT);
	    var request = new PublishInstanceRequest(authorId, dto);
	    packet.setPayload(serialize(request));
	    service.setRequest(packet)
		.setTailWriter((outputStream) -> writePayload(fileStream, outputStream))
		.setResponseHandler((NetworkPacket p) -> version.set(onPublishResponse(p)))
		.setExceptionHandler(this::onPublishException);
	    service.run();
      } catch (Exception e) {
	    output.sendError("Network error", e.getMessage());
      }
      return Optional.ofNullable(version.get());
}

private void writePayload(FileInputStream payloadStream, OutputStream socketStream) throws IOException {
      byte[] buffer = new byte[65536 >> 1]; //the half of TCP segment
      int cbRead;
      while ((cbRead = payloadStream.read(buffer)) > 0) {
	    socketStream.write(buffer, 0, cbRead);
      }
}

//each time on publish request server return Integer value
//PackageId or VersionId
private @Nullable Integer onPublishResponse(@NotNull NetworkPacket response) {
      Integer result = null;
      boolean approved = (response.type(ResponseType.class).equals(ResponseType.Approve));
      if (approved) {
	    result = ByteBuffer.wrap(response.data()).getInt();
      }
      if (!approved && response.containsCode(ResponseCode.VERBOSE_FORMAT))
	    output.sendError("Publish error", BinaryObjectMapper.toString(response.data()));
      return result;
}

private @NotNull Publisher fromConfigFile(@NotNull String entityPath) throws IllegalStateException {
      Path configPath = Path.of(entityPath);
      if (!configPath.endsWith(".pum"))
	    configPath = Path.of(entityPath + ".pum");
      File configFile = configPath.toFile();
      if (!configFile.exists()) {
	    throw new IllegalStateException("Publish config file is not exists");
      }
      Publisher publisher;
      try {
	    String configData = Files.readString(configFile.toPath());
	    Optional<Publisher> optional = Publisher.valueOf(configData);
	    if (optional.isPresent() && Path.of(optional.get().exePath).toFile().exists()) {
		  publisher = optional.get();
	    } else {
		  throw new IllegalStateException("Publish file has incorrect format");
	    }
      } catch (IOException e) {
	    throw new IllegalStateException("File error during publish config reading");
      }
      return publisher;
}

private void onPublishException(Exception e) {
      if (e instanceof ServerAccessException) {
	    output.sendError("Server says", e.getMessage());
      } else {
	    output.sendError("Unknown error", e.getMessage());
      }
}

private static PublishInfoDTO extractPublishHat(Publisher entity) {
      return new PublishInfoDTO(entity.name, entity.type.toString(), entity.aliases);
}

private void onRemoveCommand(ParameterMap params) {
      List<InputParameter> removables = params.get(ParameterType.Raw);
      if (removables.isEmpty())
	    throw new IllegalStateException("The package is not specified");
      for (var removable : removables) {
	    removePackage(removable.self());
      }
}

private void removePackage(String name) {
      InstallationState state = storage.getPackageState(name);
      if (state.isRemovable()) {
	    try (var session = storage.initSession(RemovableSession.class)) {
		  var fullInfo = storage.getFullInfo(name);
		  if (fullInfo.isPresent()) {
			session.removeLocally(fullInfo.get());//to mark dependencies as deletable
			//the dependency resotion is obligatory only to determine the integrity of package
			ResolutionMode mode = ResolutionMode.valueOf(ResolutionMode.Removable, storage);
			Map<Integer, FullPackageInfoDTO> dependencies = resolveDependencies(fullInfo.get(), mode);//only existing dependencies
			if (dependencies.size() < fullInfo.get().dependencies.length) {
			      output.sendMessage("Warning", "There are some global dependencies");
			}
			for (var dto : dependencies.values()) {
			      session.removeLocally(dto);
			}
			session.commit(CommitState.Success);
			output.sendMessage("Success", "The package was removed successfully");
		  } else {
			output.sendError("", String.format("The package %s is damaged", name));
		  }
	    } catch (PackageIntegrityException e) {
		  output.sendError("Broken dependencies", e.getMessage());
	    } catch (ConfigurationException e) {
		  output.sendError("", "Package configuration error");
	    }
      } else {
	    output.sendError("", String.format("The package %s is not installed or cannot be removed", name));
      }

}

private void onRepositoryCommand(ParameterMap params) {
      List<RepositoryInfo> repositories = storage.getRepositoryAll();
      boolean isVerboseMode = params.hasParameter(ParameterType.Verbose, "verbose") ||
				  params.hasParameter(ParameterType.Shorts, "v");
      boolean wasChosen = false;
      if (params.hasParameter(ParameterType.Verbose, "list")) {
	    wasChosen = true;
	    printRepository(isVerboseMode);
      }
      if (!wasChosen) {
	    Optional<InputParameter> parameter = params.get(ParameterType.Verbose).stream()
						     .filter(p -> p.self().equals("add") && !p.value().isEmpty())
						     .findFirst();
	    parameter.ifPresent(p -> addRepository(p.value()));

      }

}

private void addRepository(String url) {
      output.sendMessage("", "Try to add a new repository");
      Optional<RepoInfoDTO> dto = getRepoInfo(url);
      if (dto.isPresent()) {
	    try {
		  storage.updateRepository(url, dto.get());
	    } catch (IOException e) {
		  output.sendError("Local error", "Impossible to save repository");
	    }
	    output.sendMessage("Success", "The repo was locally added");
      } else {
	    output.sendError("", "Impossible to add the repository by " + url);
      }
}

private Optional<RepoInfoDTO> getRepoInfo(String url) {
      Wrapper<RepoInfoDTO> wrapper = new Wrapper<>();
      try (NetworkService service = urlService(url)) {
	    NetworkPacket request = new NetworkPacket(RequestType.GetRepo, RequestCode.DEFAULT_FORMAT);
	    AtomicBoolean shouldRerun = new AtomicBoolean(false);
	    service.setRequest(request).setResponseHandler((NetworkPacket p) -> {
		  if (p.type(ResponseType.class) == ResponseType.Approve) {
			wrapper.set(construct(RepoInfoDTO.class, p.data()));
		  } else {
			if (p.containsCode(ResponseCode.TRY_AGAIN)) {
			      shouldRerun.set(true);
			}
		  }
	    });
	    service.run();
	    while (wrapper.isEmpty() && shouldRerun.get()) {
		  System.out.print(".");
		  service.run();
	    }
	    if (wrapper.isEmpty() && !shouldRerun.get()) {
		  output.sendError("Server says", "Forbidden");
	    }
      } catch (ServerAccessException e) {
	    output.sendError("Network error", e.getMessage());
      } catch (Exception e) {
	    logger.error("GetRepoInfo error: " + e.getMessage());
      }
      return Optional.ofNullable(wrapper.get());
}

private boolean isValidRepository(long lastUpdate, long timeout) {
      long localTime = Clock.systemUTC().millis();
      long futureTime = lastUpdate + timeout;
      return futureTime > localTime;
}

private void printRepository(boolean isVerbose) {
      List<RepositoryInfo> repositories = storage.getRepositoryAll();
      output.sendMessage("", "Collect info about local repositories");
      Consumer<RepositoryInfo> printHandler;
      if (isVerbose)
	    printHandler = this::printRepository;
      else
	    printHandler = this::printRepositoryHat;
      output.sendMessage("", "Check repositories timeout");
      for (var repoInfo : repositories) {
	    if (repoInfo.isEnabled() && isValidRepository(repoInfo.getLastUpdate(), repoInfo.getTimeout())) {
		  printHandler.accept(repoInfo);
	    }
	    if (repoInfo.isEnabled() && !isValidRepository(repoInfo.getLastUpdate(), repoInfo.getTimeout())) {
		  Optional<RepoInfoDTO> dto = getRepoInfo(repoInfo.getBaseUrl());
		  if (dto.isPresent()) {
			try {
			      storage.updateRepository(repoInfo.getBaseUrl(), dto.get());
			      Optional<RepositoryInfo> info = storage.getRepository(repoInfo.getName());
			      if (info.isPresent()) {
				    printHandler.accept(info.get());
			      } else {
				    output.sendError("Storage error", dto.get().getName() + "is not set");
			      }
			} catch (IOException e) {
			      output.sendError("Storage error", "Local repository database is not accessible");
			}
		  }
	    }
      }
      if (repositories.isEmpty()) {
	    output.sendMessage("", "No repository is available");
      }
}

private void onListCommand(ParameterMap params) throws IOException {
      boolean isVerboseMode = params.hasParameter(ParameterType.Shorts, "v") ||
				  params.hasParameter(ParameterType.Verbose, "verbose");
      if (params.hasParameter(ParameterType.Verbose, "installed")) {
	    showPackagesLocally(isVerboseMode);
      } else {
	    showPackagesAll();
      }

}

private void showPackagesAll() {
      try (NetworkService service = repositoryService()) {
	    var request = new NetworkPacket(RequestType.GetAll, RequestCode.DEFAULT_FORMAT);
	    service.setRequest(request).setResponseHandler(this::onListAllResponse);
	    service.run();
      } catch (Exception e) {
	    output.sendError("Network error", e.getMessage());
      }
}

private void showPackagesLocally(boolean isVerbose) {
      System.out.println("Installed packages");
      final int DUMMY_ID = 0;
      var packages = storage.localPackages();
      if (isVerbose) {
	    packages.forEach(this::printPackageInfo);
      } else {
	    packages.stream()
		.map(fullInfo -> ShortPackageInfoDTO.mapDTO(fullInfo, DUMMY_ID))
		.forEach(this::printShortInfo);
      }
      if (storage.getLastStatus() != OperationStatus.Success) {
	    output.sendMessage("Integrity warning", "some packages are broken");
      }
}

private void onListAllResponse(NetworkPacket response) throws IOException {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No payload to print");
      ShortPackageInfoDTO[] packages = construct(ShortPackageInfoDTO[].class, response.data());
      System.out.format("Available packages:\n");
      for (ShortPackageInfoDTO info : packages) {
	    printShortInfo(info);
      }
}

private void onInstallCommand(ParameterMap params) {
      var rawParams = params.get(ParameterType.Raw);
      if (rawParams.isEmpty() || rawParams.size() > 1)
	    throw new IllegalArgumentException("Package is not specified");

      String packageName = rawParams.get(0).self();//raw parameter has only value
      var state = storage.getPackageState(packageName);
      if (state.isRemote())
	    installAppPackage(packageName);
      else {
	    output.sendMessage("", "Package is already installed");
      }
}

/**
 * By information in Main dto try to resolve all dependencies
 *
 * @return all dependencies that should be installed locally (without current info)
 */

private void printPackageInfo(@NotNull FullPackageInfoDTO dto) {
      String output = String.format("%-40s\t%-15s\t%-10s\t%-10s\t%-30d\n",
	  dto.name, dto.version, dto.payloadType, dto.licenseType, dto.payloadSize);
      System.out.print(output);

}

private void printRepository(RepositoryInfo repoInfo) {
      String output = String.format("name: %s\nbase-url: %s\nmirrors: %s\nstatus: %s\nlast update: %s\ntimeout: %s\n",
	  repoInfo.getName(), repoInfo.getBaseUrl(), Arrays.toString(repoInfo.getMirrors()),
	  repoInfo.getStatus(), repoInfo.getLastUpdate(), repoInfo.getTimeout());
      System.out.println(output);
}

private void printRepositoryHat(RepositoryInfo repoInfo) {
      System.out.printf("%-30s %-40s\n", repoInfo.getName(), repoInfo.getBaseUrl());

}

private boolean hasUserAgreement(@NotNull FullPackageInfoDTO info, @NotNull Collection<FullPackageInfoDTO> dependencies) {
      printPackageInfo(info);
      output.sendMessage("The additional dependencies", "");
      long totalSize = info.payloadSize;
      for (var dto : dependencies) {
	    printPackageInfo(dto);
	    totalSize += dto.payloadSize;
      }
      output.sendMessage("The total space required: ", String.format("%d", totalSize));
      QuestionResponse userResponse = output.sendQuestion("Is it ok?", QuestionType.YesNo);
      return (Boolean) userResponse.value();
}

//this method install dependencies according the common conventional rules
private long storeDependencies(InstallerSession session, Map<Integer, FullPackageInfoDTO> dependencies) throws PackageIntegrityException {
      long downloadSize = 0;
      try {
	    for (var entry : dependencies.entrySet()) {
		  var rawAssembly = getRawAssembly(entry.getKey(), entry.getValue().version, session.getEncryption());
		  if (rawAssembly.isPresent()) {
			downloadSize += rawAssembly.get().length;
			session.storeLocally(entry.getValue(), rawAssembly.get());
		  }
	    }
      } catch (PackageIntegrityException | PackageAssembly.VerificationException e) {
	    throw new PackageIntegrityException("Cannot install package: " + e.getMessage());
      }
      return downloadSize;
}

//the main thread is for gui
//the multiple thread for dependencies
//This method is final in sequence of installation
private void startInstallation(InstallerSession session, Integer id, FullPackageInfoDTO info, Map<Integer, FullPackageInfoDTO> dependencies)
    throws PackageAssembly.VerificationException, PackageIntegrityException, ConfigurationException {
      output.sendMessage("", "Installation in progress...");
      CommitState commitState = CommitState.Failed;
      session.setEncryption(encryption);
      output.sendMessage("", "Dependency installation...");
      long downloadSize = storeDependencies(session, dependencies);
      var optional = getRawAssembly(id, info.version, session.getEncryption());//latest version
      output.sendMessage("", "Verification in progress...");
      if (optional.isPresent()) {
	    output.sendMessage("", "Installation locally...");
	    downloadSize += optional.get().length;
	    session.storeLocally(info, optional.get());
	    output.sendMessage("", "Local transactions are running...");
	    commitState = CommitState.Success;
	    output.sendMessage("The total download size", "" + downloadSize);
      } else {
	    output.sendMessage("", "Failed to load package");
	    logger.warn("Package is not installed");
      }
      session.commit(commitState);
      if (commitState == CommitState.Success)
	    output.sendMessage("Congratulations!", "Installed successfully!");
}

private void assignDependencies(Integer packageId, FullPackageInfoDTO dto) {
      Optional<RepositoryInfo> repoInfo = storage.getRepository(packageId);
      if (repoInfo.isPresent() && dto.dependencies != null) {
	    String baseUrl = repoInfo.get().getBaseUrl();
	    for (var dependency : dto.dependencies) {
		  storage.setRepoMapping(dependency.getPackageId(), baseUrl);
	    }
      }
}

private void installAppPackage(String packageName) {
      Optional<Integer> packageId = Optional.empty();
      if (storage.getPackageState(packageName) == InstallationState.Cached) {
	    packageId = storage.getCachedInfo(packageName)
			    .map(CachedInfo::id);
      }
      if (packageId.isEmpty())
	    packageId = getPackageIdByDefault(packageName);
      Optional<FullPackageInfoDTO> fullInfo = packageId.flatMap(id -> getFullInfo(id, UpdateMode.LATEST));
      Optional<RepositoryInfo> repoInfo = packageId.flatMap(id -> storage.getRepository(id));
      if (fullInfo.isPresent() && isApplication(fullInfo.get()) && repoInfo.isPresent()) {
	    try (InstallerSession session = storage.initSession(InstallerSession.class)) {
		  FullPackageInfoDTO dto = fullInfo.get();
		  assignDependencies(packageId.get(), dto); //add to local storage mapping remote id to database
		  session.assignSource(repoInfo.get().getBaseUrl());
		  ResolutionMode mode = ResolutionMode.valueOf(ResolutionMode.Remote, storage);
		  Map<Integer, FullPackageInfoDTO> dependencies = resolveDependencies(dto, mode);
		  if (hasUserAgreement(dto, dependencies.values())) {
			startInstallation(session, packageId.get(), dto, dependencies);
		  } else {
			output.sendMessage("", "Installation is aborted by user");
		  }
	    } catch (PackageIntegrityException e) {
		  logger.warn("Broken dependencies: " + e.getMessage());
		  output.sendError("Broken dependencies", e.getMessage());
	    } catch (ConfigurationException e) {
		  output.sendError("common.Configuration error", "Impossible to start installation");
	    } catch (PackageAssembly.VerificationException e) {
		  output.sendMessage("Verification error", e.getMessage());
	    }
      } else {
	    output.sendMessage("", "Application is not found");
      }
}

private boolean isApplication(FullPackageInfoDTO dto) {
      return convert(dto.payloadType) == PayloadType.Application;
}

/**
 * @param encryption also holds the public key for encryption
 */
private Optional<byte[]> getRawAssembly(Integer id, String label, Encryption encryption) {
      Wrapper<byte[]> payload = new Wrapper<>();
      try (NetworkService service = packageService(id)) {
	    var packet = new NetworkPacket(RequestType.GetPayload,
		RequestCode.RESPONSE_VERBOSE_FORMAT | RequestCode.TRANSFER_ATTACHED_FORMAT);
	    var request = new PayloadRequest(id, label, archive);
	    request.setEncryption(encryption);
	    packet.setPayload(serialize(request));
	    service.setRequest(packet).setResponseHandler((NetworkPacket p) -> payload.set(onRawAssemblyResponse(p)));
	    service.run();
      } catch (Exception e) {
	    output.sendError("Network packet", e.getMessage());
      }
      return Optional.ofNullable(payload.get());
}

private byte[] onRawAssemblyResponse(NetworkPacket response) {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package exists");
      return response.data();
}


private void printShortInfo(ShortPackageInfoDTO info) {
      System.out.format("%-40s\t%-10s\t%-30s\n",
	  info.getName(), info.getVersion(), "PetOS Central");
}

/**
 * convert Entries from FullPackageInfoDTO to Dependencies Map
 */
private Map<Integer, FullPackageInfoDTO> resolveDependencies(FullPackageInfoDTO info, ResolutionMode mode) throws PackageIntegrityException {
      assert info.dependencies != null;
      List<DependencyInfoDTO> dependencies = Arrays.stream(info.dependencies)
						 .filter(mode::isSuitable)
						 .toList();
      Map<Integer, FullPackageInfoDTO> dependencyMap = new HashMap<>();
      for (var dependency : dependencies) {
	    var fullInfo = storage.getFullInfo(dependency.getPackageId(), dependency.getLabel()); //attempt to fetch data from local database
	    if (fullInfo.isEmpty())
		  fullInfo = getFullInfo(dependency.getPackageId(), dependency.getLabel()); //fetch the info from remote server
	    if (fullInfo.isPresent()) {
		  dependencyMap.put(dependency.getPackageId(), fullInfo.get());
		  dependencyMap.putAll(resolveDependencies(fullInfo.get(), mode));
	    } else {
		  logger.warn("Broken dependency:" +
				  dependency.getLabel() + " id=" + dependency.getPackageId());
		  throw new PackageIntegrityException("Broken dependency");
	    }
      }
      return dependencyMap;
}

private Optional<Integer> getPackageIdByUrl(String packageName, String url) {
      Optional<Integer> packageId = Optional.empty();
      try (NetworkService service = urlService(url)) {
	    packageId = getPackageId(service, packageName);
	    packageId.ifPresent(id -> storage.updateCache(id, packageName, url));
//	    service.close(); to prevent exception if getPackageId is didn't close
      } catch (Exception ignored) {
      }
      return packageId;
}

/**
 * This function is a bit tricky. It uses <code.>storage</code.>instance to save request result
 */
private Optional<Integer> getPackageIdByDefault(String packageName) {
      Optional<Integer> packageId = Optional.empty();
      try (MultiplexService service = repositoryService()) {
	    packageId = getPackageId(service, packageName);
//	    service.close(); to prevent exception if getPackageId is didn't close
	    Optional<UrlInfo> lastInfo = service.lastInfo();
	    packageId.ifPresent(id -> {
		  storage.updateCache(id, packageName);
		  lastInfo.ifPresent(urlInfo -> storage.setRepoMapping(id, urlInfo.url()));
	    });
      } catch (Exception ignored) {
	    output.sendError("", ignored.getMessage());
      }
      return packageId;
}

private Optional<Integer> getPackageId(NetworkService service, String packageName) {
      AtomicReference<Integer> id = new AtomicReference<>(null);
      var packet = new NetworkPacket(RequestType.GetId, RequestCode.TRANSFER_ATTACHED_FORMAT);
      var request = new IdRequest(packageName);
      packet.setPayload(serialize(request));
      service.setRequest(packet)
	  .setResponseHandler((NetworkPacket p) -> {
		Integer responseId = onPackageIdResponse(p);
		Integer oldValue = null;
		if (responseId != null) {
		      oldValue = id.compareAndExchange(null, responseId);
		}
		if (responseId != null && oldValue == null) { //the first attempt to change
		      try {
			    service.close();
		      } catch (Exception e) {
			    id.set(null); //attempt to close service as redundant functionality
			    //because each repository should support global name uniqueness
			    logger.warn(e.getMessage());
		      }
		}
	  }).run();
      return Optional.ofNullable(id.get());
}

private @Nullable Integer onPackageIdResponse(NetworkPacket response) {
      Integer result = null;
      if (response.type() == ResponseType.Approve) {
	    ByteBuffer buffer = ByteBuffer.wrap(response.data());
	    if (buffer.capacity() == 4) {
		  result = buffer.getInt();
	    } else {
		  throw new IllegalStateException("The server send illegal response");
	    }
      }
      return result;
}

private Optional<FullPackageInfoDTO> getFullInfo(Integer id, String label) {
      var packet = new NetworkPacket(RequestType.GetInfo, RequestCode.RESPONSE_VERBOSE_FORMAT | RequestCode.TRANSFER_ATTACHED_FORMAT);
      var request = new InfoRequest(id, label);
      packet.setPayload(serialize(request));
      return getFullInfo(id, packet);
}

private Optional<FullPackageInfoDTO> getFullInfo(Integer id, Integer offset) {
      var packet = new NetworkPacket(RequestType.GetInfo, RequestCode.RESPONSE_COMPACT_FORMAT | RequestCode.TRANSFER_ATTACHED_FORMAT);
      var request = new InfoRequest(id, offset);
      packet.setPayload(serialize(request));
      return getFullInfo(id, packet);
}

private byte[] onPackageInfoResponse(NetworkPacket response) {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package info");
      return response.data();
}

private Optional<FullPackageInfoDTO> getFullInfo(Integer id, NetworkPacket request) {
      Optional<FullPackageInfoDTO> dto = Optional.empty();
      try (NetworkService service = packageService(id)) {
	    Wrapper<byte[]> info = new Wrapper<>();
	    service.setRequest(request).setResponseHandler((NetworkPacket p) -> info.set(onPackageInfoResponse(p)));
	    service.run();
	    dto = Optional.ofNullable(info.get()).map(bytes -> construct(FullPackageInfoDTO.class, bytes));
      } catch (ServerAccessException e) {
	    output.sendError("", e.getMessage());
      } catch (Exception e) {
	    output.sendError("Unknown error", e.getMessage());
      }
      return dto;
}

private void defaultErrorHandler(Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
}

private byte[] serialize(Object instance) {
      assert binaryObjectMapper != null;
      return binaryObjectMapper.serialize(instance);
}

private <T> T construct(Class<T> clazz, byte[] bytes) {
      return binaryObjectMapper.construct(bytes, clazz);
}

public static void main(String[] args) {
      try {
	    Client client = new Client(3344);
	    client.start();
      } catch (ConfigFormatException e) {
	    System.out.println("Some files are not valid or absent. Impossible to start client"); //probably replace exception with recreation of program
      }
}

}
