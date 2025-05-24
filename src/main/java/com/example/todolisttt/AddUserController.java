package com.example.todolisttt;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.HashMap;

public class AddUserController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final HashMap<String, User> users = UserDatabase.loadUsers();

    @FXML
    private void handleAddUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            messageLabel.setText("Username and password cannot be empty.");
            return;
        }

        if (users.containsKey(username)) {
            messageLabel.setText("This username is already registered.");
            return;
        }

        String hashed = LoginControllerStatic.hashPassword(password);
        users.put(username, new User(username, hashed));
        UserDatabase.saveUsers(users);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "User added successfully!");
        alert.showAndWait();

        backToMainView();
    }

    @FXML
    private void handleCancel() {
        backToMainView();
    }

    private void backToMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("ToDoList");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
