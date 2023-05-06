package storage;

import database.InstanceInfo;
import packages.FullPackageInfoDTO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import static storage.PackageStorage.*;


public class RemovableSession implements StorageSession {
private Path centralPath; //the path of main package
private boolean isManaged;
public void removeLocally(FullPackageInfoDTO dto) throws PackageIntegrityException {
      Optional<InstanceInfo> instance = storage.getInstanceInfo(dto.name);
      try {
            if (instance.isPresent() && storage.getInstanceState(instance.get()).isRemovable()){
                  storage.unlinkLibraries(instance.get());
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
                        removeFiles(path);
                  }
                  storage.rebuildConfig(list, RebuildMode.Remove);
            }
            if (!isManaged)
                  clearConfig();
      } catch(IOException e){
            throw new PackageIntegrityException("Some packages cannot be removed");
      }
}
private void clearConfig(){
      try{
            isManaged = true;
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
      this.isManaged = false;
}
private final PackageStorage storage;
private File configFile;
}
