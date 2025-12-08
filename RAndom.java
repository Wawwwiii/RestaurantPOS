package projectjava;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.*;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;


public final class RAndom extends JFrame {

    private static final Color MENU_BG = Color.decode("#7ef59bff");
    private static final Color RECEIPT_BG = Color.decode("#6fa187ff");
    private static final Font MENU_FONT = new Font("Roboto", Font.BOLD, 20);
    private static final Font BUTTON_FONT = new Font("Roboto", Font.PLAIN, 20);
    private static final int ITEM_BUTTON_HEIGHT = 80;
    private static final int ITEM_BUTTON_WIDTH = 200;

    private final List<Category> categories = MenuInitializer.initializeMenu();
    private Order currentOrder = new Order();
    private String lastItemName;
    private int lastItemQty, lastItemPrice;
    private final OrderQueue orderQueue = new OrderQueue();
    private final JTextArea receiptArea = new JTextArea(20, 40);
    private JScrollPane receiptScroll;
    private JTable orderTable;
    private DefaultTableModel orderTableModel;
    private JTabbedPane menuTabbedPane;
    private static int orderCounter;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private boolean beepEnabled = true;
    private boolean darkMode = false;
    private Clip backgroundMusicClip;
    private MediaPlayer backgroundMediaPlayer;
    private Map<String, String> customers = new HashMap<>();
    private javax.swing.Timer backupTimer;
    private double totalSales = 0.0;
    private static final String DAILY_SALES_FILE_PREFIX = "daily_sales_";

    private static java.util.LinkedList<User> accounts = new java.util.LinkedList<>();
    private static java.util.Scanner loginScanner = new java.util.Scanner(System.in);
    private static final String ACCOUNTS_FILE = "accounts.txt";

