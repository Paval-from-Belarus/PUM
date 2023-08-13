module org.petos.packagemanager.Server {
      requires static lombok;
      requires static annotations;
      requires org.petos.packagemanager.Networks;
      requires jakarta.persistence;
      requires spring.boot;
      requires spring.context;
      requires spring.beans;
      requires spring.core;
      requires spring.boot.autoconfigure;
      requires spring.integration.core;
      requires spring.integration.ip;
      opens database;
      opens main;
}