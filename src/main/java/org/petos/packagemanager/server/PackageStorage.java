package org.petos.packagemanager.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.NotFound;
import org.hibernate.cfg.Configuration;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.database.*;
import org.petos.packagemanager.packages.FullPackageInfoDTO;
import org.petos.packagemanager.packages.ShortPackageInfoDTO;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
      List<PackageHat> hats = getPackageHatAll();
      return hats.parallelStream()
		 .map(ShortPackageInfoDTO::valueOf)
		 .toList();
}

//The important assumption is that all database check is unsafe (doesn't check anything)
//Because all should be checked before addition
private void amendAliases(PackageId id, @NotNull Collection<String> aliases) throws StorageException {
      //checkUnique
      if (!nameMapper.addAll(id, aliases)) //it's realy easy to check in memory aliases
	    //comparing with addition in DataBase
	    throw new StorageException("Aliases already defined");
}

//todo: review implementation
private void checkPackageUniqueness(ShortPackageInfoDTO info) throws StorageException {
      logger.info("Attempt to check info uniqueness");
      var id = getPackageId(info.name());
      if (id.isPresent()) {
	    logger.warn("Uniqueness check is failed");
	    throw new StorageException("Package name is busy");
      }
      for (String alias : info.aliases()) {
	    id = getPackageId(alias);
	    if (id.isPresent()) {
		  logger.warn("Uniqueness check is failed");
		  throw new StorageException("Package alias is busy");
	    }
      }
      logger.info("Uniqueness check is passed");
}

public Optional<PackageId> getPackageId(String aliasName) {
      return nameMapper.get(aliasName);
}

/**
 * Used to convert public value to PackageId instance
 */
public Optional<PackageId> getPackageId(int value) {
      PackageId id = PackageId.valueOf(value);
      var optional = getPackageHat(id);
      if (optional.isEmpty())
	    id = null;
      return Optional.ofNullable(id);
}

/**
 * Convert versionOffset to unique versionId
 * Generally, each certain package is determined by packageId and versionId. In case, if current version offset is not exists
 * return last available package
 *
 * @param versionOffset is client view of version<br> See getDataPackage to determine how it works
 * @param id            is unique packageId
 * @return unique versionId
 */
public VersionId mapVersion(PackageId id, int versionOffset) {
      List<PackageInfo> infoList;
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from PackageInfo where packageId= :familyId");
      query.setParameter("familyId", id.value());
      infoList = query.getResultList();
      session.getTransaction().commit();
      int maxOffset = infoList.size() - 1;
      assert maxOffset >= 0;
      if (versionOffset != -1) {
	    versionOffset = Math.max(versionOffset, 0); //not negative
	    versionOffset = Math.min(versionOffset, maxOffset); //any correct offset
      } else {
	    versionOffset = maxOffset;
      }
      int version = infoList.get(versionOffset).getVersionId();
      return VersionId.valueOf(version);
}
//assume: aliases are unique


/**
 * @return sorted DataPackage according time that had been added <br>
 * More fresh Data stores closer to first item. <br>
 * IE. The first item is most fresh
 */
//todo: access to table with short info
public Optional<ShortPackageInfoDTO> getShortInfo(PackageId id) {
      var optional = getPackageHat(id);
      ShortPackageInfoDTO result = null;
      if (optional.isPresent()) {
	    result = ShortPackageInfoDTO.valueOf(optional.get());
      }
      return Optional.ofNullable(result);
}

/**
 * if version == 0: get latest version<br>
 * if version == -1: get oldest version
 * elsif version in [1,9]: get specific latter version<br>
 * else: get minimal available version<br>
 */
public Optional<FullPackageInfoDTO> getFullInfo(@NotNull PackageId id, @NotNull VersionId version) {
      var optionalInfo = getInstanceInfo(id, version);
      var optionalHat = getPackageHat(id);
      FullPackageInfoDTO dto = null;
      if (optionalHat.isPresent() && optionalInfo.isPresent()) {
	    PackageHat hat = optionalHat.get();
	    PackageInfo info = optionalInfo.get();
	    try {
		  dto = new FullPackageInfoDTO();
		  dto.version = info.getVersionLabel();
		  dto.aliases = hat.getAliases().toArray(new String[0]);
		  dto.name = hat.getName();
		  dto.licenseType = info.getLicence().getName();
		  dto.payloadType = hat.getPayload().getName();
		  dto.payloadSize = (int) Files.size(Path.of(info.getPayloadPath()));
		  //todo: add dependencies to dto
		  //todo: exhause max file size to long
	    } catch (IOException e) {
		  String log = String.format("Inaccessible full info by PackageId and VersionId: %d ; %d",
		      id.value(), version.value());
		  logger.warn(log);
	    }
      }
      return Optional.ofNullable(dto);
}


/**
 * The terrible thing is that memory can be finished by a many simultaneously connection
 * Should be replaced by OutputStream
 */
public Optional<byte[]> getPayload(PackageId id, VersionId version) {
      var optional = getInstanceInfo(id, version);
      byte[] bytes = null;
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
      return Optional.ofNullable(bytes);
}

//check package on uniqueness and store in database
//return package
public synchronized @NotNull PackageId storePackageInfo(ShortPackageInfoDTO info) throws StorageException {
      checkPackageUniqueness(info);//methods throw exception if something wrong
      return initPackageHat(info);
}

