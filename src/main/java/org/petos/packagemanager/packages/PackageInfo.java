package org.petos.packagemanager.packages;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//generally, package info is supposed to transfer through network
public class PackageInfo {
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
public static @Nullable PackageInfo fromJson(@NotNull String rawJson){
      PackageInfo result = null;
      Gson gson = new Gson();
      try {
            result = gson.fromJson(rawJson, PackageInfo.class);
      }catch (JsonSyntaxException ignored){}
      return result;
}
}
