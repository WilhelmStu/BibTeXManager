module org.wst {
    requires javafx.controls;
    requires javafx.fxml;
    //requires jnativehook;
    requires java.logging;

    opens org.wst;
    exports org.wst;

    opens org.wst.model;
}