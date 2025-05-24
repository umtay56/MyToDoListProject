package com.example.todolisttt;

import javafx.scene.input.MouseEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;

public class MainController {

    @FXML private ListView<TaskItem> taskListView;
    @FXML private TextField taskInput;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> hourBox;
    @FXML private ComboBox<String> minuteBox;
    @FXML private TextArea detailArea;
    @FXML private ListView<String> userListView;
    @FXML private VBox userPanel;
    @FXML private Button toggleUserButton;
    @FXML private VBox taskPanel;
    @FXML private HBox mainContainer;


    private Runnable currentFilterAction = this::showAllTasks;



    private final ObservableList<TaskItem> allTasks = FXCollections.observableArrayList();
    private final HashMap<String, User> users = UserDatabase.loadUsers();
    private String currentUser;

    public void setCurrentUser(String username) {
        this.currentUser = username;
        loadUserTasks();
        setupListView();
    }

    @FXML
    private void initialize() {
        Locale.setDefault(Locale.ENGLISH);

        for (int i = 0; i < 24; i++) hourBox.getItems().add(String.format("%02d", i));
        for (int i = 0; i < 60; i += 5) minuteBox.getItems().add(String.format("%02d", i));

        userPanel.setVisible(false);
        userPanel.setManaged(false);
        toggleUserButton.setText("Show Users");
        HBox.setHgrow(taskPanel, Priority.ALWAYS);

        datePicker.getEditor().setStyle(
                "-fx-background-color: #eee8c9; -fx-text-fill: black; -fx-prompt-text-fill: black;"
        );

        datePicker.setPromptText("Select date");
        datePicker.setConverter(new StringConverter<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);

            @Override
            public String toString(LocalDate date) {
                return (date != null) ? formatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                return (string != null && !string.isEmpty()) ? LocalDate.parse(string, formatter) : null;
            }
        });

        userListView.setItems(FXCollections.observableArrayList(users.keySet()));

        userListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedUser = userListView.getSelectionModel().getSelectedItem();
                if (selectedUser != null) promptPasswordForUser(selectedUser);
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete User");
        deleteItem.setOnAction(e -> {
            String selectedUser = userListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Are you sure you want to delete user \"" + selectedUser + "\"?",
                        ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        deleteUser(selectedUser);
                    }
                });
            }
        });
        contextMenu.getItems().add(deleteItem);
        userListView.setContextMenu(contextMenu);
    }

    @FXML
    private void toggleUserPanel() {
        boolean isVisible = userPanel.isVisible();
        userPanel.setVisible(!isVisible);
        userPanel.setManaged(!isVisible);
        toggleUserButton.setText(isVisible ? "Show Users" : "Hide Users");
        HBox.setHgrow(taskPanel, Priority.ALWAYS);
    }

    private void promptPasswordForUser(String username) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Password Required");
        dialog.setHeaderText("Switch to user: " + username);
        dialog.setContentText("Enter password:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(inputPassword -> {
            String hashedInput = LoginControllerStatic.hashPassword(inputPassword);
            User user = users.get(username);
            if (user != null && user.getPasswordHash().equals(hashedInput)) {
                setCurrentUser(username);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Switched to user: " + username);
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Incorrect password.");
                alert.showAndWait();
            }
        });
    }

    private void deleteUser(String username) {
        users.remove(username);
        UserDatabase.saveUsers(users);

        File taskFile = new File("tasks_" + username + ".dat");
        if (taskFile.exists()) taskFile.delete();

        userListView.setItems(FXCollections.observableArrayList(users.keySet()));

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "User \"" + username + "\" deleted.");
        alert.showAndWait();

        if (username.equals(currentUser)) {
            handleLogout();
        }
    }

    private void setupListView() {
        taskListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(TaskItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    String fullDesc = item.getDescription();
                    String[] parts = fullDesc.split(" - ", 2);
                    String rawDateTime = parts.length > 0 ? parts[0] : "";
                    String title = parts.length > 1 ? parts[1] : "";

                    String formattedDate = rawDateTime;
                    try {
                        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d • HH:mm", Locale.ENGLISH);
                        LocalDateTime dateTime = LocalDateTime.parse(rawDateTime, inputFormat);
                        formattedDate = dateTime.format(outputFormat);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Label titleLabel = new Label(title);
                    Label dateLabel = new Label(formattedDate);
                    Label detailLabel = new Label(item.getDetail());

                    dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
                    detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #777;");
                    detailLabel.setWrapText(true);
                    detailLabel.setMaxWidth(600);

                    VBox vBox = new VBox(2, titleLabel, dateLabel, detailLabel);

                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(item.isCompleted());

                    Runnable applyCompletionStyle = () -> {
                        if (item.isCompleted()) {
                            titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-strikethrough: true;");
                            setStyle("-fx-background-color: #c9ffa6; -fx-background-radius: 5;");
                        } else {
                            titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
                            setStyle("");
                        }
                    };
                    applyCompletionStyle.run();

                    checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        item.setCompleted(newVal);
                        saveUserTasks();
                        applyCompletionStyle.run();
                    });

                    HBox container = new HBox(10, checkBox, vBox);
                    container.setPrefWidth(800);
                    container.setStyle("-fx-background-color: rgba(255,255,255,0.4); -fx-background-radius: 10; -fx-padding: 10;");

                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem deleteItem = new MenuItem("Delete");
                    deleteItem.setOnAction(e -> {
                        allTasks.remove(item);
                        taskListView.getItems().remove(item);
                        saveUserTasks();
                    });
                    contextMenu.getItems().add(deleteItem);
                    setContextMenu(contextMenu);

                    setGraphic(container);
                }
            }
        });

        taskListView.setItems(allTasks);
    }


    @FXML
    private void handleAddTask() {
        String task = taskInput.getText();
        LocalDate date = datePicker.getValue();
        String hour = hourBox.getValue();
        String minute = minuteBox.getValue();
        String detail = detailArea.getText();

        if (task != null && !task.isEmpty() && date != null && hour != null && minute != null) {
            String fullDescription = String.format("%02d/%02d/%04d %s:%s - %s",
                    date.getDayOfMonth(), date.getMonthValue(), date.getYear(),
                    hour, minute, task);

            TaskItem newTask = new TaskItem(fullDescription, detail);
            allTasks.add(newTask);

            taskInput.clear();
            datePicker.setValue(null);
            hourBox.setValue(null);
            minuteBox.setValue(null);
            detailArea.clear();

            saveUserTasks();

            currentFilterAction.run();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please fill in all task fields.");
            alert.showAndWait();
        }
    }



    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginView.fxml"));
            Scene scene = new Scene(loader.load(), 600, 600);
            Stage stage = (Stage) taskListView.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExit() {
        Stage stage = (Stage) taskPanel.getScene().getWindow();
        stage.close();
    }


    @FXML
    private void filterToday() {
        LocalDate today = LocalDate.now();
        filterTasksByDateRange(today, today);
        currentFilterAction = this::filterToday;
    }

    @FXML
    private void filterUpcomingTasks() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate endDay = LocalDate.now().plusDays(3);
        filterTasksByDateRange(tomorrow, endDay);
        currentFilterAction = this::filterUpcomingTasks;
    }

    @FXML
    private void filterWeek() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate endOfWeek = LocalDate.now().with(java.time.DayOfWeek.SUNDAY);
        if (endOfWeek.isBefore(tomorrow)) {
            endOfWeek = tomorrow;
        }
        filterTasksByDateRange(tomorrow, endOfWeek);
        currentFilterAction = this::filterWeek;
    }

    @FXML
    private void filterMonth() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate lastDayOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        filterTasksByDateRange(tomorrow, lastDayOfMonth);
        currentFilterAction = this::filterMonth;
    }

    @FXML
    private void showAllTasks() {
        taskListView.setItems(allTasks);
        currentFilterAction = this::showAllTasks;
    }




    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginView.fxml")); // artık AddUserView değil
            Scene scene = new Scene(loader.load(), 600, 600);
            Stage stage = (Stage) taskPanel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.centerOnScreen();
            stage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void loadUserTasks() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("tasks_" + currentUser + ".dat"))) {
            List<TaskItem> tasks = (List<TaskItem>) in.readObject();
            allTasks.setAll(tasks);
        } catch (Exception e) {
            allTasks.clear();
        }
    }

    private void filterTasksByDateRange(LocalDate start, LocalDate end) {
        ObservableList<TaskItem> filtered = FXCollections.observableArrayList();

        for (TaskItem item : allTasks) {
            try {
                String[] parts = item.getDescription().split(" - ", 2);
                if (parts.length > 0) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    LocalDateTime dateTime = LocalDateTime.parse(parts[0], formatter);
                    LocalDate taskDate = dateTime.toLocalDate();

                    if ((taskDate.isEqual(start) || taskDate.isAfter(start)) &&
                            (taskDate.isEqual(end) || taskDate.isBefore(end))) {
                        filtered.add(item);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        taskListView.setItems(filtered);
    }


    private void saveUserTasks() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("tasks_" + currentUser + ".dat"))) {
            out.writeObject(new ArrayList<>(allTasks));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

