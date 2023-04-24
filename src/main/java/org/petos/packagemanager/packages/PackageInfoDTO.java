package org.petos.packagemanager.packages;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.petos.packagemanager.database.PackageHat;
import org.petos.packagemanager.database.PackageInfo;

//generally, package info is supposed to transfer through network
public class PackageInfoDTO {
public  String name;
public String[] aliases;
public String payloadType;
public String version;
public String licenseType;
public String[] dependencies;
public Integer payloadSize;
public String toJson(){
      Gson gson = new Gson();
      return gson.toJson(this);
}
public static @Nullable PackageInfoDTO fromJson(@NotNull String rawJson){
      PackageInfoDTO result = null;
      Gson gson = new Gson();
      try {
            result = gson.fromJson(rawJson, PackageInfoDTO.class);
      }catch (JsonSyntaxException ignored){}
      return result;
}
}
