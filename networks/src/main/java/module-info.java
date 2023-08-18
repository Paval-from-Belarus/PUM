module org.petos.pum.networks {
      requires annotations;
      requires static lombok;
      requires com.aayushatharva.brotli4j;
      requires com.google.gson;
      requires jdk.unsupported;
      requires java.xml;
      opens dto;
      opens transfer;
      opens requests;
      exports transfer;
      exports dto;
      exports security;
      exports requests;
}