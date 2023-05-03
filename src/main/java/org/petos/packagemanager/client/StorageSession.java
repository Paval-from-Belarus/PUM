package org.petos.packagemanager.client;

import static org.petos.packagemanager.client.storage.PackageStorage.*;

public interface StorageSession extends AutoCloseable {
enum CommitState {Failed, Success}
void commit(CommitState state) throws PackageIntegrityException;
@Override
default void close() throws PackageIntegrityException{
      commit(CommitState.Failed);
}
}
