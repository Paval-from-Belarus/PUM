package transfer;

import packages.PackageAssembly;
import security.Encryptor;

import java.nio.ByteBuffer;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static packages.PackageAssembly.*;
import static security.Encryptor.*;

public record TransferFormat(ArchiveType archive, Encryption encryption) {
public static final int TRANSFER_BYTES_CNT = 4 * 3 + 170;
public void apply(PackageAssembly assembly) {
      assembly.setEncryption(encryption);
      assembly.setArchive(archive);
}
public byte[] toBytes() {
      ByteBuffer buffer = ByteBuffer.allocate(TRANSFER_BYTES_CNT);
      buffer.putInt(archive().ordinal()).putInt(encryption.ordinal());
      if (encryption.holdsKey()) {
            byte[] bytesKey = encryption.getDetached().getEncoded();
            assert bytesKey != null;
            buffer.putInt(bytesKey.length).put(bytesKey);
      }
      return buffer.array();
}

public static TransferFormat construct(byte[] bytes) {
      if (bytes.length < TRANSFER_BYTES_CNT) {
            throw new IllegalStateException("Incorrect format");
      }
      TransferFormat format;
      try {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            ArchiveType archive = ArchiveType.values()[buffer.getInt()];
            Encryption encryption = Encryption.values()[buffer.getInt()];
            if (encryption != Encryption.None) {
                  int length = buffer.getInt();
                  byte[] bytesKey = new byte[length];
                  buffer.get(bytesKey, 0, length);

                  X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytesKey);
                  KeyFactory factory = KeyFactory.getInstance(encryption.getTransformation());
                  Key key = factory.generatePublic(keySpec); //only public key can be transfer
                  encryption.detachKey(key);
            }
            format = new TransferFormat(archive, encryption);
      } catch (InvalidKeySpecException | NoSuchAlgorithmException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            throw new IllegalStateException("Incorrect format data");
      }
      return format;
}

public static TransferFormat valueOf(ArchiveType archive, Encryption encryption) {
      if (encryption != Encryption.None && !encryption.holdsKey()) {
	    encryption = Encryption.None;
      }
      return new TransferFormat(archive, encryption);
}
}
