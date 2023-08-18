module org.petos.pum.Client {
      requires annotations;
      requires com.google.gson;
      requires static lombok;
      requires org.apache.logging.log4j;
      requires org.petos.pum.networks;
      requires java.xml;
      opens database;
      opens common;
      opens storage;
}