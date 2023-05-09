package common;

import database.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jetbrains.annotations.NotNull;
import packages.*;
import security.Author;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

private static Logger logger = LogManager.getLogger(PackageStorage.class);
//not will be replaced by database
//this exactly convert
//it's supposed to be suitable to have hashtable
//conversation String to PackageId is the most frequently (I believe) operation
//always in coherent state
private NameMapper nameMapper; //upper functionality

public PackageStorage() {
      init();
      initNameMapper();
}

private SessionFactory dbFactory;

public void close() {
      dbFactory.close();
}

public List<ShortPackageInfoDTO> shortInfoList() {
      List<ShortPackageInfoDTO> list = new ArrayList<>();
      try (Session session = dbFactory.openSession()) {
	    List<PackageHat> hats = getPackageHatAll(session);
	    for (PackageHat hat : hats) {
		  PackageId id = PackageId.valueOf(hat.getId());
		  Optional<ShortPackageInfoDTO> info = getShortInfo(id);
		  info.ifPresent(list::add);
	    }
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
			session.beginTransaction();
			Query query = session.createQuery("from PackageHat where id= :packageId");
			query.setParameter("packageId", packageId.get().value());
			packageHat = query.getResultList().stream().findAny();
			session.getTransaction().commit();
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

private void checkInstance(AuthorId author, PackageInstanceDTO dto) throws StorageException {
      Optional<PackageId> id = getPackageId(dto.packageId());
      boolean isValidInstance = id.isPresent();
      if (isValidInstance) {
	    try (Session session = dbFactory.openSession()) {
		  session.beginTransaction();
		  Query query = session.createQuery("from PackageHat where authorId= :author");
		  query.setParameter("author", author);
		  List<PackageHat> hats = query.getResultList();
		  session.getTransaction().commit();
		  isValidInstance = (hats == null) || hats.stream().anyMatch(h -> h.getId().equals(dto.packageId()));
	    }
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
	    var optional = getPackageHat(session, id);
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
      Optional<ShortPackageInfoDTO> dto = Optional.empty();
      try (Session session = dbFactory.openSession()) {
	    Optional<PackageHat> hat = getPackageHat(session, id);
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

public Optional<FullPackageInfoDTO> getFullInfo(@NotNull PackageId id, @NotNull VersionId version) {
      FullPackageInfoDTO dto = null;
      try (Session session = dbFactory.openSession()) {
	    var optionalInfo = getInstanceInfo(session, id, version);
	    var optionalHat = getPackageHat(session, id);
	    if (optionalHat.isPresent() && optionalInfo.isPresent()) {
		  PackageHat hat = optionalHat.get();
		  PackageInfo info = optionalInfo.get();
		  dto = new FullPackageInfoDTO();
		  dto.version = info.getVersionLabel();
		  dto.aliases = hat.getAliases().toArray(new String[0]);
		  dto.name = hat.getName();
		  dto.licenseType = info.getLicence().getName();
		  dto.payloadType = hat.getPayload().getName();
		  dto.payloadSize = (int) Files.size(Path.of(info.getPayloadPath()));
		  dto.dependencies = collectDependencies(info).toArray(new DependencyInfoDTO[0]);
		  //todo: exhause max file size to Long

	    }
      } catch (StorageException | IOException e) {
	    logger.error("Broken dependency" +
			     "Id= " + id +
			     "Version=" + version);
      }
      return Optional.ofNullable(dto);
}

/**
 * The terrible thing is that memory can be finished by a many simultaneously connection
 * Should be replaced by OutputStream
 */
public Optional<byte[]> getPayload(PackageId id, VersionId version) {
      byte[] bytes = null;
      try (Session session = dbFactory.openSession()) {
	    var optional = getInstanceInfo(session, id, version);
	    if (optional.isPresent()) {
		  var info = optional.get();
		  File payload = new File(info.getPayloadPath());
		  try {
			if (payload.exists() && payload.canRead()) {
			      bytes = Files.readAllBytes(payload.toPath());
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
      try (Session session = dbFactory.openSession()) {
	    PackageHat hat = constructFrom(session, author, dto);
	    session.beginTransaction();
	    Query query = session.createQuery("from PackageHat where id= :id");
	    query.setParameter("id", id.value());
	    Optional<PackageHat> optional = query.getResultList().stream().findAny();
	    session.getTransaction().commit();
	    if (optional.isPresent()) {
		  hat.setId(optional.get().getId());//only id is differ
		  session.beginTransaction();
		  session.remove(optional.get());
		  session.save(hat);
		  session.getTransaction().commit();
	    } else {
		  throw new StorageException("PackageInfo is not exists");
	    }
      }
}

/**
 * Automatically remove old package and replace it by new payload
 *
 * @throws StorageException if packageFamily by PackageId is not exists
 */
public VersionId storePayload(@NotNull AuthorId author, @NotNull PackageInstanceDTO dto, @NotNull byte[] payload) throws StorageException {
      checkInstance(author, dto);
      //replace with simple rules
      return initPackageInfo(dto, payload);
}

//todo: here should be database trigger
private @NotNull VersionId nextVersionId(PackageId id) {
      List<PackageInfo> infoList;
      VersionId version;
      try (Session session = dbFactory.openSession()) {
	    session.beginTransaction();
	    Query query = session.createQuery("from PackageInfo where packageId= :id order by time desc");
	    query.setParameter("id", id).setMaxResults(1);
	    infoList = query.getResultList();
	    session.getTransaction().commit();
      }
      if (!infoList.isEmpty()) {
	    var info = infoList.get(0);
	    version = VersionId.valueOf(info.getVersionId() + 1);//there should be trigger in database
      } else {
	    logger.info("Initialization of PackageInfo entity");
	    version = VersionId.valueOf(0);
      }
//      removeOldestVersion(id);
      return version;
}

private @NotNull Path toPayloadPath(PackageInstanceDTO dto) throws IOException {
      return Path.of("dummy.txt");
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
	    new Thread(() -> {
		  File file = new File(info.getPayloadPath());
		  deletePayload(file);
	    }).start();
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
	    File file = new File(info.getPayloadPath());
	    deletePayload(file);
      }).start();
}

private void removePackageAll(@NotNull Session session, PackageId id) {
      var optionalHat = getPackageHat(session, id);
      if (optionalHat.isPresent()) {
	    session.beginTransaction();
	    session.getTransaction().commit();
      } else {
	    logger.warn("Attempt to remove not existent package");
      }
}

/**
 * All checks are passed
 */
private VersionId initPackageInfo(@NotNull PackageInstanceDTO dto, byte[] payload) throws StorageException {
      VersionId version;
      try (Session session = dbFactory.openSession()) {
	    Licence licence = fetchLicense(session, dto.getLicense());
	    Optional<PackageId> optionalId = getPackageId(dto.packageId());
	    if (optionalId.isPresent()) {
		  PackageId id = optionalId.get();
		  version = nextVersionId(id);
		  PackageInfo info = PackageInfo.valueOf(id.value(), version.value());
		  info.setVersionLabel(dto.version());
		  info.setLicence(licence);
		  try {
			Path path = toPayloadPath(dto);
			Files.write(path, payload);
			info.setPayloadPath(path.toString());
		  } catch (IOException e) {
			throw new StorageException("Error during file saving");
		  }
		  session.beginTransaction();
		  session.saveOrUpdate(info);
		  session.getTransaction().commit();
		  validatePackageHat(session, id, true); //now it's forbidden to change PackageHat
	    } else {
		  throw new StorageException("Invalid package id");
	    }
      }
      return version;
}

private void updatePackageHat(@NotNull PackageId id, @NotNull PublishInfoDTO dto) {

}

@SessionMethod
private PackageHat constructFrom(Session session, @NotNull AuthorId authorId, @NotNull PublishInfoDTO dto) throws StorageException {
      PackageHat hat = PackageHat.valueOf(dto.name(), dto.aliases());
      Payload payload = fetchPayload(session, dto.payloadType()); //throws StorageException
      hat.setPayload(payload);
      hat.setAuthorId(authorId.value());
      return hat;
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
	    session.save(hat);
	    session.getTransaction().commit();
	    session.beginTransaction();
	    Query query = session.createQuery("from PackageHat where name= :packageName");
	    query.setParameter("packageName", hat.getName());
	    hat = (PackageHat) query.getSingleResult(); //replace hat with database entity
	    session.getTransaction().commit();
	    if (hat != null) {
		  id = PackageId.valueOf(hat.getId());
		  Collection<String> aliases = hat.getAliases();
		  aliases.add(hat.getName());
		  amendAliases(id, aliases);
	    } else {
		  String log = String.format("Package hat is not saved: %s", dto.name());
		  logger.error(log);
		  throw new StorageException("Impossible to save package hat " + dto.name());
	    }
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

//todo: if PackageHat is downgraded to invalid, it means Package is deprecated and should be removed in future
@SessionMethod
private void validatePackageHat(@NotNull Session session, PackageId id, boolean isValid) {
      session.beginTransaction();
      Query query = session.createQuery("from PackageHat where id= :packageId");
      query.setParameter("packageId", id);
      PackageHat hat = (PackageHat) query.getSingleResult();
      hat.setValid(isValid);
      session.update(hat);
      session.getTransaction().commit();
}

@SessionMethod
private Optional<PackageHat> getPackageHat(@NotNull Session session, PackageId id) {
      session.beginTransaction();
      Query query = session.createQuery("from PackageHat where id= :packageId and valid= true");
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
	    SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
	    random.nextBytes(salt);
      } catch (NoSuchAlgorithmException e) {
	    throw new RuntimeException(e);
      }
      return salt;
}

private static final String DEFAULT_LICENSE = "GNU";

private void init() {
      dbFactory = new Configuration().configure().buildSessionFactory();
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