/**
 * Automatically remove old package and replace it by new payload
 *
 * @throws IllegalArgumentException if packageFamily by PackageId is not exists
 */
public void storePayload(PackageId id, byte[] payload) {
//      if (packagesMap.get(id) == null)
//	    throw new IllegalArgumentException("Package id is not exists");
      //todo: store in random place of FileSystem package's payload
}

private @NotNull VersionId nextVersionId(PackageId id) throws StorageException {
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from PackageInfo where packageId= :id order by time desc");
      query.setParameter("id", id);
      query.setMaxResults(1);
      List<PackageInfo> infoList = query.getResultList();
      session.getTransaction().commit();
      VersionId version;
      if (!infoList.isEmpty()) {
	    var info = infoList.get(0);
	    version = VersionId.valueOf(info.getVersionId() + 1);//there should be trigger in database
      } else {
	    logger.error("No existent packageInfo");
	    throw new StorageException("Package family not exists");
      }
      return version;
}

private void removeOldestVersion(PackageId id) {
      Session session = dbFactory.openSession();
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
      var instanceId = InstanceId.valueOf(id.value(), version.value());
      PackageInfo info = session.get(PackageInfo.class, instanceId);
      session.remove(info);
      session.getTransaction().commit();
      new Thread(() -> {
	    File file = new File(info.getPayloadPath());
	    deletePayload(file);
      }).start();
}

private void removePackageAll(PackageId id) {
      var optionalHat = getPackageHat(id);
      if (optionalHat.isPresent()) {
	    Session session = dbFactory.openSession();
	    session.getTransaction().commit();
      } else {
	    logger.warn("Attempt to remove not existent package");
      }
}

/**
 * Assume that all checks are passed
 * To add this dto to database is safe
 */
private @NotNull PackageId initPackageHat(@NotNull ShortPackageInfoDTO dto) throws StorageException {
      Payload payload = fetchPayload(dto.payloadType()); //throws StorageException
      PackageHat hat = PackageHat.valueOf(dto.name(), dto.aliases());
      hat.setPayload(payload);
      Session session = dbFactory.openSession();
      session.save(hat);
      Query query = session.createQuery("from PackageHat where name= :packageName");
      query.setParameter("packageName", hat.getName());
      hat = (PackageHat) query.getSingleResult(); //replace hat with database entity
      session.getTransaction().commit();
      PackageId id;
      if (hat != null) {
	    id = PackageId.valueOf(hat.getId());
	    Collection<String> aliases = hat.getAliases();
	    aliases.add(hat.getName());
	    amendAliases(id, aliases);
      } else {
	    String log = String.format("Package hat is not saved: %s", dto.name());
	    logger.error(log);
	    throw new StorageException("Impossible to save package hat " + hat.getName());
      }
      return id;
}


//Additional DataBase's entity such as Payload or License should be
private @NotNull Payload fetchPayload(String payloadType) throws StorageException {
      Payload payload;
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from Payload where name= :payloadType");
      query.setParameter("payloadType", payloadType);
      payload = (Payload) query.getSingleResult();
      session.getTransaction().commit();
      if (payload == null)
	    throw new StorageException("Invalid payload type");
      return payload;
}

private @NotNull Licence fetchLicense(String type) throws StorageException {
      Licence licence;
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from Licence where name= :licenceType");
      query.setParameter("licenceType", type);
      licence = (Licence) query.getSingleResult();
      session.getTransaction().commit();
      if (licence == null)
	    throw new StorageException("Invalid license type");
      return licence;
}

private Optional<PackageHat> getPackageHat(PackageId id) {
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from PackageHat where id= :packageId");
      query.setParameter("packageId", id.value());
      PackageHat result = (PackageHat) query.getSingleResult();
      session.getTransaction().commit();
      return Optional.ofNullable(result);
}

private @NotNull List<PackageHat> getPackageHatAll() {
      Session session = dbFactory.openSession();
      session.beginTransaction();
      Query query = session.createQuery("from PackageHat");
      query.setFirstResult(0);
      List<PackageHat> hats = query.getResultList();
      session.getTransaction().commit();
      return hats;
}

private Optional<PackageInfo> getInstanceInfo(PackageId id, VersionId version) {
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from PackageInfo where id= :instanceId");
      query.setParameter("instanceId", InstanceId.valueOf(id.value(), version.value()));
      PackageInfo info = (PackageInfo) query.getSingleResult();
      session.getTransaction().commit();
      return Optional.ofNullable(info);
}

//todo: limit request to instances with specific count
private @NotNull List<PackageInfo> getInstanceInfoAll(PackageId id) {
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from PackageInfo where id= :familyId");
      query.setParameter("familyId", id.value());
      List<PackageInfo> family = query.getResultList();
      session.getTransaction().commit();
      return family;
}

private void init() {
      dbFactory = new Configuration().configure().buildSessionFactory();
      Session session = dbFactory.openSession();
      session.beginTransaction();
      session.getTransaction().commit();
}

//@SuppressWarnings("unchecked")
private void initNameMapper() {
      nameMapper = new NameMapper();
      List<PackageHat> hats = getPackageHatAll();
      for (PackageHat hat : hats) {
	    PackageId id = PackageId.valueOf(hat.getId());
	    //don't check any addition because database is supposed to be correct
	    nameMapper.add(id, hat.getName());
	    nameMapper.addAll(id, hat.getAliases());
      }
}
}
