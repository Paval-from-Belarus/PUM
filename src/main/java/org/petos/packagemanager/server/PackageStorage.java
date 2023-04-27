package org.petos.packagemanager.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

Logger logger = LogManager.getLogger(PackageStorage.class);
//not will be replaced by database
//this exactly convert
//it's supposed to be suitable to have hashtable
//conversation String to PackageId is the most frequently (I believe) operation
//always in coherent state
private ConcurrentHashMap<String, PackageId> nameMapper; //upper functionality

//todo: replace hashmaps by databases
public PackageStorage() {
      init();
      initNameMapper();
}

private SessionFactory dbFactory;

private void init() {
      dbFactory = new Configuration().configure().buildSessionFactory();
      Session session = dbFactory.openSession();
      session.beginTransaction();
      session.getTransaction().commit();
}

public void close() {
      dbFactory.close();
}

//@SuppressWarnings("unchecked")
private void initNameMapper() {
      nameMapper = new ConcurrentHashMap<>();
      List<PackageHat> hats = getPackageHatAll();
      for (PackageHat hat : hats) {
	    PackageId id = PackageId.valueOf(hat.getId());
	    nameMapper.put(hat.getName(), id);
	    hat.getAliases()
		.forEach(alias -> nameMapper.put(alias, id));
      }
}

public List<ShortPackageInfoDTO> shortInfoList() {
      List<PackageHat> hats = getPackageHatAll();
      return hats.parallelStream()
		 .map(ShortPackageInfoDTO::valueOf)
		 .toList();
}

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
      var id = nameMapper.get(aliasName);
      return Optional.ofNullable(id);
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


//check package on uniqueness and store in database
//return package
public synchronized @NotNull PackageId storePackageInfo(ShortPackageInfoDTO info) throws StorageException {
      checkPackageUniqueness(info);//methods throw exception if something wrong
      var id = initPackageHat(info);
      List<String> aliases = Arrays.asList(info.aliases);
      addAliases(id, aliases);
      appendCommonInfo(id, info);
      return id;

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

private synchronized void addAliases(PackageId id, @NotNull List<String> aliases) throws StorageException {
      boolean isUnique = true;
      for (String alias : aliases) {
	    if (nameMapper.get(alias) != null)
		  break;
      }
      if (!isUnique)
	    throw new StorageException("Alias is already used");
      List<PackageAlias> aliasList = aliases.stream()
					 .map(raw -> new PackageAlias(id.value(), raw))
					 .toList();
      Session session = dbFactory.openSession();
      for (var aliasEntity : aliasList)
	    session.save(aliasEntity);
      session.getTransaction().commit();
}

private void appendCommonInfo(PackageId id, FullPackageInfoDTO dto) throws StorageException {
      var license = getLicence(dto.licenseType);
      String version = dto.version == null ? "0.0.1" : dto.version;
      if (license.isPresent()) {
	    PackageInfo info = new PackageInfo();
	    info.setPackageId(id.value());
	    info.setVersionLabel(version);
	    info.setLicence(license.get());
	    var versionId = nextVersionId(id);
	    Session session = dbFactory.openSession();
	    info.setVersionId(versionId.value());
	    session.save(info);
	    session.getTransaction().commit();
      } else {
	    throw new StorageException("Licence type is not correct");
      }
}

private @NotNull VersionId nextVersionId(PackageId id) throws StorageException {
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from PackageInfo where packageId= :id order by time desc");
      query.setParameter("id", id);
      query.setMaxResults(1);
      List<PackageInfo> infoList = query.getResultList();
      session.getTransaction().commit();
      VersionId version;
      if(!infoList.isEmpty()){
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
 * */
private @NotNull PackageId initPackageHat(ShortPackageInfoDTO dto) throws StorageException {
      Payload payload = fetchPayload(dto.payloadType()); //throws StorageException
      PackageHat hat = PackageHat.valueOf(dto.name(), dto.aliases());
      hat.setPayload(payload);
      Session session = dbFactory.openSession();
      session.save(hat);
      session.getTransaction().commit();

      Optional<PackageId> optional = getPackageId(hat.getName());
      PackageId id = null;
      if (optional.isPresent()) {
	    id = optional.get();
	    //todo: thinking about ACID in db
	    removeOldestVersion(id);
      } else {
	    String log = String.format("Package hat is not saved: %s", hat.getName());
	    logger.error(log);
	    throw new StorageException("Impossible to save package hat " + hat.getName());
      }
      return id;
}

private List<PackageHat> getPackageHatAll() {
      Session session = dbFactory.openSession();
      session.beginTransaction();
      Query query = session.createQuery("from PackageHat");
      query.setFirstResult(0);
      List<PackageHat> hats = query.getResultList();
      session.getTransaction().commit();
      return hats;
}

private @NotNull Payload fetchPayload(String payloadType) throws StorageException {
      Payload payload;
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from Payload where name= :payloadType");
      query.setParameter("payloadType", payloadType);
      payload = (Payload) query.getSingleResult();
      session.getTransaction().commit();
      if(payload == null)
	    throw new StorageException("Invalid payload type");
      return payload;
}

private Optional<Licence> getLicence(String type) {
      Licence licence;
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from Licence where name= :licenceType");
      query.setParameter("licenceType", type);
      licence = (Licence) query.getSingleResult();
      session.getTransaction().commit();
      return Optional.ofNullable(licence);
}

private Optional<PackageHat> getPackageHat(PackageId id) {
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from PackageHat where id= :packageId");
      query.setParameter("packageId", id.value());
      PackageHat result = (PackageHat) query.getSingleResult();
      session.getTransaction().commit();
      return Optional.ofNullable(result);
}

private Optional<PackageInfo> getInstanceInfo(PackageId id, VersionId version) {
      Session session = dbFactory.openSession();
      Query query = session.createQuery("from PackageInfo where id= :instanceId");
      query.setParameter("instanceId", InstanceId.valueOf(id.value(), version.value()));
      PackageInfo info = (PackageInfo) query.getSingleResult();
      session.getTransaction().commit();
      return Optional.ofNullable(info);
}
}
