module com.example.packagemanager {
      requires javafx.controls;
      requires javafx.fxml;

      requires org.controlsfx.controls;
      requires com.dlsc.formsfx;
      requires net.synedra.validatorfx;

      requires annotations;
      requires com.google.gson;
      requires  org.apache.logging.log4j;
      opens org.petos.packagemanager to javafx.fxml;
      exports org.petos.packagemanager;
      exports org.petos.packagemanager.transfer;
      opens org.petos.packagemanager.transfer to javafx.fxml;
}