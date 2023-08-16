package storage;

import database.InstanceInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractSession implements StorageSession {
@Override
public void commit(CommitState state) throws PackageStorage.PackageIntegrityException {

}
void appendJournal(JournalTransaction.Type type, InstanceInfo info) throws IOException {
      assert journalPath != null;
      JournalTransaction transaction = new JournalTransaction(type, info);
      Files.writeString(journalPath, transaction.stringify(), StandardOpenOption.APPEND);
}
void deleteJournal() throws IOException {
      Files.deleteIfExists(journalPath);
}
void eraseJournal() throws IOException {
      Files.writeString(journalPath, "");
}
List<JournalTransaction> getTransactions() throws IOException {
      if (transactions != null)
            return transactions;
      String stringTransactions = Files.readString(journalPath);
      transactions = JournalTransaction.listOf(stringTransactions);
      return transactions;
}
List<JournalTransaction> getTransactions(JournalTransaction.Type type) throws IOException {
      var transactions = getTransactions();
      List<JournalTransaction> atoms = new ArrayList<>();
      for (var atom : transactions) {
            if (atom.getType() == type)
                  atoms.add(atom);
      }
      return atoms;
}
private List<JournalTransaction> transactions = null;
@Setter(AccessLevel.MODULE) @Getter(AccessLevel.PROTECTED)
private Path journalPath;
@Setter(AccessLevel.PACKAGE) @Getter(AccessLevel.PACKAGE)
private boolean isManaged;
}
