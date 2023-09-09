package storage;

import database.InstanceInfo;
import org.petos.pum.networks.dto.FullPackageInfoDTO;
import org.petos.pum.networks.transfer.PackageAssembly;
import org.petos.pum.networks.dto.VersionInfoDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static storage.JournalTransaction.Type;
import static storage.PackageStorage.*;

public class ModifierSession extends AbstractSession {
private final static int DUMMY_PACKAGE_ID = 0;

@FunctionalInterface
public interface VersionPredicate {
      boolean check(VersionInfoDTO old, VersionInfoDTO last);
}

public enum Rank {None, Silent, Jumping}

public enum Mode {Downgrade, Upgrade}

public static Rank compare(VersionInfoDTO old, VersionInfoDTO last) {
      Rank rank = Rank.Jumping;
      if (old.getLabel().equals(last.getLabel())) {
	    rank = Rank.Silent;
	    if (old.getVersionId().equals(last.getVersionId())) {
		  rank = Rank.None;
	    }
      }
      return rank;
}

public InstallerSession getInstaller() {
      assert getJournalPath() != null && cacheDirectory != null;
      ProxyInstaller session = new ProxyInstaller(storage, cacheDirectory);
      session.setJournalPath(getJournalPath());
      proxyInstaller = session;
      return session;
}

public RemovableSession getRemover() {
      assert getJournalPath() != null;
      ProxyRemover session = new ProxyRemover(storage);
      session.setJournalPath(getJournalPath());
      proxyRemover = session;
      return session;
}


ModifierSession(PackageStorage storage) {
      this.storage = storage;
}

@Override
public void commit(CommitState state) throws PackageIntegrityException {
      if (isManaged())
	    return;
      try {
	    if (state == CommitState.Success) {
		  List<JournalTransaction> installed = getTransactions(Type.Install);
		  List<InstanceInfo> removables = getTransactions(Type.Remove).stream()
						      .map(JournalTransaction::getInstance)
						      .collect(Collectors.toList());
		  for (var atom : removables) {
			PackageStorage.removeFiles(Path.of(atom.getStringPath()));
		  }
		  storage.rebuildConfig(removables, RebuildMode.Remove);
		  eraseJournal();
		  for (var atom : installed) {
			proxyInstaller.replace(Path.of(atom.getStringPath()));
		  }
		  proxyInstaller.forceCommit();
	    } else {
		  if (proxyInstaller != null) {
			proxyInstaller.commit(CommitState.Failed);
		  }
		  if (proxyRemover != null) {
			proxyRemover.commit(CommitState.Failed);
		  }
		  deleteJournal(); //removeFil
	    }
	    setManaged(true);
      } catch (PackageAssembly.VerificationException e) {
	    throw new PackageIntegrityException(e);
      } catch (IOException e) {
	    throw new RuntimeException(e);
      }

}

void setCacheDirectory(Path directory) {
      assert Files.isDirectory(directory);
      cacheDirectory = directory;
}

private Path cacheDirectory;
private ProxyRemover proxyRemover;
private ProxyInstaller proxyInstaller;
private final PackageStorage storage;

private static Path resolveAssembly(Path dir) {
      return dir.resolve("assembly.bin");
}

private static Path resolveInfo(Path dir) {
      return dir.resolve("info.pum");
}

private static class ProxyInstaller extends InstallerSession {
      private final Path programs;

      ProxyInstaller(PackageStorage storage, Path programs) {
	    super(storage);
	    this.programs = programs;
      }

      private void forceCommit() throws PackageIntegrityException {
	    super.commit(CommitState.Success);
      }

      private void replace(Path packageDir) throws PackageIntegrityException, PackageAssembly.VerificationException {
	    String stringInfo;
	    try {
		  stringInfo = Files.readString(resolveInfo(packageDir));
		  byte[] assembly = Files.readAllBytes(resolveAssembly(packageDir));
		  var dto = PackageStorage.fromJson(stringInfo, FullPackageInfoDTO.class);
		  if (dto != null) {
			super.storeLocally(dto, assembly);
		  } else {
			throw new PackageIntegrityException("The temp package info is damaged");
		  }
	    } catch (IOException e) {
		  //something go wrong
		  throw new RuntimeException(e);
	    }
      }

      @Override
      public void storeLocally(FullPackageInfoDTO dto, byte[] assembly) throws PackageIntegrityException {
	    PayloadType payload = PackageStorage.convert(dto.payloadType);
	    if (payload == PayloadType.Unknown)
		  throw new PackageIntegrityException("Invalid payload type");
	    try {
		  Path cacheDir = programs.resolve(dto.name);
		  if (Files.exists(cacheDir))
			removeFiles(cacheDir);
		  Files.createDirectory(cacheDir);
		  Files.write(resolveAssembly(cacheDir), assembly);
		  Files.writeString(resolveInfo(cacheDir), PackageStorage.toJson(dto));
		  appendJournal(DUMMY_PACKAGE_ID, cacheDir, dto);
	    } catch (IOException e) {
		  throw new RuntimeException(e);
	    }
      }

      @Override
      public void commit(CommitState state) {
      }

}

private static class ProxyRemover extends RemovableSession {

      ProxyRemover(PackageStorage storage) {
	    super(storage);
      }

      @Override
      void deleteJournal() {
      }

      @Override
      public void commit(CommitState state) throws PackageIntegrityException {
	    if (state == CommitState.Failed)
		  super.commit(state); //restore
      }
}

}
