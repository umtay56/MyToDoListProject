module com.example.todolisttt {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.todolisttt to javafx.fxml;
    exports com.example.todolisttt;
}