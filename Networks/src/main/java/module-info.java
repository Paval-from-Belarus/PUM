module org.petos.packagemanager.Networks {
      requires annotations;
      requires static lombok;
      requires com.aayushatharva.brotli4j;
      opens packages;
      opens transfer;
      exports transfer;
      exports packages;
      exports security;
}