package security;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.petos.pum.networks.security.Encryptor.*;

class EncryptorTest {

private void changeEnumKey() {
      Encryption type = Encryption.Des;
      SecretKey key = generateSecret(Encryption.Des);
      type.detachKey(key);
      System.out.println(Thread.currentThread().getId() + " has " + key);
      Thread.yield();
      System.out.println(Thread.currentThread().getId() + " has " + key);
}
@Test
public void testEnums() {

}
@Test
public void testRequest() {
//      var dto = PayloadRequest.valueOf("aa#1##", VersionFormat.Int);
//      assertFalse(dto.isPresent());
//      dto = PayloadRequest.valueOf("###", VersionFormat.Int);
//      assertFalse(dto.isPresent());
//      dto = PayloadRequest.valueOf("aaaaaaaa#babbabab#0.0.1", VersionFormat.String);
//      assertFalse(dto.isPresent());
//      dto = PayloadRequest.valueOf("baba#a1#0.0.1#", VersionFormat.String);
//      assertFalse(dto.isPresent());
//      var instance = new PayloadRequest(11, "0.0.1", PackageAssembly.ArchiveType.Brotli);
//      dto = PayloadRequest.valueOf(instance.stringify(), VersionFormat.String);
//      assertTrue(dto.isPresent());
//      instance = new PayloadRequest(11, 0, PackageAssembly.ArchiveType.Brotli);
//      dto = PayloadRequest.valueOf(instance.stringify(), VersionFormat.Int);
//      assertTrue(dto.isPresent());
}
}