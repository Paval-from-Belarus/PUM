module org.petos.packagemanager.Networks {
      requires annotations;
      requires com.aayushatharva.brotli4j;
      opens packages;
      opens transfer;
      exports transfer;
      exports packages;
      exports security;
}