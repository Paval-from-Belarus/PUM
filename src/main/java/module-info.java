module com.example.packagemanager {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
      requires annotations;
      requires com.google.gson;

      opens org.petos.packagemanager to javafx.fxml;
    exports org.petos.packagemanager;
}