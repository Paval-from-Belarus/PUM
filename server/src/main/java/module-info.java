module org.petos.pum.server {
      requires static lombok;
      requires static annotations;
      requires org.petos.pum.Networks;
      requires jakarta.persistence;
      requires spring.boot;
      requires spring.boot.autoconfigure;
      requires spring.context;
      requires spring.beans;
      requires spring.core;
      requires spring.messaging;
      requires spring.integration.core;
      requires spring.integration.ip;
      opens org.petos.pum.server;
      opens org.petos.pum.server.database;
      opens org.petos.pum.server.network;
      opens org.petos.pum.server.common;
}