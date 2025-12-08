import java.io.*;
import java.util.LinkedList;
import java.util.Scanner;

class User {
    String username;
    String password;

    User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

public class Login {
    private static LinkedList<User> accounts = new LinkedList<>();
    private static Scanner s = new Scanner(System.in);
    private static final String FILE_NAME = "accounts.txt";

    public static void main(String[] args) {
        loadAccounts();

        OUTER: while (true) {
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            int choice;

            try {
                choice = Integer.parseInt(s.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number.");
                continue;
            }

            switch (choice) {
                case 1:
                    register();
                    saveAccounts();
                    break;
                case 2:
                    logon();
                    break;
                case 3:
                    saveAccounts();
                    s.close();
                    break OUTER;
                default:
                    System.out.println("Invalid option. Try again.");
                    break;
            }
        }
    }

    private static void saveAccounts() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (User user : accounts) {
                pw.println(user.username + "," + user.password);
            }
        } catch (IOException e) {
            System.out.println("Error saving accounts: " + e.getMessage());
        }
    }

    private static void loadAccounts() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2)
                    accounts.add(new User(parts[0], parts[1]));
            }
        } catch (IOException e) {
            System.out.println("Cannot load the account: " + e.getMessage());
        }
    }

    private static void register() {
        System.out.print("Create username: ");
        String newUsername = s.nextLine();

        for (User user : accounts) {
            if (user.username.equals(newUsername)) {
                System.out.println("Username already exists! Try again.");
                return;
            }
        }

        System.out.print("Create password: ");
        String newPassword = s.nextLine();

        accounts.add(new User(newUsername, newPassword));
        System.out.println("Registration successful!");
    }

    private static void logon() {
        System.out.print("Enter username: ");
        String username = s.nextLine();
        System.out.print("Enter password: ");
        String password = s.nextLine();

        boolean loggedIn = false;
        for (User user : accounts) {
            if (user.username.equals(username) && user.password.equals(password)) {
                loggedIn = true;
                break;
            }
        }

        if (loggedIn)
            System.out.println("Login successful!");
        else
            System.out.println("Invalid username or password.");
    }
}
