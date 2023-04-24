package org.petos.packagemanager.packages;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * This class is set by Storage class. Storage class in this case is a factory of DataPackage class.
 */
public class FullPackageInfo {
Logger logger = LogManager.getLogger(FullPackageInfo.class);
public PackageInfoDTO info;
public byte[] payload;
private Integer versionId;
public FullPackageInfo(PackageInfoDTO info, String path){
      this.payloadPath = Path.of(path);
      this.info = info;
}

public Integer getVersionId() {
      return versionId;
}

/**
 * Truly, it's impossible to set directly payload
 * In case of possible performance improvement
 *
 * @param payloadPath path to payload in current FileSystem
 */
public void setPayloadPath(Path payloadPath) {
      this.payloadPath = payloadPath;
}

public void setInfoPath(Path packageInfoPath) {
      this.packageInfoPath = packageInfoPath;
}

/**
 * if result is empty there are several cases why it happens:<br>
 * If even desired resource is buse, this method is blocking (you should understand this).
 * The file is not exists in file system.
 * User of this method should notify user of Storage class to update entry.
 * Probably, you should remove corrupted entry from Storage class
 *
 * @return byte[] if PayloadPath is set correspondingly<br>
 */
public Optional<byte[]> getPayload() {
      assert payloadPath != null;
      Optional<byte[]> result = Optional.empty();
      try {
            byte[] buffer = Files.readAllBytes(payloadPath);
            result = Optional.of(buffer);
      } catch (IOException e) {
            logger.warn("Payload file error: " + e.getMessage());
      }
      return result;
}

/**
 * Description as above. You should notify storage class that Data is not correct.
 *
 * @return PackageInfo (verbose information about package)
 */
public Optional<PackageInfoDTO> getPackageInfo() {
      assert packageInfoPath != null;
      Optional<PackageInfoDTO> result = Optional.empty();
      try{
            String rawInfo = Files.readString(packageInfoPath);
            Gson gson = new Gson();
            result = Optional.of(gson.fromJson(rawInfo, PackageInfoDTO.class));
      } catch (JsonSyntaxException e){
            logger.warn("Json syntax error in " + packageInfoPath.toAbsolutePath());
      } catch (IOException e) {
            logger.warn("Payload file error: " + e.getMessage());
      }
      return result;
}


public Path payloadPath;
private Path packageInfoPath;
}
