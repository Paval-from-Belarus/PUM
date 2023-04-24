package org.petos.packagemanager.server;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.database.PackageHat;
import org.petos.packagemanager.packages.DataPackage;
import org.petos.packagemanager.packages.PackageInfo;
import org.petos.packagemanager.packages.ShortPackageInfo;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PackageStorage {

public static class PackageId {
      private final int value;

      private PackageId(Integer value) {
	    this.value = value;
      }

      public Integer value() {
	    return value;
      }

      private static PackageId valueOf(Integer id) {
	    return new PackageId(id);
      }
      @Override
      public boolean equals(Object other){
	    if(other instanceof PackageId)
		  return ((PackageId)other).value == value;
	    return false;
      }
      @Override
      public int hashCode(){
	    return value;
      }
}
public static class VersionId{
private final int value;

      public VersionId(int value) {
	    this.value = value;
      }
      public Integer value(){
	    return value;
      }
      private static VersionId valueOf(Integer id){
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
//in future all hashmaps will be replaced by database
private ConcurrentHashMap<String, PackageId> nameMapper; //upper functionality
private ConcurrentHashMap<PackageId, List<DataPackage>> packagesMap;

//todo: replace hashmaps by databases
public PackageStorage() {
      init();
      initPackages();
      initNameMapper();
}
private SessionFactory dbFactory;
private void init(){
	dbFactory = new Configuration().configure().buildSessionFactory();
	Session session = dbFactory.openSession();
	session.beginTransaction();
	session.getTransaction().commit();
}
public void close(){
      dbFactory.close();
}
private static int FIRST_PACKAGE_ID = 3;//

private static PackageId nextPackageId() {
      return PackageId.valueOf(FIRST_PACKAGE_ID++);
}

//@SuppressWarnings("unchecked")
private void initNameMapper() {
      nameMapper = new ConcurrentHashMap<>();
      Session session = dbFactory.openSession();
      session.beginTransaction();
      Query query = session.createQuery("from PackageHat");
      query.setFirstResult(0);
      List<PackageHat> hats = query.getResultList();
      session.getTransaction().commit();
      for (PackageHat hat : hats){
	    PackageId id = PackageId.valueOf(hat.getId());
	    nameMapper.put(hat.getName(), id);
	    hat.getAliases()
		.forEach(alias -> nameMapper.put(alias, id));
      }
      assert packagesMap != null;
      nameMapper = new ConcurrentHashMap<>();
      for (var entry : packagesMap.entrySet()) {
	    var id = entry.getKey();
	    assert entry.getValue().size() > 0;
	    nameMapper.put(entry.getValue().get(0).info.name, id);
	    Arrays.stream(entry.getValue().get(0).info.aliases)
		.forEach(alias -> nameMapper.put(alias, id));
      }
}
private void addMapper(PackageId id, List<String> aliases) {
      boolean isUnique = true;
      for (String alias : aliases) {
	    if (nameMapper.get(alias) != null) {
		  isUnique = false;
		  break;
	    }
      }
      assert isUnique;
      for (String alias : aliases) {
	    nameMapper.put(alias, id);
      }
}
private void initPackages() {
      packagesMap = new ConcurrentHashMap<>();
      try {
	    String content = Files.readString(Path.of("packages.json"));
	    PackageInfo[] packages = new Gson().fromJson(content, PackageInfo[].class);
	    AtomicInteger counter = new AtomicInteger(0);
	    Arrays.stream(packages)
		.forEach(info -> {
		      var id = PackageId.valueOf(counter.get());
		      counter.incrementAndGet();
		      var packageFamily = packagesMap.getOrDefault(id, new ArrayList<>());
		      var data = new DataPackage(info, "");
		      packageFamily.add(data);
		      packagesMap.put(id, packageFamily);
		});
      } catch (IOException e) {
	    logger.error("packages.json file is not found or impossible to read");
	    throw new RuntimeException(e);
      }
}

public List<PackageId> keyList() {
      return packagesMap.keySet().stream().toList();
}

private void checkPackageUniqueness(PackageInfo info) throws StorageException {
      logger.info("Attempt to check info uniqueness");
      var id = getPackageId(info.name);
      if (id.isPresent()) {
	    logger.warn("Uniqueness check is failed");
	    throw new StorageException("Package name is busy");
      }
      for (String alias : info.aliases) {
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
      Optional<PackageId> result = Optional.empty();
      if (id != null) {
	    result = Optional.of(id);
      }
      return result;
}

/**
 * Used to convert public value to PackageId instance
 */
public Optional<PackageId> getPackageId(int value) {
      var id = PackageId.valueOf(value);
      Optional<PackageId> optional = Optional.empty();
      if (packagesMap.get(id) != null)
	    optional = Optional.of(id);
      return optional;
}
/**
 * Convert versionOffset to unique versionId
 * Generally, each certain package is determined by packageId and versionId. In case, if current version offset is not exists
 * return last available package
 * @param version is client view of version<br> See getDataPackage to determine how it works
 * @param id is unique packageId
 * @return unique versionId
 * */
public VersionId mapVersion(PackageId id, int version){
      int maxOffset = packagesMap.get(id).size() - 1;
      int result;
      assert maxOffset >= 0;
      switch (version) {
	    case 0 -> result = 0;
	    case -1 -> result = maxOffset;
	    default -> {
		  version = Math.max(version, 0);//not negative
		  result = Math.min(version, maxOffset);
	    }
      }
      return VersionId.valueOf(result);
}
//assume: aliases are unique


//check package on uniqueness and store in database
//return package
public @NotNull PackageId storePackage(PackageInfo info) throws StorageException {
      checkPackageUniqueness(info);
      PackageId id = nextPackageId();
      List<String> aliases = Arrays.asList(info.aliases);
      aliases.add(info.name);
      addMapper(id, aliases);
      return id;

}

/**
 * Automatically remove old package and replace it by new payload
 * @throws IllegalArgumentException if packageFamily by PackageId is not exists
 */
public void storePayload(PackageId id, byte[] payload) {
      if (packagesMap.get(id) == null)
	    throw new IllegalArgumentException("Package id is not exists");
      //todo: store in random place of FileSystem package's payload
}

/**
 * @return sorted DataPackage according time that had been added <br>
 * More fresh Data stores closer to first item. <br>
 * IE. The first item is most fresh
 */
//todo: access to table with short info
public Optional<ShortPackageInfo> getShortInfo(PackageId id) {
      Optional<ShortPackageInfo> result = Optional.empty();
      var packageFamily = packagesMap.get(id);
      if (packageFamily != null) {
	    result = Optional.of(ShortPackageInfo.valueOf(packageFamily.get(0).info));
      }
      return result;
}

/**
 * if version == 0: get latest version<br>
 * if version == -1: get oldest version
 * elsif version in [1,9]: get specific latter version<br>
 * else: get minimal available version<br>
 */
public Optional<PackageInfo> getFullInfo(PackageId id, VersionId version) {
      var data = getDataPackage(id, version);
      Optional<PackageInfo> result = Optional.empty();
      if (data.isPresent())
	    result = Optional.of(data.get().info);
      return result;
}

/**
 * The terrible thing is that memory can be finished by a many simultaneously connection
 * Should be replaced by OutputStream
 */
public Optional<byte[]> getPayload(PackageId id, VersionId versionId){
      var data = getDataPackage(id, versionId);
      byte[] payload = null;
      if (data.isPresent()) {
	    File file = data.get().payloadPath.toFile();
	    try {
		  if (file.exists() && file.canRead()) {
			payload = Files.readAllBytes(data.get().payloadPath);

		  }
	    } catch (IOException e) {
		  logger.error("Impossible to read package payload: " + e.getMessage());
	    }
      }
      return Optional.ofNullable(payload);
}
private Optional<DataPackage> getDataPackage(PackageId id, VersionId version) {
      //The main assumption that entries in database has ordered by time adding
      var packageFamily = packagesMap.get(id);
      Optional<DataPackage> result = Optional.empty();
      if (packageFamily != null) {
	    result = Optional.of(packageFamily.get(version.value()));
      }

      return result;
}
}
