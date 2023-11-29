package org.petos.pum.publisher.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * @author Paval Shlyk
 * @since 29/11/2023
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthorizationUtils {
public static byte[] newRandomSalt() {
      SecureRandom random = new SecureRandom();
      byte[] bytes = new byte[32];
      random.nextBytes(bytes);
      return bytes;
}
@SneakyThrows
public static byte[] generateHash(String password, byte[] salt) {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(salt);
      return md.digest(salt);
}
}
