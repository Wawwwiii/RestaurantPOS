package projectjava;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private  final String name;
    private final List<Menu> items;

    public Category(String name) {
        this.name = name;
        this.items = new ArrayList<>();
    }

    public void addItem(Menu item) {
        items.add(item);
    }

    public List<Menu> getItems() {
        return items;
    }

    public String getName() {
        return name;
    }
}
