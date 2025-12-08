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
    private JTextField searchField;
    private JTabbedPane menuTabbedPane;
    private static int orderCounter;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private boolean beepEnabled = true;
    private boolean darkMode = false;
    private Clip backgroundMusicClip;
    private Map<String, String> customers = new HashMap<>();
    private Timer backupTimer;
    private double totalSales = 0.0;

    public RAndom() {
        this.loadOrderCounter();
        this.initGUI();
        this.startBackupTimer();
        this.playBackgroundMusic();
        this.setVisible(true);
    }

    private boolean loginCashier() {
        String username = JOptionPane.showInputDialog(null, "Enter Cashier Username:", "Cashier Login", JOptionPane.QUESTION_MESSAGE);
        if (username != null && !username.trim().isEmpty()) {
            String password = JOptionPane.showInputDialog(null, "Enter Password:", "Cashier Login", JOptionPane.QUESTION_MESSAGE);
            if (password != null && password.equals("admin")) {
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "Invalid login.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else {
            return false;
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

    private void startBackupTimer() {
        backupTimer = new Timer(600000, e -> backupData());
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
        setTitle("Turo Turo ni Eliza - Advanced Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());
        add(createMenuPanel(), BorderLayout.WEST);
        add(createOrderPanel(), BorderLayout.CENTER);
        add(createRightPanel(), BorderLayout.EAST);
        setJMenuBar(createMenuBar());
        pack();
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(MENU_BG);
        panel.setPreferredSize(new Dimension(450, 600));
        searchField = new JTextField();
        searchField.setFont(BUTTON_FONT);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterMenu(); }
            public void removeUpdate(DocumentEvent e) { filterMenu(); }
            public void insertUpdate(DocumentEvent e) { filterMenu(); }
        });
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(new JLabel("🔍 Search:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.NORTH);
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

    private void filterMenu() {
        String query = searchField.getText().toLowerCase();
        for (int i = 0; i < menuTabbedPane.getTabCount(); i++) {
            JPanel tabPanel = (JPanel) menuTabbedPane.getComponentAt(i);
            Component[] components = tabPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JButton btn) {
                    String text = btn.getText().toLowerCase();
                    btn.setVisible(text.contains(query));
                }
            }
            tabPanel.revalidate();
            tabPanel.repaint();
        }
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
        JButton saveBtn = new JButton("Save Receipt");
        saveBtn.setFont(BUTTON_FONT);
        saveBtn.setPreferredSize(new Dimension(150, 50));
        saveBtn.addActionListener(e -> saveReceiptToFile());
        JButton shareBtn = new JButton("Share Receipt");
        shareBtn.setFont(BUTTON_FONT);
        shareBtn.setPreferredSize(new Dimension(150, 50));
        shareBtn.addActionListener(e -> shareReceipt());
        receiptButtonPanel.add(saveBtn);
        receiptButtonPanel.add(shareBtn);
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
        if (paymentType == JOptionPane.CLOSED_OPTION) return false;
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
        if (beepEnabled) playPaymentSound();
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
        receipt.append("          TURO TURO NI ELIZA\n");
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
        receipt.append("QR Code: [Simulated Digital Receipt]\n");
        receipt.append("==========================================\n");
        receiptArea.setText(receipt.toString());
        if (beepEnabled) playReceiptSound();
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
        Platform.runLater(() -> {
            try {
                File soundFile = new File("src/background music.mp3");
                if (soundFile.exists()) {
                    Media media = new Media(soundFile.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    mediaPlayer.play();
                }
            } catch (Exception ex) {
                // Silent fail
            }
        });
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
        Timer timer = new Timer(800, e -> receiptArea.setBackground(old));
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
        } catch (IOException e) {
            // Silent fail
        }
    }

    // Custom Button Renderer and Editor for Table
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
                // Handle button click
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
        SwingUtilities.invokeLater(RAndom::new);
    }
}

// Supporting Classes (add these at the end of the file)
class Category {
    private String name;
    private List<Menu> items = new ArrayList<>();
    public Category(String name) { this.name = name; }
    public String getName() { return name; }
    public List<Menu> getItems() { return items; }
    public void addItem(Menu item) { items.add(item); }
}

class Menu {
    private String name;
    private int price;
    public Menu(String name, int price) { this.name = name; this.price = price; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public String toString() { return name + " - $" + price; }
}

class Order {
    private List<String> orderItems = new ArrayList<>();
    private int totalAmount = 0;
    private int cash = 0;
    private int orderId = 0;
    public void addItem(String name, int qty, int price) {
        orderItems.add(qty + "|" + name);
        totalAmount += qty * price;
    }
    public void removeItem(String name, int qty, int price) {
        orderItems.remove(qty + "|" + name);
        totalAmount -= qty * price;
    }
    public List<String> getOrderItems() { return orderItems; }
    public int getTotalAmount() { return totalAmount; }
    public void setCash(int cash) { this.cash = cash; }
    public int getCash() { return cash; }
    public int getChange() { return cash - totalAmount; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int id) { this.orderId = id; }
}

class OrderQueue {
    private java.util.Queue<Order> queue = new java.util.LinkedList<>();
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    public void enqueue(Order order, String label) { queue.add(order); listModel.addElement(label); }
    public Order dequeue() { Order o = queue.poll(); if (o != null) listModel.remove(0); return o; }
    public boolean isEmpty() { return queue.isEmpty(); }
    public DefaultListModel<String> getListModel() { return listModel; }
    public Order getOrderAt(int index) { return (Order) queue.toArray()[index]; }
    public boolean promoteToFront(int index) { if (index > 0) { Order o = (Order) queue.toArray()[index]; queue.remove(o); queue.add(o); listModel.remove(index); listModel.add(0, listModel.get(index)); return true; } return false; }
}

class MenuInitializer {
    public static List<Category> initializeMenu() {
        List<Category> categories = new ArrayList<>();
        Category mains = new Category("Mains");
        mains.addItem(new Menu("Adobo", 100));
        mains.addItem(new Menu("Sinigang", 120));
        categories.add(mains);
        // Add more categories/items as needed
        return categories;
    }
}