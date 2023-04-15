package org.petos.packagemanager.server;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.petos.packagemanager.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PackageStorage {
Logger logger = LogManager.getLogger(PackageStorage.class);
//not will be replaced by database
//this exactly convert
//in future all hashmaps will be replaced by database
private ConcurrentHashMap<String, PackageId> nameMapper; //upper functionality
private ConcurrentHashMap<PackageId, List<DataPackage>> packagesMap;
//todo: replace hashmaps by databases
public PackageStorage() {
      initPackages();
      initNameMapper();
}
private void initNameMapper() {
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

private void initPackages() {
      packagesMap = new ConcurrentHashMap<>();
      try{
	    String content = Files.readString(Path.of("packages.json"));
	    PackageInfo[] packages = new Gson().fromJson(content, PackageInfo[].class);
	    AtomicInteger counter = new AtomicInteger(0);
	    Arrays.stream(packages)
		.forEach(info -> {
		      var id = PackageId.valueOf(counter.get());
		      counter.incrementAndGet();
		      var packageFamily = packagesMap.getOrDefault(id, List.of());
		      var data = new DataPackage(info, "");
		      packageFamily.add(data);
		});
      } catch(IOException e){
	    logger.error("packages.json file is not found or impossible to read");
	    throw new RuntimeException(e);
      }
}
public List<PackageId> keyList(){
      return packagesMap.keySet().stream().toList();
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
      if(packageFamily != null){
	    result = Optional.of(ShortPackageInfo.valueOf(packageFamily.get(0).info));
      }
      return result;
}


public Optional<PackageId> getPackageId(String aliasName) {
	var id = nameMapper.get(aliasName);
	Optional<PackageId> result = Optional.empty();
	if(id != null){
	      result = Optional.of(id);
	}
	return result;
}

private Optional<DataPackage> getDataPackage(PackageId id, int versionId){
      //The main assumption that entries in data base has ordered by time adding
      var packageFamily = packagesMap.get(id);
      Optional<DataPackage> result = Optional.empty();
      if(packageFamily != null){
	    switch(versionId){
		  case 0 -> result = Optional.of(packageFamily.get(0));//latest version
		  case -1 -> result = Optional.of(packageFamily.get(packageFamily.size() - 1));
		  default -> {
			int maxOffset = Math.min(packageFamily.size() - 1, versionId);
			result = Optional.of(packageFamily.get(maxOffset));

		  }
	    }
      }
      return result;
}
/**
 * if version == 0: get latest version<br>
 * if version == -1: get oldest version
 * elsif version in [1,9]: get specific latter version<br>
 * else: get minimal available version<br>
 */
public Optional<PackageInfo> getFullInfo(PackageId id, int versionId) {
      var data = getDataPackage(id, versionId);
      Optional<PackageInfo> result = Optional.empty();
      if(data.isPresent())
	    result = Optional.of(data.get().info);
      return result;
}
/**
 * The terrible thing is that memory can be finished by a many simultaneously connection
 * Should be replaced by OutputStream
 */
private Optional<byte[]> getPayload(PackageId id, int versionId){
      var data = getDataPackage(id, versionId);
      byte[] payload = null;
      if(data.isPresent()){
	    File file = data.get().payloadPath.toFile();
	    try{
		  if(file.exists() && file.canRead()){
			payload = Files.readAllBytes(data.get().payloadPath);

		  }
	    } catch (IOException e) {
		  logger.error("Impossible to read package payload: " + e.getMessage());
	    }
      }
      return Optional.ofNullable(payload);
}
}
