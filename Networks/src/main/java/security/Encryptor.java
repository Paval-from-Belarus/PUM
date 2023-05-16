package security;


import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Encryptor {
//todo: if want to use Encryption in multithreading ― replace signle method to two: foreign_key and local_key
public enum Encryption {
      None, Rsa, Des;
      @Getter
      private String transformation = "";
      @Getter
      private Key detached = null;

      static {
	    Rsa.transformation = "RSA";
	    Des.transformation = "DES";
      }

      public boolean holdsKey() {
	    return detached != null;
      }
      public void detachKey(@NotNull Key key) {
	    this.detached = key;
      }
      public boolean isCompatible(Encryption other) {
	    return other == None || other == this;
      }
      public boolean isSymmetric() {
	    return switch (this) {
		  case None, Rsa -> false;
		  case Des -> true;
	    };
      }
      public boolean isAsymmetric() {
	    return switch (this) {
		  case None, Des -> false;
		  case Rsa -> true;
	    };
      }
}

@Setter
@Getter
private Encryption type;

public Encryptor(Encryption type) {
      this.type = type;
}


/**
 * @return that is possible to encrypt data by chosen algorithm
 */
public static boolean validate(Encryption type, byte[] payload) {
      return switch (type) {
	    case None, Des -> true;
	    case Rsa -> payload.length <= 117;
      };
}
//there should be public key
public byte[] encrypt(byte[] payload) {
      checkEncryptionState();
      byte[] result = payload;
      Key key = type.getDetached();
      if (type != Encryption.None) {
	    try {
		  Cipher cipher = Cipher.getInstance(type.getTransformation());
		  cipher.init(Cipher.ENCRYPT_MODE, key);
		  result = cipher.doFinal(payload);
	    } catch (GeneralSecurityException e) {
		  throw new IllegalStateException(e);
	    }
      }
      return result;
}

public byte[] decrypt(byte[] payload) {
      checkEncryptionState();
      byte[] result = payload;
      Key secret = type.getDetached();
      if (type != Encryption.None) {
	    try {
		  Cipher cipher = Cipher.getInstance(type.getTransformation());
		  cipher.init(Cipher.DECRYPT_MODE, secret);
		  result = cipher.doFinal(payload);
	    } catch (GeneralSecurityException e) {
		  throw new IllegalStateException(e);
	    }
      }
      return result;
}
private void checkEncryptionState(){
      if (type != Encryption.None && !type.holdsKey()) {
	    throw new IllegalStateException("The specific encryption should holds key");
      }
}
public static Key restoreKey(byte[] encoded, Encryption type) {
      Key result = null;
      try {
	    switch (type) {
		  case Rsa -> {
			var keySpec = new X509EncodedKeySpec(encoded);
			KeyFactory factory = KeyFactory.getInstance(type.getTransformation());
			result = factory.generatePublic(keySpec);
		  }
		  case Des -> result = new SecretKeySpec(encoded, 0, encoded.length, type.getTransformation());
	    }
      } catch (GeneralSecurityException e) {
	    e.printStackTrace();
	    throw new RuntimeException(e);
      }
      return result;
}
public static SecretKey generateSecret(Encryption type) {
      if (!type.isSymmetric()) {
	    throw  new IllegalStateException("Invalid encryption mode");
      }
      SecretKey secretKey;
      try {
	    KeyGenerator keyGen = KeyGenerator.getInstance(type.getTransformation());
	    secretKey = keyGen.generateKey();
      } catch (GeneralSecurityException e) {
	    throw  new IllegalStateException("Encryptor is set inappropriately");
      }
      return secretKey;
}
public static KeyPair generatePair(Encryption type) {
      if (!type.isAsymmetric())
	    throw  new IllegalStateException("Impossible generate pair for None encryption mode");
      KeyPair pair;
      try {
	    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(type.getTransformation());
	    keyGen.initialize(1024);
	    pair = keyGen.generateKeyPair();
      } catch (NoSuchAlgorithmException e) {
	    throw  new IllegalStateException("Encryptor is set inappropriately");
      }
      return pair;

}
}
