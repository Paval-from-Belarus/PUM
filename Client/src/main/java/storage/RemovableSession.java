package storage;

import database.InstanceInfo;
import dto.FullPackageInfoDTO;
import storage.JournalTransaction.Type;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static storage.PackageStorage.*;


public class RemovableSession extends AbstractSession {
private Path centralPath = null;

public void removeLocally(FullPackageInfoDTO dto) throws PackageIntegrityException {
      Optional<InstanceInfo> instance = storage.getInstanceInfo(dto.name);
      try {
	    if (instance.isPresent() && storage.getInstanceState(instance.get()).isRemovable()) {
		  storage.unlinkLibraries(instance.get());
		  appendJournal(Type.Remove, instance.get());
		  if (PackageStorage.convert(dto.payloadType) == PayloadType.Application) {
			assert centralPath == null;//only single application per session
			centralPath = Path.of(instance.get().getStringPath());
		  }
	    } else {
		  throw new PackageIntegrityException("Non-removable package");
	    }
      } catch (IOException e) {
	    //really funny case
	    throw new PackageIntegrityException("Config file is not accessible");
      }
}

/**
 * The method commit remove actions. By another words, before this moment, no file (packages) were really removed.
 * But only marked as removable (and unlinkable from PackageStorage)
 */
@Override
public void commit(CommitState state) throws PackageIntegrityException {
      if (isManaged()) {
	    return;
      }
      try {
	    List<InstanceInfo> instances = getTransactions(Type.Remove).stream()
					       .map(JournalTransaction::getInstance)
					       .collect(Collectors.toList());
	    if (state != CommitState.Failed) {
		  for (var instance : instances) {
			removeFiles(Path.of(instance.getStringPath()));
		  }
		  storage.rebuildConfig(instances, RebuildMode.Remove);
	    } else {
		  List<InstanceInfo> dependencies = instances.stream()
							.filter(info -> !info.getStringPath().equals(centralPath.toString()))
							.collect(Collectors.toList());
		  storage.linkLibraries(centralPath, dependencies);
	    }
	    deleteJournal();
	    setManaged(true);
      } catch (IOException e) {
	    throw new PackageIntegrityException("Some packages cannot be removed");
      }
}

RemovableSession(PackageStorage storage) {
      this.storage = storage;
      setManaged(false);
}

private final PackageStorage storage;
}
