package org.petos.packagemanager.packages;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//generally, package info is supposed to transfer through network
//this class hasn't any real representation in database
//Client should only accept this entity
//This info soo verbose
public class FullPackageInfoDTO {
public  String name;
public String[] aliases;
public String payloadType;
public String version;
public String licenseType;
public DependencyInfo[] dependencies;
public Integer payloadSize;
}
