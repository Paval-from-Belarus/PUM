module com.example.packagemanager {
      requires annotations;
      requires com.google.gson;
      requires  org.apache.logging.log4j;
      exports org.petos.packagemanager.transfer;
      opens org.petos.packagemanager.transfer to javafx.fxml;
      exports org.petos.packagemanager.server;
      opens org.petos.packagemanager.server to javafx.fxml;
      exports org.petos.packagemanager.packages;
      opens org.petos.packagemanager.packages to javafx.fxml;
      exports org.petos.packagemanager.client;
      opens org.petos.packagemanager.client to javafx.fxml;
}