import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Order {
    // Use LinkedHashMap to preserve insertion order and allow easy access to quantities
    private final LinkedHashMap<String, Integer> items;
    private int totalAmount;
    private int cash;
    private int change;
    private String customerName; // optional customer name for GUI/queue
    private int estimatedPrepTime; // in minutes
    private int orderId = -1; // optional order id assigned when queued or printed


    public Order() {
        this.items = new LinkedHashMap<>();
        this.totalAmount = 0;
        this.cash = 0;
        this.change = 0;
        this.customerName = "";
        this.estimatedPrepTime = 0;
    }

    public void addItem(String item, int qty, int price) {
        if (qty <= 0) return;
        int existing = items.getOrDefault(item, 0);
        items.put(item, existing + qty);
        totalAmount += price * qty;
    }

    /**
     * Remove an item from the order with its associated price.
     * Call this when user clicks Undo/Cancel to remove an item.
     */
    public void removeItem(String item, int qty, int price) {
        if (items.isEmpty()) return;
        
        int currentQty = items.getOrDefault(item, 0);
        if (currentQty >= qty) {
            if (currentQty == qty) {
                items.remove(item);
            } else {
                items.put(item, currentQty - qty);
            }
            totalAmount -= price * qty;
            if (totalAmount < 0) totalAmount = 0;
        }
    }

    /**
     * Return a basic map of ordered items to quantity to simplify usage in client code.
     */
    public Map<String, Integer> getOrderMap() {
        return new LinkedHashMap<>(items);
    }

    public List<String> getOrderItems() {
        List<String> formatted = new ArrayList<>();
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            formatted.add(e.getValue() + "|" + e.getKey());
        }
        return formatted;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setCash(double cash) {
        this.cash = (int) cash;
        this.change = (int) (cash - totalAmount);
    }

    public int getCash() {
    
        return cash;
    }

    public int getChange() {
        return change;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
        totalAmount = 0;
        cash = 0;
        change = 0;
        customerName = "";
        estimatedPrepTime = 0;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setEstimatedPrepTime(int minutes) {
        this.estimatedPrepTime = minutes;
    }

    public int getEstimatedPrepTime() {
        return estimatedPrepTime;
    }

    public void setOrderId(int id) { this.orderId = id; }

    public int getOrderId() { return orderId; }
}