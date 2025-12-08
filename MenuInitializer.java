package projectjava;

import java.util.ArrayList;
import java.util.List;

public class MenuInitializer {

    public static List<Category> initializeMenu() {
        List<Category> categories = new ArrayList<>();

        Category breakfast = new Category("Breakfast");
        breakfast.addItem(new Menu("English Breakfast", 100));
        breakfast.addItem(new Menu("Hawaiian Breakfast", 110));
        breakfast.addItem(new Menu("Bacon & Eggs", 90));
        breakfast.addItem(new Menu("Ham & Cheese Breakfast", 95));
        breakfast.addItem(new Menu("Breakfast Burrito", 85));
        breakfast.addItem(new Menu("Mediterranean Omelette", 105));
        breakfast.addItem(new Menu("Spanish Omelette", 100));
        breakfast.addItem(new Menu("Eggs & Cheese Omelette", 80));
        categories.add(breakfast);

        Category beverages = new Category("Beverages");
        beverages.addItem(new Menu("Water", 15));
        beverages.addItem(new Menu("Blue Lemonade", 25));
        beverages.addItem(new Menu("Lemonade", 20));
        beverages.addItem(new Menu("Strawberry Lemonade", 30));
        beverages.addItem(new Menu("Iced Tea", 25));
        beverages.addItem(new Menu("Mango Juice", 35));
        beverages.addItem(new Menu("Pineapple Juice", 35));
        beverages.addItem(new Menu("Cranberry Juice", 40));
        categories.add(beverages);

        Category mainCourse = new Category("Main Course");
        mainCourse.addItem(new Menu("Caesar Salad", 80));
        mainCourse.addItem(new Menu("Greek Salad", 85));
        mainCourse.addItem(new Menu("Kale Salad", 75));
        mainCourse.addItem(new Menu("Vegan Salad", 70));
        mainCourse.addItem(new Menu("Chicken Salad", 90));
        mainCourse.addItem(new Menu("BBQ Pork Salad", 95));
        mainCourse.addItem(new Menu("Crispy Salad", 85));
        mainCourse.addItem(new Menu("Grilled Chicken", 120));
        categories.add(mainCourse);

        Category desserts = new Category("Desserts");
        desserts.addItem(new Menu("Strawberry Cake", 50));
        desserts.addItem(new Menu("Cheesecake", 55));
        desserts.addItem(new Menu("Lemon Cake", 50));
        desserts.addItem(new Menu("Carrot Cake", 60));
        desserts.addItem(new Menu("Vanilla Cake", 45));
        desserts.addItem(new Menu("Chocolate Cake", 55));
        desserts.addItem(new Menu("Brownie", 40));
        desserts.addItem(new Menu("Tiramisu", 65));
        categories.add(desserts);

        return categories;
    }
}
