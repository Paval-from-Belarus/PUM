package org.petos.packagemanager;

import org.petos.packagemanager.transfer.ShortPackageInfo;

public record PackageFamily(ShortPackageInfo info, String[] versions) {
      public static PackageFamily valueOf(ShortPackageInfo info, String[] versions){
            return new PackageFamily(info, versions);
      }
}
