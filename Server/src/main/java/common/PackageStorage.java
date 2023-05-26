package common;

import database.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jetbrains.annotations.NotNull;
import dto.*;
import security.Author;
import security.Encryptor;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.time.Clock;
import java.util.*;

public class PackageStorage {
public static class PackageId {
      private final int value;

      private PackageId(Integer value) {
	    this.value = value;
      }

      public Integer value() {
	    return value;
      }

      private @NotNull
      static PackageId valueOf(Integer id) {
	    return new PackageId(id);
      }

      @Override
      public boolean equals(Object other) {
	    if (other instanceof PackageId)
		  return ((PackageId) other).value == value;
	    return false;
      }

      @Override
      public int hashCode() {
	    return value;
      }
}

public static class VersionId {
      private final int value;

      public VersionId(int value) {
	    this.value = value;
      }

      public Integer value() {
	    return value;
      }

      private static VersionId valueOf(Integer id) {
	    return new VersionId(id);
      }
}

public record AuthorId(Integer value) {
      private static AuthorId valueOf(Integer id) {
	    return new AuthorId(id);
      }
}

public static class StorageException extends Exception {
      StorageException(String message) {
	    super(message);
      }
}

private static String DEFAULT_ARCHIVE = "None";
private static Logger logger = LogManager.getLogger(PackageStorage.class);
//not will be replaced by database
//this exactly convert
//it's supposed to be suitable to have hashtable
//conversation String to PackageId is the most frequently (I believe) operation
//always in coherent state
private NameMapper nameMapper; //upper functionality

public PackageStorage() {
      signTimer = new Timer(true);
      init();
      initNameMapper();
}

private SessionFactory dbFactory;

public void close() {
      dbFactory.close();
}

public List<ShortPackageInfoDTO> shortInfoList() {
      List<ShortPackageInfoDTO> list = new ArrayList<>();
      List<PackageHat> hats;
      try (Session session = dbFactory.openSession()) {
	    hats = getPackageHatAll(session);
      }
      for (PackageHat hat : hats) {
	    PackageId id = PackageId.valueOf(hat.getId());
	    Optional<ShortPackageInfoDTO> info = getShortInfo(id);
	    info.ifPresent(list::add);
      }
      return list;
}

//The important assumption is that all database check is unsafe (doesn't check anything)
//Because all should be checked before addition
private void amendAliases(PackageId id, @NotNull Collection<String> aliases) throws StorageException {
      //checkUnique
      if (!nameMapper.addAll(id, aliases)) //it's realy easy to check in memory aliases
	    //comparing with addition in DataBase
	    throw new StorageException("Aliases already defined");
}

//it's forbidden for everyone to change existing PackageHat (if it's not valid)
private void checkPermissions(AuthorId author, PublishInfoDTO info) throws StorageException {
      List<String> aliases = new ArrayList<>(Arrays.asList(info.aliases()));
      aliases.add(info.name());
      boolean isPermitted = true;
      try (Session session = dbFactory.openSession()) {
	    for (String alias : aliases) {
		  Optional<PackageId> packageId = toPackageId(alias);
		  Optional<PackageHat> packageHat = Optional.empty();
		  if (packageId.isPresent()) {
			packageHat = getAnyPackageHat(session, packageId.get());
		  }
		  if (packageHat.isPresent()) {
			var hat = packageHat.get();
			isPermitted = hat.getAuthorId().equals(author.value()) && !hat.isValid();
		  }
		  if (!isPermitted)
			break;
	    }
      }
      if (!isPermitted)
	    throw new StorageException("Package name is busy");
}

private void checkInstance(AuthorId author, PublishInstanceDTO dto) throws StorageException {
      boolean isValidInstance;
      try (Session session = dbFactory.openSession()) {
	    session.beginTransaction();
	    Query query = session.createQuery("from PackageHat where authorId= :author and id= :packageId");
	    query.setParameter("author", author.value()).setParameter("packageId", dto.packageId());
	    List<PackageHat> hats = query.getResultList();
	    session.getTransaction().commit();
	    isValidInstance = hats != null && hats.size() != 0;
      }
      if (!isValidInstance)
	    throw new StorageException("Version label is busy");
}

public Optional<PackageId> toPackageId(String aliasName) {
      return nameMapper.get(aliasName);
}

/**
 * Used to convert public value to PackageId instance
 */
public Optional<PackageId> getPackageId(int value) {
      PackageId id = PackageId.valueOf(value);
      try (Session session = dbFactory.openSession()) {
	    var optional = getValidPackageHat(session, id);
	    if (optional.isEmpty())
		  id = null;
      }
      return Optional.ofNullable(id);
}

public Optional<AuthorId> getAuthorId(Integer id) {
      AuthorId authorId = null;
      try (Session session = dbFactory.openSession()) {
	    session.beginTransaction();
	    AuthorInfo author = session.get(AuthorInfo.class, id);
	    session.getTransaction().commit();
	    if (author != null)
		  authorId = AuthorId.valueOf(id);
      }
      return Optional.ofNullable(authorId);
}

public Optional<AuthorId> getAuthorId(Author author) {
      AuthorId result = null;
      try (Session session = dbFactory.openSession()) {
	    AuthorInfo localAuthor = getAuthorInfo(session, author).orElse(null);
	    if (localAuthor != null) {
		  byte[] salt = Base64.getDecoder().decode(localAuthor.getSalt());
		  String hash = getHashCode(author.token(), salt);
		  if (hash.equals(localAuthor.getHash())) {
			result = AuthorId.valueOf(localAuthor.getId());
		  }
	    }
      }
      return Optional.ofNullable(result);
}

public void authorize(Author author) throws StorageException {
      try (Session session = dbFactory.openSession()) {
	    AuthorInfo localAuthor = getAuthorInfo(session, author).orElse(null);
	    if (localAuthor == null && author.token().length() >= Author.PREF_TOKEN_LENGTH) {
		  byte[] salt = getSalt();
		  String hash = getHashCode(author.token(), salt);
		  String stringSalt = Base64.getEncoder().encodeToString(salt);
		  localAuthor = AuthorInfo.valueOf(author.name(), hash, stringSalt);
		  session.beginTransaction();
		  session.save(localAuthor);
		  session.getTransaction().commit();
	    } else {
		  throw new StorageException("The same name is already defined");
	    }
      }
}

/**
 * Convert versionOffset to unique versionId.
 * Generally, each certain package is determined by packageId and versionId. In case, if current version offset is not exists
 * return last available package.<br>
 * if version == 0: get latest version<br>
 * if version == -1: get oldest version<br>
 * elsif version in [1,9]: get specific latter version<br>
 * else: get minimal available version<br>
 *
 * @param versionOffset is client view of version<br> See getDataPackage to determine how it works
 * @param id            is unique packageId
 * @return unique versionId
 */
public Optional<VersionId> mapVersion(PackageId id, int versionOffset) {
      final int MAX_PACKAGES_CNT = 10;//only ten latest packages are available
      Optional<VersionId> result = Optional.empty();
      try (Session session = dbFactory.openSession()) {
	    session.beginTransaction();
	    Query query = session.createQuery("from PackageInfo where packageId= :familyId order by time desc");
	    query.setParameter("familyId", id.value());
	    query.setMaxResults(MAX_PACKAGES_CNT);
	    List<PackageInfo> infoList = query.getResultList();
	    session.getTransaction().commit();
	    int maxOffset = infoList.size() - 1;
	    if (versionOffset != -1) {
		  versionOffset = Math.max(versionOffset, 0); //not negative
		  versionOffset = Math.min(versionOffset, maxOffset); //any correct offset
	    } else {
		  versionOffset = maxOffset;
	    }
	    if (infoList.size() != 0) { //last assertion should be erased
		  int version = infoList.get(versionOffset).getVersionId();
		  VersionId versionId = VersionId.valueOf(version);
		  result = Optional.of(versionId);
	    }
      }
      return result;
}


public Optional<VersionId> mapVersion(PackageId id, String label) {
      Optional<PackageInfo> instance;
      try (Session session = dbFactory.openSession()) {
	    session.beginTransaction();
	    Query query = session.createQuery("from PackageInfo where packageId= :packageId and versionLabel= :label");
	    query.setParameter("label", label).setParameter("packageId", id.value());
	    instance = query.getResultList().stream().findAny();
	    session.getTransaction().commit();
      }
      return instance.map(info -> VersionId.valueOf(info.getVersionId()));
}
//assume: aliases are unique


/**
 * @return sorted DataPackage according time that had been added <br>
 * More fresh Data stores closer to first item. <br>
 * IE. The first item is most fresh
 */
//todo: access to table with short info
public Optional<ShortPackageInfoDTO> getShortInfo(PackageId id) {
      Optional<ShortPackageInfoDTO> dto;
      try (Session session = dbFactory.openSession()) {
	    Optional<PackageHat> hat = getValidPackageHat(session, id);
	    Optional<PackageInfo> optionalInfo;
	    session.beginTransaction();
	    Query query =
		session.createQuery("from PackageInfo where packageId= :id order by time desc")
		    .setParameter("id", id.value()).setMaxResults(1);
	    optionalInfo = query.getResultList().stream().findAny();
	    session.getTransaction().commit();
	    dto = optionalInfo.map(info -> new ShortPackageInfoDTO(id.value(), hat.get().getName(),
		info.getVersionLabel(), hat.get().getAliases().toArray(String[]::new)));
      }
      return dto;
}
public RepoInfoDTO getRepoInfo() {
      RepoInfoDTO dto = new RepoInfoDTO();
      dto.setName(config.getRepoName());
      dto.setMirrors(config.getMirrors());
      completeRepoInfo(dto);
      return dto;
}
private void completeRepoInfo(@NotNull RepoInfoDTO dto) {
      Long currTime = Clock.systemUTC().millis();
      Long lastUpdate = getLastTimerUpdate();
      long diff = currTime - lastUpdate;
      if (!DEFAULT_ENCRYPTION.holdsKey()) {
	    PublicKey key = getPublicSign();
	    DEFAULT_ENCRYPTION.detachKey(key);
      }
      dto.setPublicKey(DEFAULT_ENCRYPTION.getEncoded());
      dto.setTimeout(diff);
}
public Optional<FullPackageInfoDTO> getFullInfo(@NotNull PackageId id, @NotNull VersionId version) {
      FullPackageInfoDTO dto = null;
      try (Session session = dbFactory.openSession()) {
	    var optionalInfo = getInstanceInfo(session, id, version);
	    var optionalHat = getValidPackageHat(session, id);
	    if (optionalHat.isPresent() && optionalInfo.isPresent()) {

		  PackageHat hat = optionalHat.get();
		  PackageInfo info = optionalInfo.get();
		  dto = new FullPackageInfoDTO();
		  dto.version = info.getVersionLabel();
		  dto.aliases = hat.getAliases().toArray(new String[0]);
		  dto.name = hat.getName();
		  dto.licenseType = info.getLicence().getName();
		  dto.payloadType = hat.getPayload().getName();
		  var payload = info.getPayloads().stream().findAny(); //replace
		  if (payload.isPresent()) {
			dto.payloadSize = (int) Files.size(Path.of(payload.get().getPath()));
			dto.dependencies = collectDependencies(info).toArray(new DependencyInfoDTO[0]);
		  } else {
			dto = null;
		  }
		  //todo: exhause max file size to Long

	    }
      } catch (StorageException | IOException e) {
	    dto = null;
	    logger.error("Broken dependency" +
			     "Id= " + id.value() +
			     "Version=" + version.value());
      }
      return Optional.ofNullable(dto);
}

/**
 * The terrible thing is that memory can be finished by a many simultaneously connection
 * Should be replaced by OutputStream
 */
public Optional<byte[]> getPayload(PackageId id, VersionId version) {
      final String archiveType = "None"; //hardly
      byte[] bytes = null;
      try (Session session = dbFactory.openSession()) {
	    var optional = getInstanceInfo(session, id, version);
	    if (optional.isPresent()) {
		  var info = optional.get();
		  Optional<Path> payloadPath = info.getPayloads().stream()
						   .findAny()
						   .map(payload -> Path.of(payload.getPath()))
						   .filter(path -> Files.exists(path) && Files.isReadable(path));
		  try {
			if (payloadPath.isPresent()) {
			      bytes = Files.readAllBytes(payloadPath.get());
			}
		  } catch (IOException e) {
			logger.error("Impossible to read package payload: " + e.getMessage());
		  }
	    }
      }
      return Optional.ofNullable(bytes);
}

//check package on uniqueness and store in database
//return package
public @NotNull PackageId storePackageInfo(AuthorId author, PublishInfoDTO dto) throws
    StorageException {
      checkPermissions(author, dto);//methods throw exception if something wrong
      return initPackageHat(author, dto);
}

public void updatePackageInfo(AuthorId author, PackageId id, PublishInfoDTO dto) throws StorageException {
      checkPermissions(author, dto);
      updatePackageHat(author, id, dto);
}

/**
 * Automatically remove old package and replace it by new payload
 *
 * @throws StorageException if packageFamily by PackageId is not exists
 */
//todo: add release replacement logic
public VersionId storePayload(@NotNull AuthorId author, @NotNull PublishInstanceDTO dto, @NotNull byte[] payload) throws StorageException {
      checkInstance(author, dto);
      //replace with simple rules
      return updatePackageInfo(dto, payload);
}

//todo: here should be database trigger
private synchronized @NotNull VersionId nextVersionId(PackageId id, String label) {
      List<PackageInfo> infoList;
      VersionId version;
      try (Session session = dbFactory.openSession()) {
	    session.beginTransaction();
	    Query query = session.createQuery("from PackageInfo where packageId= :id order by time desc");
	    query.setParameter("id", id.value());
	    infoList = query.getResultList();
	    session.getTransaction().commit();
	    Optional<PackageInfo> oldInstance = infoList.stream()
						    .filter(info -> info.getVersionLabel().equalsIgnoreCase(label))
						    .findAny();//only single instance can exists
	    oldInstance.ifPresent(old -> {
		  session.beginTransaction();
		  session.remove(old);
		  session.getTransaction().commit();
	    });
      }
      if (!infoList.isEmpty()) {
	    var info = infoList.get(0); //the last item will first
	    version = VersionId.valueOf(info.getVersionId() + 1);//there should be trigger in database
      } else {
	    logger.info("Initialization of PackageInfo entity");
	    version = VersionId.valueOf(0);
      }
//      removeOldestVersion(id);
      return version;
}

private @NotNull Path toPayloadPath(PackageHat hat, PublishInstanceDTO dto) throws IOException {
      Path packagesPath = Path.of(config.getPackages()).resolve(hat.getName());
      if (Files.exists(packagesPath) && !Files.isDirectory(packagesPath))
	    throw new IOException("Invalid File system structure");
      if (!Files.exists(packagesPath)) {
	    Files.createDirectory(packagesPath);
      }
      return packagesPath.resolve(hat.getPayload().getName() + dto.version());
}

/**
 * Check if package instance has dependencies in other package
 */
private boolean hasDeepDependencies(PackageId id, VersionId version) {
      return false;
}

private void removeOldestVersion(PackageId id) {
      Session session = dbFactory.openSession();
      session.beginTransaction();
      Query query = session.createQuery("from PackageInfo where packageId= :id order by time asc");
      query.setParameter("id", id);
      query.setMaxResults(1);
      List<PackageInfo> infoList = query.getResultList();
      session.getTransaction().commit();
      if (!infoList.isEmpty()) {
	    var info = infoList.get(0);
	    //todo: remove payload
      } else {
	    logger.warn("No PackageInfo to remove");
      }
}

private void deletePayload(File file) {
      boolean isDeleted = false;
      while (!isDeleted && file.exists()) {
	    try {
		  Files.delete(file.toPath());
		  isDeleted = true;
	    } catch (IOException e) {
		  logger.warn("File is busy: " + file.getAbsolutePath());
	    }
	    if (!isDeleted)
		  Thread.yield();
      }
}

private void removeVersion(PackageId id, VersionId version) {
      Session session = dbFactory.openSession();
      session.beginTransaction();
      var instanceId = InstanceId.valueOf(id.value(), version.value());
      PackageInfo info = session.get(PackageInfo.class, instanceId);
      session.remove(info);
      session.getTransaction().commit();
      new Thread(() -> {
	    //todo: remove payload
      }).start();
}

private void removePackageAll(@NotNull Session session, PackageId id) {
      var optionalHat = getValidPackageHat(session, id);
      if (optionalHat.isPresent()) {
	    session.beginTransaction();
	    session.getTransaction().commit();
      } else {
	    logger.warn("Attempt to remove not existent package");
      }
}

/**
 * Set only known info
 */
@SessionMethod
private PackageInfo constructFrom(@NotNull Session session, VersionId version, @NotNull PublishInstanceDTO dto, @NotNull String path) throws StorageException {
      Licence licence = fetchLicense(session, dto.getLicense());
      Archive archive = fetchArchive(session, DEFAULT_ARCHIVE);
      PackageInfo info = PackageInfo.valueOf(dto.packageId(), version.value());
      info.setPackageId(dto.packageId());
      info.setVersionLabel(dto.version());
      info.setLicence(licence);
      info.setPayloadPath(archive, path);
      Set<DependencyId> dependencies = new HashSet<>();
      for (DependencyInfoDTO libDTO : dto.dependencies()) {
	    Optional<PackageId> libId = getPackageId(libDTO.packageId());
	    Optional<VersionId> libVersion = libId.flatMap(id -> mapVersion(id, dto.version()));
	    var instance = libVersion.flatMap(v -> getInstanceInfo(session, libId.get(), v));
	    if (instance.isPresent()) {
		  dependencies.add(new DependencyId(libId.get().value(), libVersion.get().value()));
	    } else {
		  logger.error("The package " + dto.packageId() + " has broken dependencies");
		  throw new StorageException("Broken dependencies");
	    }
      }
      info.setDependencies(dependencies);
      return info;
}

/**
 * All checks are passed
 */
private VersionId updatePackageInfo(@NotNull PublishInstanceDTO dto, byte[] payload) throws StorageException {
      VersionId version;
      try (Session session = dbFactory.openSession()) {
	    PackageId id = PackageId.valueOf(dto.packageId());
	    Optional<PackageHat> hat = getAnyPackageHat(session, id);
	    if (hat.isPresent()) {
		  version = nextVersionId(id, dto.version());//each time unique)
		  PackageInfo info;
		  try {
			Path path = toPayloadPath(hat.get(), dto);
			Files.deleteIfExists(path);
			Files.write(path, payload);
			info = constructFrom(session, version, dto, path.toString());
		  } catch (IOException e) {
			throw new StorageException("Error during file saving");
		  }
		  session.beginTransaction();
		  session.save(info);
		  session.getTransaction().commit();
		  validatePackageHat(session, id, true); //now it's forbidden to change PackageHat
	    } else {
		  throw new StorageException("Invalid package id");
	    }
      }
      return version;
}

@SessionMethod
private PackageHat constructFrom(Session session, @NotNull AuthorId authorId, @NotNull PublishInfoDTO dto) throws StorageException {
      PackageHat hat = PackageHat.valueOf(dto.name(), dto.aliases());
      Payload payload = fetchPayload(session, dto.payloadType()); //throws StorageException
      hat.setPayload(payload);
      hat.setAuthorId(authorId.value());
      return hat;
}

private void updatePackageHat(AuthorId author, PackageId id, PublishInfoDTO dto) throws StorageException {
      try (Session session = dbFactory.openSession()) {
	    PackageHat hat = constructFrom(session, author, dto);
	    hat.setId(id.value());
	    session.beginTransaction();
	    session.merge(hat);
	    session.getTransaction().commit();
      }
}

/**
 * Assume that all checks are passed
 * To add this dto to database is safe
 */
private @NotNull PackageId initPackageHat(@NotNull AuthorId author, @NotNull PublishInfoDTO dto) throws StorageException {
      PackageId id;
      try (Session session = dbFactory.openSession()) {
	    PackageHat hat = constructFrom(session, author, dto);
	    session.beginTransaction();
	    id = PackageId.valueOf((Integer) session.save(hat));
	    session.getTransaction().commit();
	    Collection<String> aliases = hat.getAliases();
	    aliases.add(hat.getName());
	    amendAliases(id, aliases);
      }
      return id;
}

@SessionMethod
private List<DependencyInfoDTO> collectDependencies(@NotNull PackageInfo info) throws StorageException {
      List<DependencyInfoDTO> list = new ArrayList<>();
      for (var dependency : info.getDependencies()) {
	    Optional<PackageId> packageId = getPackageId(dependency.getPackageId());
	    Optional<VersionId> versionId =
		packageId.flatMap(id -> mapVersion(id, dependency.getVersionId()));
	    var depInfo =
		versionId.flatMap(version -> getFullInfo(packageId.get(), version));
	    if (depInfo.isPresent()) {
		  list.add(new DependencyInfoDTO(dependency.getPackageId(), depInfo.get().version));
	    } else {
		  throw new StorageException("Broken dependency");
	    }
      }
      return list;
}

//Additional DataBase's entity such as Payload or License should be
@SessionMethod
private @NotNull Payload fetchPayload(@NotNull Session session, String payloadType) throws StorageException {
      session.beginTransaction();
      Query query = session.createQuery("from Payload where name= :payloadType");
      query.setParameter("payloadType", payloadType);
      Payload payload = (Payload) query.getSingleResult();
      session.getTransaction().commit();
      if (payload == null)
	    throw new StorageException("Invalid payload type");
      return payload;
}

@SessionMethod
private @NotNull Licence fetchLicense(@NotNull Session session, String type) throws StorageException {
      session.beginTransaction();
      Query query = session.createQuery("from Licence where name= :licenceType");
      query.setParameter("licenceType", type);
      Licence licence = (Licence) query.getSingleResult();
      session.getTransaction().commit();
      if (licence == null)
	    throw new StorageException("Invalid license type");
      return licence;
}

@SessionMethod
private @NotNull Archive fetchArchive(@NotNull Session session, String type) throws StorageException {
      session.beginTransaction();
      Query query = session.createQuery("from Archive where type = :type");
      query.setParameter("type", type);
      Archive archive = (Archive) query.getSingleResult();
      session.getTransaction().commit();
      if (archive == null) {
	    throw new StorageException("Invalid archive type");
      }
      return archive;
}

//todo: if PackageHat is downgraded to invalid, it means Package is deprecated and should be removed in future
@SessionMethod
private void validatePackageHat(@NotNull Session session, PackageId id, boolean isValid) {
      Optional<PackageHat> hat = getAnyPackageHat(session, id);
      hat.ifPresent(packageHat -> {
	    session.beginTransaction();
	    packageHat.setValid(true);
	    session.update(packageHat);
	    session.getTransaction().commit();
      });
}

@SessionMethod
private Optional<PackageHat> getValidPackageHat(@NotNull Session session, PackageId id) {
      session.beginTransaction();
      Query query = session.createQuery("from PackageHat where id= :packageId and valid= true");
      query.setParameter("packageId", id.value());
      Optional<PackageHat> result = query.getResultList().stream().findAny();
      session.getTransaction().commit();
      return result;
}

@SessionMethod
private Optional<PackageHat> getAnyPackageHat(@NotNull Session session, PackageId id) {
      session.beginTransaction();
      Query query = session.createQuery("from PackageHat where id= :packageId");
      query.setParameter("packageId", id.value());
      Optional<PackageHat> result = query.getResultList().stream().findAny();
      session.getTransaction().commit();
      return result;
}

@SessionMethod
private @NotNull List<PackageHat> getPackageHatAll(@NotNull Session session) {
      session.beginTransaction();
      Query query = session.createQuery("from PackageHat");
      query.setFirstResult(0);
      List<PackageHat> hats = query.getResultList();
      session.getTransaction().commit();
      return hats;
}

@SessionMethod
private Optional<PackageInfo> getInstanceInfo(@NotNull Session session, PackageId id, VersionId version) {
      Optional<PackageInfo> info;
      session.beginTransaction();
      Query query = session.createQuery("from PackageInfo where id= :instanceId");
      query.setParameter("instanceId", InstanceId.valueOf(id.value(), version.value()));
      info = query.getResultList().stream().findAny();
      session.getTransaction().commit();
      return info;
}

@SessionMethod
private Optional<AuthorInfo> getAuthorInfo(@NotNull Session session, @NotNull Author author) {
      session.beginTransaction();
      Query query = session.createQuery("from AuthorInfo where name= :name");
      query.setParameter("name", author.name());
      Optional<AuthorInfo> localAuthor = query.getResultList().stream().findAny();
      session.getTransaction().commit();
      return localAuthor;
}


//todo: limit request to instances with specific count
@SessionMethod
private @NotNull List<PackageInfo> getInstanceInfoAll(@NotNull Session session, PackageId id) {
      List<PackageInfo> family;
      session.beginTransaction();
      Query query = session.createQuery("from PackageInfo where id= :familyId");
      query.setParameter("familyId", id.value());
      family = query.getResultList();
      session.getTransaction().commit();
      return family;
}

private static @NotNull String getHashCode(String token, byte[] salt) {
      byte[] bytes;
      try {
	    MessageDigest md = MessageDigest.getInstance("SHA-256");
	    md.update(salt);
	    bytes = md.digest(token.getBytes());
	    md.reset();
      } catch (NoSuchAlgorithmException e) {
	    //unreachable case...
	    throw new IllegalStateException("Internal error");
      }
      Base64.Encoder encoder = Base64.getEncoder();
      return encoder.encodeToString(bytes);
}

private static @NotNull byte[] getSalt() {
      byte[] salt = new byte[16];
      try {
	    SecureRandom random = SecureRandom.getInstance("SHA1PRNG"); //replace to DRBG
	    random.nextBytes(salt);
      } catch (NoSuchAlgorithmException e) {
	    throw new RuntimeException(e);
      }
      return salt;
}

private static final String DEFAULT_LICENSE = "GNU";
private static final String DEFAULT_CONFIG_PATH = "server.conf";
private static final Encryptor.Encryption DEFAULT_ENCRYPTION = Encryptor.Encryption.Rsa;
private LocalConfig config;
private KeyPair signPair;
private Long lastTimerUpdate;
private final Timer signTimer;
private void init() {
      dbFactory = new Configuration().configure().buildSessionFactory();
      config = LocalConfig.load(DEFAULT_CONFIG_PATH);
      signTimer.schedule(new KeyGenTimerTask(), 0L, config.getTimeout() * 1000);
}
private PublicKey getPublicSign() {
      synchronized (signTimer) {
	    return signPair.getPublic();
      }
}
private Long getLastTimerUpdate() {
      synchronized (signTimer) {
	    return lastTimerUpdate;
      }
}
private class KeyGenTimerTask extends TimerTask {
      @Override
      public void run() {
	    synchronized (signTimer) {
		  signPair = Encryptor.generatePair(DEFAULT_ENCRYPTION);
		  lastTimerUpdate = Clock.systemUTC().millis();
		  DEFAULT_ENCRYPTION.releaseKey();
	    }
      }
}

//@SuppressWarnings("unchecked")
private void initNameMapper() {
      nameMapper = new NameMapper();
      try (Session session = dbFactory.openSession()) {
	    List<PackageHat> hats = getPackageHatAll(session);
	    for (PackageHat hat : hats) {
		  PackageId id = PackageId.valueOf(hat.getId());
		  //don't check any addition because database is supposed to be correct
		  nameMapper.add(id, hat.getName());
		  nameMapper.addAll(id, hat.getAliases());
	    }
      }
}
}
