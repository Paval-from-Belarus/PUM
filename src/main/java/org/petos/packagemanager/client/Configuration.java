package org.petos.packagemanager.client;

public class Configuration {
public String installation;
public String temporary;

public String getTempPath(){
      return temporary;
}
public String getInstallationPath(){
      return installation;
}
}
