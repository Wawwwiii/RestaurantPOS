import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;


public class Restaurant extends JFrame {
    private List<Category> categories;
    private Order currentOrder;
    private OrderQueue orderQueue = new OrderQueue();
    private JTextArea menuArea = new JTextArea(20, 40);
    private JTextArea orderSummaryArea = new JTextArea(10, 40);
    private JTextArea receiptArea = new JTextArea(20, 40);
    private JScrollPane receiptScroll;
    private static int orderCounter;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Using shared Order class (Order.java)

    public Restaurant() {
        categories = MenuInitializer.initializeMenu();
        currentOrder = new Order(); // Initialize with empty order
        loadOrderCounter();
        initGUI();
        displayMenu(); // Call to display menu on startup
    }

    private void loadOrderCounter() {
        try (BufferedReader br = new BufferedReader(new FileReader("order_counter.txt"))) {
            String line = br.readLine();
            if (line != null && !line.trim().isEmpty()) {
                orderCounter = Integer.parseInt(line.trim());
            } else {
                orderCounter = 1;
            }
        } catch (IOException | NumberFormatException e) {
            orderCounter = 1;
        }
    }

    private void saveOrderCounter() {
        try (PrintWriter out = new PrintWriter(new FileWriter("order_counter.txt"))) {
            out.println(orderCounter);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving order counter: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initGUI() {
        setTitle("Turo Turo ni Eliza - Restaurant Ordering System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximize window
        setLayout(new BorderLayout());

        // Menu panel
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.add(new JLabel("Menu:"), BorderLayout.NORTH);
        menuArea.setEditable(false);
        JScrollPane menuScroll = new JScrollPane(menuArea);
        menuPanel.add(menuScroll, BorderLayout.CENTER);

        JButton processOrderButton = new JButton("Process Order");
        processOrderButton.addActionListener(e -> processOrders());
        menuPanel.add(processOrderButton, BorderLayout.SOUTH);

        // Order summary panel
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.add(new JLabel("Order Summary:"), BorderLayout.NORTH);
        orderSummaryArea.setEditable(false);
        JScrollPane summaryScroll = new JScrollPane(orderSummaryArea);
        summaryPanel.add(summaryScroll, BorderLayout.CENTER);

        JButton handlePaymentButton = new JButton("Handle Payment");
        handlePaymentButton.addActionListener(e -> {
            boolean queued = handlePayment();
            if (queued) {
                printReceipt();
                saveOrderCounter();
                currentOrder = new Order(); // Reset order
                displayOrderSummary(); // Clear summary
            }
        });
        summaryPanel.add(handlePaymentButton, BorderLayout.SOUTH);

        // Queue and receipt panel
        JPanel rightPanel = new JPanel(new BorderLayout());

        JPanel queuePanel = new JPanel(new BorderLayout());
        queuePanel.add(new JLabel("Handle Payment:"), BorderLayout.NORTH);
        JList<String> queueList = new JList<>(orderQueue.getListModel());
        queueList.setFont(new Font("Monospaced", Font.PLAIN, 16));
        JScrollPane queueScroll = new JScrollPane(queueList);
        queueScroll.setPreferredSize(new java.awt.Dimension(420, 200));
        queuePanel.add(queueScroll, BorderLayout.CENTER);
        // small settings removed: orders are identified by Order# and beep occurs automatically
        JButton serveOrderButton = new JButton("Serve Next Order");
        serveOrderButton.addActionListener(e -> serveOrder());
        // Double-click to show order details
        queueList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = queueList.locationToIndex(e.getPoint());
                    Order selected = orderQueue.getOrderAt(idx);
                    if (selected != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Order ID: ").append(selected.getOrderId()).append("\n");
                        // No separate customer name; orders are identified by order ID
                        sb.append("Items:\n");
                        for (String orderItem : selected.getOrderItems()) {
                            String[] parts = orderItem.split("\\|");
                            sb.append(parts[0]).append(" x ").append(parts[1]).append("\n");
                        }
                        JOptionPane.showMessageDialog(Restaurant.this, sb.toString(), "Order Details", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
        // Context menu for queue actions
        JPopupMenu popup = new JPopupMenu();
        JMenuItem promoteItem = new JMenuItem("Promote to front");
        promoteItem.addActionListener(ev -> {
            int idx = queueList.getSelectedIndex();
            if (idx >= 0) {
                boolean success = orderQueue.promoteToFront(idx);
                if (success) JOptionPane.showMessageDialog(Restaurant.this, "Order promoted to front.", "Promoted", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        JMenuItem reprintItem = new JMenuItem("Reprint Receipt");
        reprintItem.addActionListener(ev -> {
            int idx = queueList.getSelectedIndex();
            Order selected = orderQueue.getOrderAt(idx);
            if (selected != null) {
                currentOrder = selected;
                printReceipt();
            }
        });
        popup.add(promoteItem);
        popup.add(reprintItem);
        queueList.setComponentPopupMenu(popup);
        queuePanel.add(serveOrderButton, BorderLayout.SOUTH);

        rightPanel.add(queuePanel, BorderLayout.NORTH);

        JPanel receiptPanel = new JPanel(new BorderLayout());
        receiptPanel.add(new JLabel("Receipt:"), BorderLayout.NORTH);
        receiptArea.setEditable(false);
        receiptScroll = new JScrollPane(receiptArea);
        receiptScroll.setPreferredSize(new java.awt.Dimension(420, 300));
        receiptPanel.add(receiptScroll, BorderLayout.CENTER);

        rightPanel.add(receiptPanel, BorderLayout.CENTER);

        // Split panes
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, menuPanel, summaryPanel);
        leftSplit.setDividerLocation(300);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        mainSplit.setDividerLocation(500);
        add(mainSplit, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }

    public void displayMenu() {
        StringBuilder menuText = new StringBuilder("==========================================\n");
        menuText.append("          Turo Turo ni Eliza\n");
        menuText.append("==========================================\n");
        for (int i = 0; i < categories.size(); i++) {
            menuText.append((i + 1)).append(". ").append(categories.get(i).getName()).append("\n");
        }
        menuText.append("==========================================\n");
        menuArea.setText(menuText.toString());
    }

    public void processOrders() {
        Map<String, Integer> orderQuantities = new HashMap<>();
        while (true) {
            // Select category via dialog
            String[] categoryOptions = categories.stream().map(Category::getName).toArray(String[]::new);
            String selectedCategoryName = (String) JOptionPane.showInputDialog(this, "Select a category:", "Category Selection",
                    JOptionPane.QUESTION_MESSAGE, null, categoryOptions, categoryOptions[0]);
            if (selectedCategoryName == null) break; // Cancel

            Category selectedCategory = categories.stream().filter(c -> c.getName().equals(selectedCategoryName)).findFirst().orElse(null);
            if (selectedCategory == null) continue;

            // Display items in category via dialog
            List<Menu> items = selectedCategory.getItems();
            String[] itemOptions = items.stream().map(Menu::toString).toArray(String[]::new);
            while (true) {
                String selectedItemStr = (String) JOptionPane.showInputDialog(this, "Select an item (or Cancel to finish category):", "Item Selection",
                        JOptionPane.QUESTION_MESSAGE, null, itemOptions, itemOptions[0]);
                if (selectedItemStr == null) break; // Cancel

                Menu selectedItem = items.stream().filter(i -> i.toString().equals(selectedItemStr)).findFirst().orElse(null);
                if (selectedItem == null) continue;

                // Ask for quantity via dialog
                String qtyStr = JOptionPane.showInputDialog(this, "How many " + selectedItem.getName() + " would you like?", "Quantity", JOptionPane.QUESTION_MESSAGE);
                if (qtyStr == null) continue;
                try {
                    int qty = Integer.parseInt(qtyStr.trim());
                    if (qty <= 0) {
                        JOptionPane.showMessageDialog(this, "Quantity must be positive.", "Error", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                    orderQuantities.compute(selectedItem.getName(), (k, v) -> (v == null) ? qty : v + qty);
                    JOptionPane.showMessageDialog(this, qty + " " + selectedItem.getName() + " added.");
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid quantity.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            // Ask if final order
            int result = JOptionPane.showConfirmDialog(this, "Is this your final order?", "Final Order", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) break;
        }

        // Set current order
        int prepTime = calculatePrepTime(orderQuantities);
        currentOrder = new Order();
        for (Map.Entry<String, Integer> e : orderQuantities.entrySet()) {
            currentOrder.addItem(e.getKey(), e.getValue(), getPrice(e.getKey()));
        }
        // Prep time is not used; do not set it on the order
        displayOrderSummary();
    }

    private int calculateTotal(Map<String, Integer> orderQuantities) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : orderQuantities.entrySet()) {
            total += getPrice(entry.getKey()) * entry.getValue();
        }
        return total;
    }

    private int calculatePrepTime(Map<String, Integer> orderQuantities) {
        int prepTime = 0;
        for (Map.Entry<String, Integer> entry : orderQuantities.entrySet()) {
            int index = getIndex(entry.getKey());
            if (index != -1) {
                prepTime += PREP_TIMES_MINUTES[index] * entry.getValue();
            }
        }
        return prepTime;
    }

    public void displayOrderSummary() {
        StringBuilder summary = new StringBuilder("Orders:\n");
        for (String orderItem : currentOrder.getOrderItems()) {
            String[] parts = orderItem.split("\\|");
            int qty = Integer.parseInt(parts[0]);
            String itemName = parts[1];
            summary.append("- ").append(qty).append(" ").append(itemName).append("\n");
        }
        summary.append("Total Amount: $").append(currentOrder.getTotalAmount()).append("\n");
        // Prep time removed from the order summary display
        orderSummaryArea.setText(summary.toString());
    }

    public boolean handlePayment() {
        int totalAmount = currentOrder.getTotalAmount();
        int totalPaid = 0;
        boolean canceledPayment = false;

        while (totalPaid < totalAmount) {
            String cashStr = JOptionPane.showInputDialog(this, "Enter cash: $" + (totalAmount - totalPaid) + " more needed.", "Payment", JOptionPane.QUESTION_MESSAGE);
            if (cashStr == null) {
                canceledPayment = true;
                break;
            }
            try {
                int cash = Integer.parseInt(cashStr.trim());
                totalPaid += cash;
                if (totalPaid < totalAmount) {
                    JOptionPane.showMessageDialog(this, "Still need $" + (totalAmount - totalPaid) + ". Please add more.", "Insufficient Funds", JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid integer amount.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // If payment was not completed and user cancelled, ask for confirmation before queuing
        if (canceledPayment && totalPaid < totalAmount) {
            try {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Payment incomplete. Amount entered: $" + totalPaid + " out of $" + totalAmount + ".\nProceed anyway?",
                        "Confirm Payment", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(this, "Order not queued. Please try payment again.", "Payment Canceled", JOptionPane.INFORMATION_MESSAGE);
                    return false; // Don't queue the order
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Payment confirmation failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        currentOrder.setCash(totalPaid);

        // Add to queue — auto-assign Order #ID and use it as label
        int id = orderCounter++;
        String customerName = "Order #" + id;
        currentOrder.setOrderId(id);
        orderQueue.enqueue(currentOrder, customerName + " - $" + currentOrder.getTotalAmount());
        return true;
    }

    public void printReceipt() {
        // Don't print a receipt if the order has a non-zero total but no cash was entered
        if (currentOrder.getTotalAmount() > 0 && currentOrder.getCash() <= 0) {
            JOptionPane.showMessageDialog(this, "Payment not completed. Receipt will not be printed.", "Payment Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double discountedTotal = currentOrder.getTotalAmount() * 0.95;
        double subtotal = discountedTotal / 1.12;
        double vatAmount = discountedTotal - subtotal;
        int orderNumber;
        if (currentOrder.getOrderId() > 0) {
            orderNumber = currentOrder.getOrderId();
        } else {
            orderNumber = orderCounter++;
            currentOrder.setOrderId(orderNumber);
        }

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
        receipt.append(String.format("Change: %29d%n", currentOrder.getChange()));
        receipt.append("==========================================\n");
        // Removed estimated prep time from receipt
        receipt.append("Thank you for dining with us!\n");
        receipt.append("No refunds or exchanges. Keep receipt.\n");
        receipt.append("==========================================\n");
        receiptArea.setText(receipt.toString());
        // Auto-adjust receipt font
        adjustReceiptFontForRestaurant();
        // Play beep on receipt (if enabled)
        playBeep();
        // Ask confirmation and clear receipt, then serve next order after 5 seconds
        javax.swing.Timer t = new javax.swing.Timer(5000, ev -> {
            int result = JOptionPane.showConfirmDialog(this, "Serve next order? This will clear the receipt.", "Serve Next Order", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                receiptArea.setText("");
                serveOrder();
            }
            ((javax.swing.Timer) ev.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();

        // Save to file
        try (FileWriter fw = new FileWriter("orders.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println("Order #" + orderNumber + " - " + LocalDateTime.now().format(formatter));
            out.println(receipt.toString());
            out.println("==========================================\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving receipt to file: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void serveOrder() {
        if (orderQueue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No orders in queue.", "Queue Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Ask confirmation: serve next customer now, or wait
        int result = JOptionPane.showConfirmDialog(this, "Serve next customer now?\nSelect No to wait.", "Serve Next Order", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            orderQueue.dequeue();
            if (receiptArea != null && !receiptArea.getText().trim().isEmpty()) {
                receiptArea.setText("");
            }
        }
    }

    private void playBeep() {
        try {
            File soundFile = new File("ding.wav");
            if (soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception ex) {
            Toolkit.getDefaultToolkit().beep();
        }
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

    private int getIndex(String itemName) {
        // Assuming PREP_TIMES_MINUTES is defined (from original RAndom.java)
        // For simplicity, map to a static array or calculate based on menu
        // Here, I'll assume a simple mapping; adjust as needed
        String[] allItems = {"Pizza", "Fried Chicken", "Spaghetti", "Fries", "Palabok", "Burger", "Coke", "Sprite", "Royal", "Pepsi", "Water", "Coffee", "Latte", "Cappuccino", "Pancakes", "Waffles", "Eggs Benedict", "Ice Cream", "Cake", "Pie"};
        for (int i = 0; i < allItems.length; i++) {
            if (allItems[i].equalsIgnoreCase(itemName)) {
                return i;
            }
        }
        return -1;
    }

    // Static arrays for prep times (from original RAndom.java)
    private static final int[] PREP_TIMES_MINUTES = {15, 10, 12, 5, 10, 8, 1, 1, 1, 1, 1, 3, 5, 5, 10, 12, 15, 2, 5, 4};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Restaurant::new);
    }
    private void adjustReceiptFontForRestaurant() {
        SwingUtilities.invokeLater(() -> {
            String text = receiptArea.getText();
            if (text == null || text.isEmpty()) return;
            int lines = text.split("\\n").length;
            if (lines <= 0) lines = 1;
            int vpHeight = receiptScroll.getViewport().getHeight();
            if (vpHeight <= 0) vpHeight = receiptArea.getHeight();
            int maxFontSize = 36;
            int minFontSize = 10;
            for (int size = maxFontSize; size >= minFontSize; size--) {
                Font f = new Font("Monospaced", receiptArea.getFont().getStyle(), size);
                BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = img.createGraphics();
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics();
                int fontHeight = fm.getHeight();
                g2.dispose();
                if ((long) fontHeight * lines <= vpHeight - 8) {
                    receiptArea.setFont(f);
                    break;
                }
            }
        });
    }
}
