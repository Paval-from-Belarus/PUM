package org.petos.packagemanager.packages;

public record ShortPackageInfo(String name, String[] aliases, String payloadType) {
      public static ShortPackageInfo valueOf(PackageInfo fullInfo){
	    return new ShortPackageInfo(fullInfo.name, fullInfo.aliases, fullInfo.payloadType);
      }
}
