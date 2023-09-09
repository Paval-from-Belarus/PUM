module org.petos.pum.server {
      requires static lombok;
      requires static annotations;
      requires static org.mapstruct;
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
      requires org.hibernate.orm.core;
      //messaging
      requires spring.messaging;
      requires spring.integration.core;
      requires spring.integration.ip;
      //tests
      opens org.petos.pum.server;
      opens org.petos.pum.server.dto;
      opens org.petos.pum.server.repositories;
      opens org.petos.pum.server.network;
      opens org.petos.pum.server.properties;
      opens org.petos.pum.server.services;
      opens org.petos.pum.server.repositories.entities;
}