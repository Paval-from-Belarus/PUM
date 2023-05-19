module org.petos.packagemanager.Networks {
      requires annotations;
      requires static lombok;
      requires com.aayushatharva.brotli4j;
      requires com.google.gson;
      opens packages;
      opens transfer;
      opens requests;
      exports transfer;
      exports packages;
      exports security;
      exports requests;
}