    public RAndom() {
        this.loadOrderCounter();
        this.loadDailySales();
        Login.loadAccounts();
        if (!loginCashier()) {
            JOptionPane.showMessageDialog(this, "Login cancelled. Exiting.", "Exit", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
        this.initGUI();
        this.startBackupTimer();
        this.playBackgroundMusic();
        this.setVisible(true);
    }

    private boolean loginCashier() {
        Login.loadAccounts();

        while (true) {
            String[] options = {"Login", "Register", "Cancel"};
            int action = JOptionPane.showOptionDialog(this, "Choose action", "Cashier Authentication",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

            if (action == 0) {
                JPanel panel = new JPanel(new GridLayout(2, 2));
                JTextField userField = new JTextField();
                JPasswordField passField = new JPasswordField();
                panel.add(new JLabel("Username:"));
                panel.add(userField);
                panel.add(new JLabel("Password:"));
                panel.add(passField);

                int res = JOptionPane.showConfirmDialog(this, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (res != JOptionPane.OK_OPTION) {
                    return false;
                }

                String username = userField.getText();
                String password = new String(passField.getPassword());

                boolean loggedIn = false;
                for (User u : accounts) {
                    if (u.username.equals(username) && u.password.equals(password)) {
                        loggedIn = true;
                        break;
                    }
                }

                if (loggedIn) {
                    JOptionPane.showMessageDialog(this, "Login successful!");
                    return true;
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.", "Error", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

            } else if (action == 1) {
                JPanel panel = new JPanel(new GridLayout(2, 2));
                JTextField userField = new JTextField();
                JPasswordField passField = new JPasswordField();
                panel.add(new JLabel("New username:"));
                panel.add(userField);
                panel.add(new JLabel("New password:"));
                panel.add(passField);

                int res = JOptionPane.showConfirmDialog(this, panel, "Register", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (res != JOptionPane.OK_OPTION) {
                    continue;
                }

                String newUsername = userField.getText().trim();
                String newPassword = new String(passField.getPassword());
                if (newUsername.isEmpty() || newPassword.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                boolean exists = false;
                for (User u : accounts) {
                    if (u.username.equals(newUsername)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    JOptionPane.showMessageDialog(this, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                accounts.add(new User(newUsername, newPassword));
                Login.saveAccounts();
                JOptionPane.showMessageDialog(this, "Registration successful!");
                continue;

            } else {
                return false;
            }
        }
    }

    private void loadOrderCounter() {
        try (BufferedReader br = new BufferedReader(new FileReader("order_counter.txt"))) {
            String line = br.readLine();
            orderCounter = (line != null && !line.trim().isEmpty()) ? Integer.parseInt(line.trim()) : 1;
        } catch (IOException | NumberFormatException e) {
            orderCounter = 1;
        }
    }

    private void saveOrderCounter() {
        try (PrintWriter out = new PrintWriter(new FileWriter("order_counter.txt"))) {
            out.println(orderCounter);
        } catch (IOException e) {
            showError("Error saving order counter: " + e.getMessage());
        }
    }

    private String getDailySalesFilename() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return DAILY_SALES_FILE_PREFIX + today + ".txt";
    }

    private void loadDailySales() {
        String filename = getDailySalesFilename();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine();
            totalSales = (line != null && !line.trim().isEmpty()) ? Double.parseDouble(line.trim()) : 0.0;
            logError("Loaded today's sales: ₱" + String.format("%.2f", totalSales));
        } catch (IOException | NumberFormatException e) {
            totalSales = 0.0;
            logError("No previous sales today, starting fresh: " + e.getMessage());
        }
    }

    private void saveDailySales() {
        String filename = getDailySalesFilename();
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println(totalSales);
            logError("Saved today's sales: ₱" + String.format("%.2f", totalSales) + " to " + filename);
        } catch (IOException e) {
            logError("Error saving daily sales: " + e.getMessage());
        }
    }

    private void startBackupTimer() {
        backupTimer = new javax.swing.Timer(600000, e -> backupData());
        backupTimer.start();
    }

    private void backupData() {
        try (PrintWriter out = new PrintWriter(new FileWriter("backup_orders.txt", true))) {
            out.println("Backup at " + LocalDateTime.now().format(formatter));
        } catch (IOException e) {
            logError("Backup failed: " + e.getMessage());
        }
    }

    private void initGUI() {
        setTitle("Gustopia - Ultimatepromax Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());
        add(createMenuPanel(), BorderLayout.WEST);
        add(createOrderPanel(), BorderLayout.CENTER);
        add(createRightPanel(), BorderLayout.EAST);
        setJMenuBar(createMenuBar());
        pack();
    }

    static class User {

        String username;
        String password;

        User(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class Login {

        public static void loadAccounts() {
            java.io.File file = new java.io.File(ACCOUNTS_FILE);
            if (!file.exists()) {
                return;
            }

            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        accounts.add(new User(parts[0], parts[1]));
                    }
                }
            } catch (java.io.IOException e) {
                System.out.println("Cannot load the account: " + e.getMessage());
            }
        }

        public static void saveAccounts() {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(ACCOUNTS_FILE))) {
                for (User user : accounts) {
                    pw.println(user.username + "," + user.password);
                }
            } catch (java.io.IOException e) {
                System.out.println("Error saving accounts: " + e.getMessage());
            }
        }

        public static void register() {
            System.out.print("Create username: ");
            String newUsername = loginScanner.nextLine();

            for (User user : accounts) {
                if (user.username.equals(newUsername)) {
                    System.out.println("Username already exists! Try again.");
                    return;
                }
            }

            System.out.print("Create password: ");
            String newPassword = loginScanner.nextLine();

            accounts.add(new User(newUsername, newPassword));
            System.out.println("Registration successful!");
        }

        public static void logon() {
            System.out.print("Enter username: ");
            String username = loginScanner.nextLine();
            System.out.print("Enter password: ");
            String password = loginScanner.nextLine();

            boolean loggedIn = false;
            for (User user : accounts) {
                if (user.username.equals(username) && user.password.equals(password)) {
                    loggedIn = true;
                    break;
                }
            }

            if (loggedIn) {
                System.out.println("Login successful!");
            } else {
                System.out.println("Invalid username or password.");
            }
        }
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(MENU_BG);
        panel.setPreferredSize(new Dimension(450, 600));
        menuTabbedPane = new JTabbedPane();
        for (Category category : categories) {
            JPanel tabPanel = new JPanel(new GridLayout(0, 2, 15, 15));
            tabPanel.setBackground(MENU_BG);
            for (Menu item : category.getItems()) {
                JButton itemButton = createItemButton(item);
                tabPanel.add(itemButton);
            }
            menuTabbedPane.addTab(category.getName(), tabPanel);
        }
        panel.add(menuTabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private JButton createItemButton(Menu item) {
        JButton button = new JButton("<html><center><b>" + item.getName() + "</b><br>₱" + item.getPrice() + "</center></html>");
        button.setFont(BUTTON_FONT);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setMargin(new Insets(10, 10, 10, 10));
        button.addActionListener(e -> addItemToOrder(item));
        button.setToolTipText("Click to add " + item.getName() + " to order");
        return button;
    }

    private JPanel createOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Order Summary"));
        String[] columnNames = {"Item", "Qty", "Price", "Total", "Action"};
        orderTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4;
            }
        };
        orderTable = new JTable(orderTableModel);
        orderTable.setFont(BUTTON_FONT);
        orderTable.setRowHeight(25);
        orderTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        orderTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));
        JScrollPane tableScroll = new JScrollPane(orderTable);
        panel.add(tableScroll, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton clearOrderBtn = new JButton("Clear Order");
        clearOrderBtn.setFont(BUTTON_FONT);
        clearOrderBtn.setPreferredSize(new Dimension(150, 50));
        clearOrderBtn.addActionListener(e -> clearOrder());
        JButton handlePaymentBtn = new JButton("Handle Payment");
        handlePaymentBtn.setFont(BUTTON_FONT);
        handlePaymentBtn.setPreferredSize(new Dimension(200, 50));
        handlePaymentBtn.setMnemonic(KeyEvent.VK_P);
        handlePaymentBtn.addActionListener(e -> handlePayment());
        buttonPanel.add(clearOrderBtn);
        buttonPanel.add(handlePaymentBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel queuePanel = new JPanel(new BorderLayout());
        queuePanel.setBorder(BorderFactory.createTitledBorder("Order Queue"));
        JList<String> queueList = new JList<>(orderQueue.getListModel());
        queueList.setFont(BUTTON_FONT);
        JScrollPane queueScroll = new JScrollPane(queueList);
        queuePanel.add(queueScroll, BorderLayout.CENTER);
        JPanel queueButtonPanel = new JPanel();
        JButton serveBtn = new JButton("Serve Next");
        serveBtn.setFont(BUTTON_FONT);
        serveBtn.setPreferredSize(new Dimension(150, 50));
        serveBtn.addActionListener(e -> serveOrder());
        JButton historyBtn = new JButton("History");
        historyBtn.setFont(BUTTON_FONT);
        historyBtn.setPreferredSize(new Dimension(150, 50));
        historyBtn.addActionListener(e -> showReceiptHistory());
        queueButtonPanel.add(serveBtn);
        queueButtonPanel.add(historyBtn);
        queuePanel.add(queueButtonPanel, BorderLayout.SOUTH);
        panel.add(queuePanel, BorderLayout.NORTH);
        JPanel receiptPanel = new JPanel(new BorderLayout());
        receiptPanel.setBorder(BorderFactory.createTitledBorder("Receipt"));
        receiptPanel.setBackground(Color.decode("#187613ff"));
        receiptArea.setBackground(RECEIPT_BG);
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        receiptScroll = new JScrollPane(receiptArea);
        receiptPanel.add(receiptScroll, BorderLayout.CENTER);
        JPanel receiptButtonPanel = new JPanel();
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(BUTTON_FONT);
        logoutBtn.setPreferredSize(new Dimension(150, 50));
        logoutBtn.addActionListener(e -> handleLogout());
        receiptButtonPanel.add(logoutBtn);
        receiptPanel.add(receiptButtonPanel, BorderLayout.SOUTH);
        panel.add(receiptPanel, BorderLayout.CENTER);
        return panel;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem fontItem = new JMenuItem("Change Font Size");
        fontItem.addActionListener(e -> changeFontSize());
        JCheckBoxMenuItem beepItem = new JCheckBoxMenuItem("Enable Beep", beepEnabled);
        beepItem.addActionListener(e -> beepEnabled = beepItem.isSelected());
        JCheckBoxMenuItem themeItem = new JCheckBoxMenuItem("Dark Mode", darkMode);
        themeItem.addActionListener(e -> toggleTheme());
        settingsMenu.add(fontItem);
        settingsMenu.add(beepItem);
        settingsMenu.add(themeItem);
        menuBar.add(settingsMenu);
        return menuBar;
    }

    private void addItemToOrder(Menu item) {
        String qtyStr = JOptionPane.showInputDialog(this, "Quantity for " + item.getName() + ":", "Quantity", JOptionPane.QUESTION_MESSAGE);
        if (qtyStr != null) {
            try {
                int qty = Integer.parseInt(qtyStr.trim());
                if (qty > 0) {
                    currentOrder.addItem(item.getName(), qty, item.getPrice());
                    lastItemName = item.getName();
                    lastItemQty = qty;
                    lastItemPrice = item.getPrice();
                    displayOrderSummary();
                    showInfo(qty + " " + item.getName() + " added.");
                } else {
                    showError("Invalid quantity.");
                }
            } catch (NumberFormatException e) {
                showError("Invalid quantity.");
            }
        }
    }

    private void displayOrderSummary() {
        orderTableModel.setRowCount(0);
        for (String orderItem : currentOrder.getOrderItems()) {
            String[] parts = orderItem.split("\\|");
            int qty = Integer.parseInt(parts[0]);
            String itemName = parts[1];
            int price = getPrice(itemName);
            int total = qty * price;
            orderTableModel.addRow(new Object[]{itemName, qty, price, total, "Remove"});
        }
        orderTableModel.addRow(new Object[]{"Total", "", "", currentOrder.getTotalAmount(), ""});
    }

    private void clearOrder() {
        int confirm = JOptionPane.showConfirmDialog(this, "Clear the entire order?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            currentOrder = new Order();
            displayOrderSummary();
        }
    }

    public boolean handlePayment() {
        if (currentOrder == null || currentOrder.getOrderItems().isEmpty() || currentOrder.getTotalAmount() <= 0) {
            showError("No items in the order.");
            return false;
        }
        String customerName = JOptionPane.showInputDialog(this, "Customer name (optional):", "Customer", JOptionPane.QUESTION_MESSAGE);
        if (customerName != null && !customerName.trim().isEmpty()) {
            customers.put(customerName, JOptionPane.showInputDialog(this, "Phone number:", "Phone", JOptionPane.QUESTION_MESSAGE));
        }
        String[] options = {"Cash", "Card"};
        int paymentType = JOptionPane.showOptionDialog(this, "Select payment method:", "Payment", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (paymentType == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (paymentType == 0) {
            int totalAmount = currentOrder.getTotalAmount();
            int totalPaid = 0;
            while (totalPaid < totalAmount) {
                String cashStr = JOptionPane.showInputDialog(this, "Enter cash: ₱" + (totalAmount - totalPaid) + " more needed.", "Cash Payment", JOptionPane.QUESTION_MESSAGE);
                if (cashStr == null) {
                    showError("Payment canceled.");
                    return false;
                }
                try {
                    int cash = Integer.parseInt(cashStr.trim());
                    if (cash <= 0) {
                        showError("Amount must be positive.");
                        continue;
                    }
                    totalPaid += cash;
                } catch (NumberFormatException e) {
                    showError("Invalid amount.");
                }
            }
            currentOrder.setCash(totalPaid);
            showInfo("Change: ₱" + (totalPaid - totalAmount));
        } else {
            currentOrder.setCash(currentOrder.getTotalAmount());
            showInfo("Card payment processed.");
        }
        totalSales += currentOrder.getTotalAmount();
        saveDailySales();
        if (beepEnabled) {
            playPaymentSound();
        }
        int id = orderCounter++;
        currentOrder.setOrderId(id);
        orderQueue.enqueue(currentOrder, "Order #" + id + " - ₱" + currentOrder.getTotalAmount());
        printReceipt();
        saveOrderCounter();
        currentOrder = new Order();
        displayOrderSummary();
        return true;
    }

    public void printReceipt() {
        if (currentOrder.getTotalAmount() > 0 && currentOrder.getCash() <= 0) {
            showError("Payment not completed.");
            return;
        }
        double discountedTotal = currentOrder.getTotalAmount() * 0.95;
        double subtotal = discountedTotal / 1.12;
        double vatAmount = discountedTotal - subtotal;
        int orderNumber = currentOrder.getOrderId() > 0 ? currentOrder.getOrderId() : orderCounter++;
        StringBuilder receipt = new StringBuilder();
        receipt.append("==========================================\n");
        receipt.append("          Gustopia\n");
        receipt.append("          123 Main Street, City\n");
        receipt.append("          Tel: (123) 456-7890\n");
        receipt.append("==========================================\n");
        receipt.append("Date: ").append(LocalDateTime.now().format(formatter)).append("\n");
        receipt.append("Order #: ").append(orderNumber).append("\n");
        receipt.append("==========================================\n");
        receipt.append("Item                     Qty   Unit Price   Total\n");
        receipt.append("------------------------------------------\n");
        for (String orderItem : currentOrder.getOrderItems()) {
            String[] parts = orderItem.split("\\|");
            int qty = Integer.parseInt(parts[0]);
            String itemName = parts[1];
            int unitPrice = getPrice(itemName);
            int totalPrice = unitPrice * qty;
            receipt.append(String.format("%-20s %3d %6d %6d%n", itemName, qty, unitPrice, totalPrice));
        }
        receipt.append("------------------------------------------\n");
        receipt.append(String.format("Subtotal: %28.2f%n", subtotal));
        receipt.append(String.format("VAT (12%%): %26.2f%n", vatAmount));
        receipt.append(String.format("Discount (5%%): %22.2f%n", currentOrder.getTotalAmount() * 0.05));
        receipt.append(String.format("Total: %30.2f%n", discountedTotal));
        receipt.append(String.format("Cash Tendered: %23d%n", currentOrder.getCash()));
        receipt.append(String.format("Change: %29d%n", currentOrder.getCash() - currentOrder.getTotalAmount()));
        receipt.append("==========================================\n");
        receipt.append("Thank you for dining with us!\n");
        receipt.append("==========================================\n");
        receiptArea.setText(receipt.toString());
        if (beepEnabled) {
            playReceiptSound();
        }
        flashReceipt();
        try (PrintWriter out = new PrintWriter(new FileWriter("orders.txt", true))) {
            out.println("Order #" + orderNumber + " - " + LocalDateTime.now().format(formatter));
            out.println(receipt.toString());
            out.println("==========================================\n\n");
        } catch (IOException e) {
            showError("Error saving receipt.");
        }
    }

    private void saveReceiptToFile() {
        if (receiptArea.getText().isEmpty()) {
            showError("No receipt to save.");
            return;
        }
        try {
            String fileName = "receipt_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
            try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
                out.println(receiptArea.getText());
            }
            showInfo("Receipt saved to " + fileName);
        } catch (IOException e) {
            showError("Error saving receipt: " + e.getMessage());
        }
    }

    private void shareReceipt() {
        String contact = JOptionPane.showInputDialog(this, "Enter phone number or email to share receipt:", "Share Receipt", JOptionPane.QUESTION_MESSAGE);
        if (contact != null && !contact.trim().isEmpty()) {
            showInfo("Receipt shared to " + contact + ".(Simulated)");
        }
    }

    private void serveOrder() {
        if (orderQueue.isEmpty()) {
            showInfo("No orders in queue.");
            return;
        }
        int result = JOptionPane.showConfirmDialog(this, "Serve next order?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            orderQueue.dequeue();
            receiptArea.setText("");
        }
    }

    private void showReceiptHistory() {
        StringBuilder history = new StringBuilder();
        history.append("Total Sales: ₱").append(String.format("%.2f", totalSales)).append("\n");
        history.append("==========================================\n");
        history.append("Sales Summary:\n");
        try (BufferedReader br = new BufferedReader(new FileReader("orders.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                history.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            history.append("No history.");
        } catch (IOException e) {
            history.append("Error reading history.");
        }
        JDialog dialog = new JDialog(this, "Sales History", true);
        JTextArea area = new JTextArea(history.toString());
        area.setEditable(false);
        dialog.add(new JScrollPane(area));
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void handleLogout() {
        String message = "Total Sales Today: ₱" + String.format("%.2f", totalSales) + "\n\n";
        message += "Session Summary:\n";
        message += "- Orders processed: " + orderCounter + "\n";
        message += "- Thank you for your service!";

        JOptionPane.showMessageDialog(this, message, "Daily Summary", JOptionPane.INFORMATION_MESSAGE);

        if (backgroundMediaPlayer != null) {
            Platform.runLater(() -> {
                try {
                    backgroundMediaPlayer.stop();
                    backgroundMediaPlayer.dispose();
                    backgroundMediaPlayer = null;
                    logError("Background music stopped on logout");
                } catch (Exception ex) {
                    logError("Error stopping background music: " + ex.getMessage());
                }
            });
        }

        this.dispose();
        SwingUtilities.invokeLater(() -> {
            JFrame newFrame = new RAndom();
        });
    }

    private void changeFontSize() {
        String sizeStr = JOptionPane.showInputDialog(this, "Enter font size (10-36):", "Font Size", JOptionPane.QUESTION_MESSAGE);
        if (sizeStr != null) {
            try {
                int size = Integer.parseInt(sizeStr.trim());
                if (size >= 10 && size <= 36) {
                    Font newFont = new Font("Roboto", Font.PLAIN, size);
                    orderTable.setFont(newFont);
                    receiptArea.setFont(newFont);
                    showInfo("Font size updated.");
                } else {
                    showError("Size must be between 10 and 36.");
                }
            } catch (NumberFormatException e) {
                showError("Invalid size.");
            }
        }
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        Color bg = darkMode ? Color.DARK_GRAY : Color.WHITE;
        Color fg = darkMode ? Color.WHITE : Color.BLACK;
        getContentPane().setBackground(bg);
        orderTable.setBackground(bg);
        orderTable.setForeground(fg);
        receiptArea.setBackground(bg);
        receiptArea.setForeground(fg);
        showInfo("Theme toggled.");
    }

    private void playBackgroundMusic() {
        System.out.println("playBackgroundMusic called");
        logError("playBackgroundMusic called");
        try {
            Platform.runLater(() -> {
                System.out.println("Inside Platform.runLater for background music");
                logError("Inside Platform.runLater for background music");
                try {
                    File soundFile = new File("src/background music.mp3");
                    System.out.println("Background file exists: " + soundFile.exists() + " -> " + soundFile.getAbsolutePath());
                    if (soundFile.exists()) {
                        Media media = new Media(soundFile.toURI().toString());
                        media.setOnError(() -> logError("Media load error: " + media.getError()));
                        backgroundMediaPlayer = new MediaPlayer(media);
                        backgroundMediaPlayer.setOnError(() -> logError("MediaPlayer error: " + backgroundMediaPlayer.getError()));
                        backgroundMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                        backgroundMediaPlayer.play();
                        logError("Background music started: " + soundFile.getAbsolutePath());
                    } else {
                        logError("Background music not found: " + soundFile.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    logError("Background music failed: " + ex.getMessage());
                }
            });
        } catch (Throwable t) {
            logError("JavaFX Platform not initialized for background music: " + t.getMessage());
        }
    }

    private void playPaymentSound() {
        Platform.runLater(() -> {
            try {
                File soundFile = new File("src/cash purchase.mp3");
                if (soundFile.exists()) {
                    Media media = new Media(soundFile.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.play();
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            } catch (Exception ex) {
                Toolkit.getDefaultToolkit().beep();
            }
        });
    }

    private void playReceiptSound() {
        Platform.runLater(() -> {
            try {
                File soundFile = new File("src/receipt.mp3");
                if (soundFile.exists()) {
                    Media media = new Media(soundFile.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.play();
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            } catch (Exception ex) {
                Toolkit.getDefaultToolkit().beep();
            }
        });
    }

    private void flashReceipt() {
        Color old = receiptArea.getBackground();
        receiptArea.setBackground(Color.WHITE);
        javax.swing.Timer timer = new javax.swing.Timer(800, e -> receiptArea.setBackground(old));
        timer.setRepeats(false);
        timer.start();
    }

    private int getPrice(String itemName) {
        for (Category category : categories) {
            for (Menu item : category.getItems()) {
                if (item.getName().equalsIgnoreCase(itemName)) {
                    return item.getPrice();
                }
            }
        }
        return 0;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        logError(msg);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void logError(String msg) {
        try (PrintWriter out = new PrintWriter(new FileWriter("error_log.txt", true))) {
            out.println(LocalDateTime.now().format(formatter) + ": " + msg);
        } catch (Exception e) {
        }
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {

        private JButton button;
        private String label;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> {
                fireEditingStopped();
                int row = orderTable.getSelectedRow();
                if (row >= 0 && row < orderTableModel.getRowCount() - 1) {
                    String itemName = (String) orderTableModel.getValueAt(row, 0);
                    int qty = (Integer) orderTableModel.getValueAt(row, 1);
                    currentOrder.removeItem(itemName, qty, getPrice(itemName));
                    displayOrderSummary();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }

    public static void main(String[] args) {
        try {
            new javafx.embed.swing.JFXPanel();
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(RAndom::new);
    }
}
