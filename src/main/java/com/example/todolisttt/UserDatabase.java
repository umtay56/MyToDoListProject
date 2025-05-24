package com.example.todolisttt;

import java.io.*;
import java.util.HashMap;

public class UserDatabase {
    private static final String FILE_NAME = "users.dat";

    public static HashMap<String, User> loadUsers() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            return (HashMap<String, User>) in.readObject();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static void saveUsers(HashMap<String, User> users) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            out.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
