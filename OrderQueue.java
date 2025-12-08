import java.util.LinkedList;
import java.util.Queue;
import javax.swing.DefaultListModel;

/**
 * A small helper for order queue logic to centralize queue operations and the associated ListModel.
 * This keeps GUI classes simpler and provides a single place to update queue behavior.
 */
public final class OrderQueue {
    private final Queue<Order> queue = new LinkedList<>();
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    public OrderQueue() {}

    /**
     * Enqueue an order and add a display label to the list model. The GUI decides how to format the label.
     */
    public void enqueue(Order order, String displayLabel) {
        queue.offer(order);
        listModel.addElement(displayLabel);
    }

    /** Dequeues the next order and removes the first display element. */
    public Order dequeue() {
        if (queue.isEmpty()) return null;
        Order o = queue.poll();
        if (!listModel.isEmpty()) listModel.remove(0);
        return o;
    }

    /**
     * Returns the order at a given index in the queue (0-based). Null if index invalid.
     */
    public Order getOrderAt(int index) {
        if (index < 0 || index >= queue.size()) return null;
        if (queue instanceof LinkedList) {
            return ((LinkedList<Order>) queue).get(index);
        }
        int i = 0;
        for (Order o : queue) {
            if (i++ == index) return o;
        }
        return null;
    }

    /**
     * Promote the order at index to the front of the queue. Returns true if promoted.
     */
    public boolean promoteToFront(int index) {
        if (!(queue instanceof LinkedList)) return false;
        LinkedList<Order> list = (LinkedList<Order>) queue;
        if (index < 0 || index >= list.size()) return false;
        Order o = list.remove(index);
        list.addFirst(o);
        // update the listModel strings accordingly (move the string too)
        String label = listModel.get(index);
        listModel.remove(index);
        listModel.add(0, label);
        return true;
    }

    public boolean isEmpty() { return queue.isEmpty(); }

    public int size() { return queue.size(); }

    public DefaultListModel<String> getListModel() { return listModel; }
}
