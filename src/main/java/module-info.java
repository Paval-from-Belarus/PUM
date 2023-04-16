module com.example.packagemanager {
      requires annotations;
      requires com.google.gson;
      requires  org.apache.logging.log4j;
      exports org.petos.packagemanager.transfer;
      exports org.petos.packagemanager.server;
      exports org.petos.packagemanager.packages;
      exports org.petos.packagemanager.client;
}