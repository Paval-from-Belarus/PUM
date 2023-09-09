module org.petos.pum.networks {
      requires static lombok;
      requires com.aayushatharva.brotli4j;
      requires com.google.gson;
      requires jdk.unsupported;
      requires java.xml;
      requires annotations;
      opens org.petos.pum.networks.dto;
      opens org.petos.pum.networks.transfer;
      opens org.petos.pum.networks.requests;
      exports org.petos.pum.networks.transfer;
      exports org.petos.pum.networks.dto;
      exports org.petos.pum.networks.security;
      exports org.petos.pum.networks.requests;
}