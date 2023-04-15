package org.petos.packagemanager.server;

public record PackageId(int value) {
public static PackageId valueOf(Integer id) {
      return new PackageId(id);
}
}
