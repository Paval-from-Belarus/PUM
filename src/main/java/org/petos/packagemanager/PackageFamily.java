package org.petos.packagemanager;

public record PackageFamily(ShortPackageInfo info, String[] versions) {
      public static PackageFamily valueOf(ShortPackageInfo info, String[] versions){
            return new PackageFamily(info, versions);
      }
}
