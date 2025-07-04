import java.util.*;

abstract class Product {
    protected String name;
    protected double price;
    protected int quantity;

    public Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void reduceQuantity(int amount) {
        this.quantity -= amount;
    }

    public boolean isExpired() {
        return false;
    }

    public boolean requiresShipping() {
        return false;
    }

    public double getWeight() {
        return 0;
    }
}

interface Shippable {
    String getName();
    double getWeight();
}

class ExpirableProduct extends Product {
    private boolean expired;

    public ExpirableProduct(String name, double price, int quantity, boolean expired) {
        super(name, price, quantity);
        this.expired = expired;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}

class ShippableProduct extends Product implements Shippable {
    private double weight;

    public ShippableProduct(String name, double price, int quantity, double weight) {
        super(name, price, quantity);
        this.weight = weight;
    }

    @Override
    public boolean requiresShipping() {
        return true;
    }

    @Override
    public double getWeight() {
        return weight;
    }
}

class ExpirableShippableProduct extends Product implements Shippable {
    private boolean expired;
    private double weight;

    public ExpirableShippableProduct(String name, double price, int quantity, boolean expired, double weight) {
        super(name, price, quantity);
        this.expired = expired;
        this.weight = weight;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public boolean requiresShipping() {
        return true;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}

class CartItem {
    Product product;
    int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
}

class Cart {
    List<CartItem> items = new ArrayList<>();

    public void add(Product product, int quantity) {
        if (quantity > product.getQuantity()) {
            System.out.println("Error: Not enough stock for " + product.getName());
            return;
        }
        items.add(new CartItem(product, quantity));
    }

    public List<CartItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}

class Customer {
    private String name;
    private double balance;

    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public void deductBalance(double amount) {
        balance -= amount;
    }
}

class ShippingService {
    public static void ship(List<Shippable> items) {
        System.out.println("** Shipment notice **");
        
        Map<String, Integer> itemCounts = new HashMap<>();
        Map<String, Double> itemWeights = new HashMap<>();
        
        for (Shippable item : items) {
            String name = item.getName();
            itemCounts.put(name, itemCounts.getOrDefault(name, 0) + 1);
            itemWeights.put(name, item.getWeight());
        }
        
        double totalWeight = 0;
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String name = entry.getKey();
            int count = entry.getValue();
            double weight = itemWeights.get(name);
            
            System.out.printf("%dx %s %.0fg\n", count, name, weight * 1000);
            totalWeight += weight * count;
        }
        
        System.out.printf("Total package weight %.1fkg\n", totalWeight);
    }
}

class CheckoutService {
    public static void checkout(Customer customer, Cart cart) {
        if (cart.isEmpty()) {
            System.out.println("Error: Cart is empty");
            return;
        }

        double subtotal = 0;
        List<Shippable> shippableItems = new ArrayList<>();

        // Validate all items first
        for (CartItem item : cart.getItems()) {
            Product product = item.product;
            int qty = item.quantity;

            if (product.getQuantity() < qty) {
                System.out.println("Error: " + product.getName() + " is out of stock");
                return;
            }

            if (product.isExpired()) {
                System.out.println("Error: " + product.getName() + " is expired");
                return;
            }

            subtotal += product.getPrice() * qty;

            if (product.requiresShipping()) {
                for (int i = 0; i < qty; i++) {
                    shippableItems.add((Shippable) product);
                }
            }
        }

        double shippingFee = shippableItems.isEmpty() ? 0 : 30;
        double totalAmount = subtotal + shippingFee;

        if (customer.getBalance() < totalAmount) {
            System.out.println("Error: Insufficient balance");
            return;
        }

        // Process the order
        for (CartItem item : cart.getItems()) {
            item.product.reduceQuantity(item.quantity);
        }
        customer.deductBalance(totalAmount);

        if (!shippableItems.isEmpty()) {
            ShippingService.ship(shippableItems);
        }

        System.out.println("** Checkout receipt **");
        for (CartItem item : cart.getItems()) {
            System.out.printf("%dx %s %.0f\n", 
                item.quantity, 
                item.product.getName(), 
                item.product.getPrice() * item.quantity);
        }
        System.out.println("----------------------");
        System.out.printf("Subtotal %.0f\n", subtotal);
        System.out.printf("Shipping %.0f\n", shippingFee);
        System.out.printf("Amount %.0f\n", totalAmount);
        System.out.printf("Customer balance %.0f\n", customer.getBalance());
    }
}

public class App {
    public static void main(String[] args) {
        Product cheese = new ExpirableShippableProduct("Cheese", 100, 5, false, 0.2);
        Product biscuits = new ExpirableProduct("Biscuits", 150, 3, false);
        Product tv = new ShippableProduct("TV", 500, 2, 2.5);
        Product scratchCard = new Product("ScratchCard", 50, 10) {};

        Customer customer = new Customer("Ahmed", 1000);
        Cart cart = new Cart();

        cart.add(cheese, 2);
        cart.add(biscuits, 1);
        cart.add(scratchCard, 1);

        CheckoutService.checkout(customer, cart);
        
        System.out.println("\n--- Testing edge cases ---");
        
        // Test empty cart
        Cart emptyCart = new Cart();
        CheckoutService.checkout(customer, emptyCart);
        
        Customer poorCustomer = new Customer("Poor Customer", 50);
        Cart expensiveCart = new Cart();
        expensiveCart.add(tv, 1);
        CheckoutService.checkout(poorCustomer, expensiveCart);
        
        Product expiredCheese = new ExpirableShippableProduct("Expired Cheese", 100, 5, true, 0.2);
        Cart expiredCart = new Cart();
        expiredCart.add(expiredCheese, 1);
        CheckoutService.checkout(customer, expiredCart);
    }
}