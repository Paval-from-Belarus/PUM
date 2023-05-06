package storage;


public interface StorageSession extends AutoCloseable {
enum CommitState {Failed, Success}
void commit(CommitState state) throws PackageStorage.PackageIntegrityException;
@Override
default void close() throws PackageStorage.PackageIntegrityException {
      commit(CommitState.Failed);
}
}
