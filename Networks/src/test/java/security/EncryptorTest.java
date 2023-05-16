package security;

import org.junit.jupiter.api.Test;
import requests.VersionFormat;
import transfer.PackageAssembly;
import requests.PayloadRequest;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;
import static security.Encryptor.*;

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
      ForkJoinPool pool = new ForkJoinPool();
      List<Runnable> tasks = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
      	tasks.add(this::changeEnumKey);
      }
      tasks.forEach(pool::submit);
      try {
            Thread.sleep(2000);
      } catch (InterruptedException e) {
            throw new RuntimeException(e);
      }

}
@Test
public void testRequest() {
      var dto = PayloadRequest.valueOf("aa#1##", VersionFormat.Int);
      assertFalse(dto.isPresent());
      dto = PayloadRequest.valueOf("###", VersionFormat.Int);
      assertFalse(dto.isPresent());
      dto = PayloadRequest.valueOf("aaaaaaaa#babbabab#0.0.1", VersionFormat.String);
      assertFalse(dto.isPresent());
      dto = PayloadRequest.valueOf("baba#a1#0.0.1#", VersionFormat.String);
      assertFalse(dto.isPresent());
      var instance = new PayloadRequest(11, "0.0.1", PackageAssembly.ArchiveType.Brotli);
      dto = PayloadRequest.valueOf(instance.stringify(), VersionFormat.String);
      assertTrue(dto.isPresent());
      instance = new PayloadRequest(11, 0, PackageAssembly.ArchiveType.Brotli);
      dto = PayloadRequest.valueOf(instance.stringify(), VersionFormat.Int);
      assertTrue(dto.isPresent());
}
}