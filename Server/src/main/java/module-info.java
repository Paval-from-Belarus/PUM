module org.petos.packagemanager.Server {
      requires annotations;
      requires com.google.gson;
      requires org.apache.logging.log4j;
      requires java.sql;
      requires org.hsqldb;
      requires org.hibernate.orm.core;
      requires java.persistence;
      requires org.petos.packagemanager.Networks;
      opens database;
      opens common;
}