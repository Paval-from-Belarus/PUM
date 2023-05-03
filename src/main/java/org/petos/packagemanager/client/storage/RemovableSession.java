package org.petos.packagemanager.client.storage;

import org.petos.packagemanager.client.StorageSession;
import org.petos.packagemanager.client.database.InstanceInfo;
import org.petos.packagemanager.packages.FullPackageInfoDTO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import static org.petos.packagemanager.client.storage.PackageStorage.*;

public class RemovableSession implements StorageSession {
private Path centralPath; //the path of main package
public void removeLocally(FullPackageInfoDTO dto) throws PackageIntegrityException {
      Optional<InstanceInfo> instance = storage.getInstanceInfo(dto.name);
      try {
            if (instance.isPresent() && storage.getInstanceState(instance.get()).isRemovable()){
                  appendConfig(instance.get());
            } else {
                  throw new PackageIntegrityException("Non-removable package");
            }
      } catch (IOException e){
            //really funny case
            throw new PackageIntegrityException("Config file is not accessible");
      }
}

@Override
public void commit(CommitState state) throws PackageIntegrityException {
      try {
            if (state != CommitState.Failed){
                  String content = Files.readString(configFile.toPath());
                  List<InstanceInfo> list = InstanceInfo.valueOf(content);
                  for (var info : list){
                        Path path = Path.of(info.getStringPath());
                        PackageStorage.removeFiles(path);
                  }
                  storage.rebuildConfig(list, RebuildMode.Remove);
            }
            clearConfig();
      } catch(IOException e){
            throw new PackageIntegrityException("Some packages cannot be removed");
      }
}
private void clearConfig(){
      try{
            Files.deleteIfExists(configFile.toPath());
      } catch (IOException ignored){

      }
}
private void appendConfig(InstanceInfo removable) throws IOException {
      Files.writeString(configFile.toPath(), removable.toString(), StandardOpenOption.APPEND);
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
