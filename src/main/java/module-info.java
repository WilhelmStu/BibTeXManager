module org.wst {
    requires javafx.controls;
    requires javafx.fxml;

    opens org.wst to javafx.fxml;
    exports org.wst;
}