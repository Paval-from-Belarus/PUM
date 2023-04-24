package org.petos.packagemanager.packages;

import org.petos.packagemanager.database.PackageHat;

public record ShortPackageInfoDTO(String name, String[] aliases, String payloadType) {
      public static ShortPackageInfoDTO valueOf(PackageHat hat){
	    String[] aliases = hat.getAliases().toArray(String[]::new);
	    return new ShortPackageInfoDTO(hat.getName(), aliases, hat.getPayload().getName());
      }
}
