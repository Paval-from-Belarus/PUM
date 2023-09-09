package org.petos.pum.networks.security;


import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.petos.pum.networks.transfer.SimpleTransfer;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class Encryptor {
//todo: if want to use Encryption in multithreading ― replace single method to two: foreign_key and local_key
public enum Encryption implements SimpleTransfer<Encryption> {
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
	    if (this != None) {
		  this.detached = key;
	    }
	    else {
		  throw new IllegalArgumentException("Key is not available for None");
	    }
      }
      public void releaseKey() {
	    this.detached = null;
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
      public byte[] serialize() {
	    byte[] key = new byte[0];
	    if (this.detached != null) {
		  key = detached.getEncoded();
	    }
	    ByteBuffer buffer = ByteBuffer.allocate(1 + key.length);
	    buffer.put((byte)this.ordinal()).put(key);
	    return buffer.array();
      }
      public Encryption deserialize(byte[] encoded) {
	    Encryption result = null;
	    if (encoded.length >= 1) {
		  ByteBuffer buffer = ByteBuffer.wrap(encoded);
		  int ordinal = buffer.get();
		  byte[] bytes = new byte[encoded.length - 1]; //the rest
		  System.arraycopy(encoded, 1, bytes, 0, bytes.length);
		  if (ordinal < Encryption.values().length) {
			Encryption self = Encryption.values()[ordinal];
			Key key = Encryptor.restoreKey(bytes, self);
			if (key != null && self != None) {
			      self.detachKey(key);
			}
			if (self == None || self.holdsKey()) {
			      result = self;
			}
		  }
	    } else {
		  throw new IllegalStateException("The serialized array is not correct");
	    }
	    if (result == null) {
		  throw new IllegalStateException("Impossible to construct encoded key by input source");
	    }
	    return result;
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
public static @Nullable Key restoreKey(byte[] encoded, Encryption type) {
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
