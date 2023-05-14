package storage;

import packages.FullPackageInfoDTO;
import packages.VersionInfoDTO;

public class UpdateSession implements StorageSession {
private enum UpdateMode {Stable, Upgrade}
UpdateSession (PackageStorage storage) {

}
public void updateLocally(VersionInfoDTO dto) {

}

@Override
public void commit(CommitState state) throws PackageStorage.PackageIntegrityException {

}
}
