package common;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import packages.*;
import requests.*;
import security.Author;
import security.Encryptor;
import storage.*;
import storage.ModifierSession.Rank;
import storage.ModifierSession.VersionPredicate;
import transfer.NetworkPacket;
import transfer.PackageAssembly;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static common.ClientService.*;
import static common.InputProcessor.*;
import static common.OutputProcessor.*;
import static security.Encryptor.*;
import static storage.PackageStorage.*;
import static storage.StorageSession.*;
import static transfer.NetworkExchange.*;


public class Client {
/**
 * <code>Selective</code> mode is used to install package (select dependencies that are not installed)<br>
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
	    InstallationState state = storage.getPackageState(dto.packageId(), dto.label());
	    boolean response = false;
	    switch (this) {
		  case Remote -> response = state.isRemote();
		  case Local -> response = state.isLocal();
		  case Removable -> response = state.isRemovable();
	    }
	    return response;
      }
}

public final int LATEST_VERSION = 0;
public final int OLDEST_VERSION = -1;
Logger logger = LogManager.getLogger(Client.class);
private final String serverUri;
private final int port;
private Thread listenThread;
private Thread userThread;
private Configuration config;
private PackageStorage storage;
private InputProcessor input;
private OutputProcessor output;
private Encryptor.Encryption encryption = Encryption.Des;
private PackageAssembly.ArchiveType archive = PackageAssembly.ArchiveType.Brotli;

public Client(int port, String domain) throws ConfigFormatException {
      this.serverUri = domain;
      this.port = port;
      input = new InputProcessor();
      output = new OutputProcessor();
      initConfig();
}

private static final String CONFIG_PATH = "config.json";

private void initConfig() {
      try {
	    Path configPath = Path.of(CONFIG_PATH);
	    String content = Files.readString(configPath);
	    this.config = new Gson().fromJson(content, Configuration.class);
	    this.config.init();
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

private ClientService defaultService() throws ServerAccessException {
      ClientService service = new ClientService(serverUri, port);
      service.setExceptionHandler(this::defaultErrorHandler);
      return service;
}


//Multithreading intrinsic)
private void dispatchService(ClientService service) {
      service.run();
}

//this method is supposed to be multithreading
private void dispatchTask(Runnable task) {
      task.run();
}
private enum UpdateMode {Release, Version;
	public static int LATEST = 0;
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
		  final VersionFunction function = (id, dto) -> getVersionByLevel(id, mode.level(), dto);
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
			case Version -> (id, dto) -> getVersionByLevel(id, UpdateMode.LATEST, dto);
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
		  var lastInfo = getFullInfo(packageId.get(), remote.get().label());
		  var oldInfo = storage.getFullInfo(packageId.get(), local.get().label());
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

private Optional<VersionInfoDTO>  getVersionByLevel(Integer id, int level, VersionInfoDTO local) {
      Wrapper<VersionInfoDTO> wrapper = new Wrapper<>();
      try {
	    ClientService service = defaultService();
	    NetworkPacket packet = new NetworkPacket(RequestType.GetVersion, RequestCode.INT_FORMAT);
	    VersionRequest request = new VersionRequest(id, level);
	    packet.setPayload(request.stringify());
	    service.setRequest(packet).setResponseHandler((response, socket) -> {
		  onVersionResponse(wrapper, response);
	    });
	    service.run();
      } catch (ServerAccessException ignored) {
      }
      return Optional.ofNullable(wrapper.get());
}

private Optional<VersionInfoDTO> getOldestVersion(Integer id, VersionInfoDTO local) {
      return getVersionByLevel(id, OLDEST_VERSION, local);
}

private Optional<VersionInfoDTO> getLatestVersion(Integer id, VersionInfoDTO local) {
      return getVersionByLevel(id, UpdateMode.LATEST, local);
}

private Optional<VersionInfoDTO> getLatestRelease(Integer id, VersionInfoDTO local) {
      Wrapper<VersionInfoDTO> wrapper = new Wrapper<>();
      try {
	    ClientService service = defaultService();
	    NetworkPacket packet = new NetworkPacket(RequestType.GetVersion, RequestCode.STR_FORMAT);
	    VersionRequest request = new VersionRequest(id, local.label());
	    packet.setPayload(request.stringify());
	    service.setRequest(packet).setResponseHandler((response, socket) -> {
		  onVersionResponse(wrapper, response);
	    });
	    service.run();
      } catch (ServerAccessException ignored) {
      }
      return Optional.ofNullable(wrapper.get());
}

private void onVersionResponse(Wrapper<VersionInfoDTO> wrapper, NetworkPacket response) {
      VersionInfoDTO dto;
      if (response.type(ResponseType.class) == ResponseType.Approve) {
	    dto = fromJson(response.stringData(), VersionInfoDTO.class);
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

private Optional<Integer> getAuthorId(Author author) {
      if (author.token().length() < Author.PREF_TOKEN_LENGTH) {
	    output.sendError("Authentication error", "The token is too small");
      }
//      Author author = new Author(publisher.author, publisher.email, token);
      Wrapper<Integer> id = new Wrapper<>();
      try {
	    ClientService service = defaultService();
	    NetworkPacket packet = new NetworkPacket(RequestType.Authorize, RequestCode.STR_FORMAT);
	    packet.setPayload(author.stringify());
	    service.setRequest(packet).setExceptionHandler(this::handleAuthException)
		.setResponseHandler((p, s) -> id.set(onAuthorizeResponse(p, s)));
	    service.run();
      } catch (IOException e) {
	    output.sendError("Network error", "Server doesn't response");
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

private @NotNull Integer onAuthorizeResponse(NetworkPacket response, Socket socket) throws ServerAccessException {
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
      Optional<Integer> authorId = getAuthorId(author);
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
      Optional<Integer> packageId = getPackageId(publisher.name);
      if (packageId.isEmpty()) {
	    output.sendMessage("", "Attempt to publish common package info");
	    packageId = authorId.flatMap(id -> publishPackageHat(id, publisher));
      }
      if (packageId.isPresent()) { //server save some additional info
	    try (FileInputStream payloadStream = new FileInputStream(Path.of(publisher.exePath).toFile())) {
		  Optional<PublishInstanceDTO> dto = storage.collectPublishInstance(packageId.get(), publisher);
		  if (dto.isPresent()) {
			publishPayload(authorId.get(), dto.get(), payloadStream);
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
      try {
	    ClientService service = defaultService();
	    var packet = new NetworkPacket(RequestType.PublishInfo, RequestCode.STR_FORMAT);
	    PublishInfoDTO dto = extractPublishHat(entity);
	    var request = new PublishInfoRequest(authorId, dto);
	    packet.setPayload(request.stringify());
	    service.setRequest(packet)
		.setResponseHandler((r, s) -> response.set(onPublishResponse(r, s)))
		.setExceptionHandler(this::onPublishException);
	    service.run();
      } catch (ServerAccessException e) {
	    output.sendError("Network error", e.getMessage());
      }
      return Optional.ofNullable(response.get());
}

private Optional<Integer> publishPayload(Integer authorId, PublishInstanceDTO dto, FileInputStream fileStream) {
      Wrapper<Integer> version = new Wrapper<>();
      ClientService service;
      try {
	    service = defaultService();
      } catch (ServerAccessException e) {
	    output.sendError("Network error", e.getMessage());
	    return Optional.empty();
      }
      var packet = new NetworkPacket(RequestType.PublishPayload, RequestCode.STR_FORMAT | RequestCode.TAIL_FORMAT);
      var request = new PublishInstanceRequest(authorId, dto);
      packet.setPayload(request.stringify());
      service.setRequest(packet)
	  .setTailWriter((outputStream) -> writePayload(fileStream, outputStream))
	  .setResponseHandler((r, s) -> version.set(onPublishResponse(r, s)))
	  .setExceptionHandler(this::onPublishException);
      service.run();
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
private @Nullable Integer onPublishResponse(@NotNull NetworkPacket response, Socket socket) {
      Integer result = null;
      boolean approved = (response.type(ResponseType.class).equals(ResponseType.Approve));
      if (approved) {
	    result = ByteBuffer.wrap(response.data()).getInt();
      }
      if (!approved && response.containsCode(ResponseCode.VERBOSE_FORMAT))
	    output.sendError("Publish error", NetworkPacket.toString(response.data()));
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
      return new PublishInfoDTO(entity.name, entity.aliases, entity.type.toString());
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
      try {
	    ClientService service = defaultService();
	    var request = new NetworkPacket(RequestType.GetAll, RequestCode.NO_CODE);
	    service.setRequest(request)
		.setResponseHandler(this::onListAllResponse);
	    service.run();
      } catch (ServerAccessException e) {
	    output.sendError("Network error", "Server is busy");
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
		.map(full -> ShortPackageInfoDTO.valueOf(DUMMY_ID, full))
		.forEach(this::printShortInfo);
      }
      if (storage.getLastStatus() != OperationStatus.Success) {
	    output.sendMessage("Integrity warning", "some packages are broken");
      }
}

private void onListAllResponse(NetworkPacket response, Socket socket) throws IOException {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No payload to print");
      String jsonInfo = response.stringData();
      ShortPackageInfoDTO[] packages = new Gson().fromJson(jsonInfo, ShortPackageInfoDTO[].class);
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
      //todo: rewrite onto parallel stream or dispatchTask methods
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

private void installAppPackage(String packageName) {
      Optional<Integer> packageId = Optional.empty();
      if (storage.getPackageState(packageName) == InstallationState.Cached) {
	    packageId = storage.getCachedInfo(packageName)
			    .map(ShortPackageInfoDTO::id);
      }
      if (packageId.isEmpty())
	    packageId = getPackageId(packageName);
      Optional<FullPackageInfoDTO> fullInfo = packageId.flatMap(id -> getFullInfo(id, UpdateMode.LATEST));
      try (InstallerSession session = storage.initSession(InstallerSession.class)) {
	    if (fullInfo.isPresent() && isApplication(fullInfo.get())) {
		  FullPackageInfoDTO dto = fullInfo.get();
		  ResolutionMode mode = ResolutionMode.valueOf(ResolutionMode.Remote, storage);
		  Map<Integer, FullPackageInfoDTO> dependencies = resolveDependencies(dto, mode);
		  if (hasUserAgreement(dto, dependencies.values())) {
			startInstallation(session, packageId.get(), dto, dependencies);
		  } else {
			output.sendMessage("", "Installation is aborted by user");
		  }
	    } else {
		  output.sendMessage("", "Application is not found");
	    }
      } catch (PackageIntegrityException e) {
	    logger.warn("Broken dependencies: " + e.getMessage());
	    output.sendError("Broken dependencies", e.getMessage());
      } catch (ConfigurationException e) {
	    output.sendError("common.Configuration error", "Impossible to start installation");
      } catch (PackageAssembly.VerificationException e) {
	    output.sendMessage("Verification error", e.getMessage());
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
      try {
	    var packet = new NetworkPacket(RequestType.GetPayload,
		RequestCode.STR_FORMAT | RequestCode.TRANSFER_FORMAT);
	    var request = new PayloadRequest(id, label, archive);
	    request.setEncryption(encryption);
	    packet.setPayload(request.stringify());
	    ClientService service = defaultService();
	    service.setRequest(packet)
		.setResponseHandler((r, s) -> payload.set(onRawAssemblyResponse(r, s)));
	    service.run();
      } catch (ServerAccessException e) {
	    output.sendError("", e.getMessage());
      }
      return Optional.ofNullable(payload.get());
}

private byte[] onRawAssemblyResponse(NetworkPacket response, Socket socket) {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package exists");
      return response.data();
}


private void printShortInfo(ShortPackageInfoDTO info) {
      System.out.format("%-40s\t%-10s\t%-30s\n",
	  info.name(), info.version(), "PetOS Central");
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
	    var fullInfo = storage.getFullInfo(dependency.packageId(), dependency.label()); //attempt to fetch data from local database
	    if (fullInfo.isEmpty())
		  fullInfo = getFullInfo(dependency.packageId(), dependency.label()); //fetch the info from remote server
	    if (fullInfo.isPresent()) {
		  dependencyMap.put(dependency.packageId(), fullInfo.get());
		  dependencyMap.putAll(resolveDependencies(fullInfo.get(), mode));
	    } else {
		  logger.warn("Broken dependency:" +
				  dependency.label() + " id=" + dependency.packageId());
		  throw new PackageIntegrityException("Broken dependency");
	    }
      }
      return dependencyMap;
}

private Optional<Integer> getPackageId(String packageName) {
      Wrapper<Integer> id = new Wrapper<>();
      try {
	    var packet = new NetworkPacket(RequestType.GetId, RequestCode.NO_CODE);
	    var request = new IdRequest(packageName);
	    packet.setPayload(request.stringify());
	    ClientService service = defaultService();
	    service.setRequest(packet)
		.setResponseHandler((p, s) -> id.set(onPackageIdResponse(p, s)));
	    service.run();
      } catch (ServerAccessException e) {
	    output.sendError("", e.getMessage());
      }
      return Optional.ofNullable(id.get());
}

private int onPackageIdResponse(NetworkPacket response, Socket socket) {
      Integer result = null;
      if (response.type() == ResponseType.Approve) {
	    ByteBuffer buffer = ByteBuffer.wrap(response.data());
	    if (buffer.capacity() == 4)
		  result = buffer.getInt();
      }
      if (result == null)
	    throw new IllegalArgumentException("Server declines");
      return result;
}

private Optional<FullPackageInfoDTO> getFullInfo(Integer id, String label) {
      var packet = new NetworkPacket(RequestType.GetInfo, RequestCode.STR_FORMAT);
      var request = new InfoRequest(id, label);
      packet.setPayload(request.stringify());
      return getFullInfo(packet);
}

private Optional<FullPackageInfoDTO> getFullInfo(Integer id, Integer offset) {
      var packet = new NetworkPacket(RequestType.GetInfo, RequestCode.INT_FORMAT);
      var request = new InfoRequest(id, offset);
      packet.setPayload(request.stringify());
      return getFullInfo(packet);
}

private String onPackageInfoResponse(NetworkPacket response, Socket socket) {
      if (response.type() != ResponseType.Approve)
	    throw new IllegalStateException("No valid package info");
      return response.stringData();
}

private Optional<FullPackageInfoDTO> getFullInfo(NetworkPacket request) {
      FullPackageInfoDTO dto = null;
      try {
	    ClientService service = defaultService();
	    Wrapper<String> info = new Wrapper<>();
	    service.setRequest(request)
		.setResponseHandler((r, s) -> info.set(onPackageInfoResponse(r, s)));
	    service.run();
	    var stringInfo = Optional.ofNullable(info.get());
	    if (stringInfo.isPresent()) {
		  dto = new Gson().fromJson(stringInfo.get(), FullPackageInfoDTO.class);
	    }
	    if (dto != null && dto.aliases == null)
		  dto.aliases = new String[0];
      } catch (ServerAccessException e) {
	    output.sendError("", e.getMessage());
      }
      return Optional.ofNullable(dto);
}

private void defaultErrorHandler(Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
}

public static void main(String[] args) {
      try {
	    Client client = new Client(3344, "self.ip");
	    client.start();
      } catch (ConfigFormatException e) {
	    System.out.println("Some files are not valid or absent. Impossible to start client"); //probably replace exception with recreation of program
      }
}

}
