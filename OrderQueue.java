package projectjava;

import java.util.LinkedList;
import java.util.Queue;
import javax.swing.DefaultListModel;

public final class OrderQueue {

    private final Queue<Order> queue = new LinkedList<>();
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    public OrderQueue() {
    }

    public void enqueue(Order order, String displayLabel) {
        queue.offer(order);
        listModel.addElement(displayLabel);
    }

    public Order dequeue() {
        if (queue.isEmpty()) {
            return null;
        }
        Order o = queue.poll();
        if (!listModel.isEmpty()) {
            listModel.remove(0);
        }
        return o;
    }

    public Order getOrderAt(int index) {
        if (index < 0 || index >= queue.size()) {
            return null;
        }
        if (queue instanceof LinkedList) {
            return ((LinkedList<Order>) queue).get(index);
        }
        int i = 0;
        for (Order o : queue) {
            if (i++ == index) {
                return o;
            }
        }
        return null;
    }

    public boolean promoteToFront(int index) {
        if (!(queue instanceof LinkedList)) {
            return false;
        }
        LinkedList<Order> list = (LinkedList<Order>) queue;
        if (index < 0 || index >= list.size()) {
            return false;
        }
        Order o = list.remove(index);
        list.addFirst(o);
        String label = listModel.get(index);
        listModel.remove(index);
        listModel.add(0, label);
        return true;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public DefaultListModel<String> getListModel() {
        return listModel;
    }
}
