package com.example.todolisttt;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;


    private final HashMap<String, User> users = UserDatabase.loadUsers();

    @FXML
    private void handleRegister() {
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

        String hashedPassword = hashPassword(password);
        User newUser = new User(username, hashedPassword);
        users.put(username, newUser);

        UserDatabase.saveUsers(users);
        messageLabel.setText("Registration successful. Please login.");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (!users.containsKey(username)) {
            messageLabel.setText("User not found.");
            return;
        }

        String hashedPassword = hashPassword(password);
        User user = users.get(username);

        if (!user.getPasswordHash().equals(hashedPassword)) {
            messageLabel.setText("Incorrect password.");
            return;
        }

        try {
            URL fxmlLocation = getClass().getResource("MainView.fxml");

            if (fxmlLocation == null) {
                messageLabel.setText("MainView.fxml file not found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Scene scene = new Scene(loader.load());

            MainController controller = loader.getController();
            controller.setCurrentUser(username);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("ToDoList - Welcome " + username);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("An error occurred during login.");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Password encryption failed", e);
        }
    }
}
