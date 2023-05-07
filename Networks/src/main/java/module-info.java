module org.petos.packagemanager.Networks {
      requires annotations;
      opens packages;
      opens transfer;
      exports transfer;
      exports packages;
      exports security;
}