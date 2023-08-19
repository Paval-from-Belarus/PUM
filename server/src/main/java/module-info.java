module org.petos.pum.server {
      requires static lombok;
      requires static annotations;
      requires org.petos.pum.networks;
      requires jakarta.persistence;
      requires jakarta.annotation;
      requires spring.boot;
      requires spring.boot.autoconfigure;
      requires spring.context;
      requires spring.beans;
      requires spring.core;
      //data sources
      requires spring.data.commons;
      requires spring.data.jpa;
      requires spring.orm;
      requires spring.jdbc;
      requires spring.tx;
      requires com.zaxxer.hikari;
      //messaging
      requires spring.messaging;
      requires spring.integration.core;
      requires spring.integration.ip;
      opens org.petos.pum.server;
      opens org.petos.pum.server.database;
      opens org.petos.pum.server.network;
      opens org.petos.pum.server.common;
}