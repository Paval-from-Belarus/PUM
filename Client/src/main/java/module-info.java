module org.petos.packagemanager.Client {
      requires annotations;
      requires com.google.gson;
      requires static lombok;
      requires org.apache.logging.log4j;
      requires org.petos.packagemanager.Networks;
      requires java.xml;
      opens database;
      opens common;
      opens storage;
}