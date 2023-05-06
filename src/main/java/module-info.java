module com.example.packagemanager {
      requires annotations;
      requires com.google.gson;
      requires  org.apache.logging.log4j;
      requires java.sql;
      requires org.hsqldb;
      requires org.hibernate.orm.core;
      requires java.persistence;
      exports org.petos.packagemanager.server;
      exports org.petos.packagemanager.networks.packages;
      exports org.petos.packagemanager.client;
      opens org.petos.packagemanager.server.database;
      exports org.petos.packagemanager.client.database;
      exports org.petos.packagemanager.client.storage;
      exports org.petos.packagemanager.networks.transfer;
}