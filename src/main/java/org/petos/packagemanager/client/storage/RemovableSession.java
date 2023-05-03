package org.petos.packagemanager.client.storage;

import org.petos.packagemanager.client.StorageSession;
import org.petos.packagemanager.client.database.InstanceInfo;
import org.petos.packagemanager.packages.FullPackageInfoDTO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class RemovableSession implements StorageSession {
public void removeLocally(FullPackageInfoDTO dto) throws PackageStorage.PackageIntegrityException {
      Optional<InstanceInfo> instance = storage.getInstanceInfo(dto.name);
      if (instance.isPresent()){
            Path central = Path.of(instance.get().getStringPath());

      }
}

@Override
public void commit(CommitState state) throws PackageStorage.PackageIntegrityException {

}

@Override
public void close() throws PackageStorage.PackageIntegrityException {
      StorageSession.super.close();
}
private void appendConfig(InstanceInfo removable){
}
RemovableSession setConfig(File configFile){
      this.configFile = configFile;
      return this;
}
RemovableSession(PackageStorage storage) {
      this.storage = storage;
}
private final PackageStorage storage;
private File configFile;
}
