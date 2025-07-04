import java.util.*;

interface Transportable {
    String getItemName();
    double getItemWeight();
}

class Item {
    protected String itemName;
    protected double itemCost;
    protected int stock;

    public Item(String itemName, double itemCost, int stock) {
        this.itemName = itemName;
        this.itemCost = itemCost;
        this.stock = stock;
    }

    public boolean isPastDue() {
        return false;
    }

    public boolean requiresTransport() {
        return false;
    }

    public String getItemName() {
        return itemName;
    }

    public double getItemCost() {
        return itemCost;
    }

    public int getStock() {
        return stock;
    }

    public void reduceStock(int amount) {
        this.stock -= amount;
    }
}

class PerishableItem extends Item {
    private boolean pastDue;

    public PerishableItem(String itemName, double itemCost, int stock, boolean pastDue) {
        super(itemName, itemCost, stock);
        this.pastDue = pastDue;
    }

    @Override
    public boolean isPastDue() {
        return pastDue;
    }
}

class TransportableItem extends Item implements Transportable {
    private double itemWeight;

    public TransportableItem(String itemName, double itemCost, int stock, double itemWeight) {
        super(itemName, itemCost, stock);
        this.itemWeight = itemWeight;
    }

    @Override
    public boolean requiresTransport() {
        return true;
    }

    @Override
    public double getItemWeight() {
        return itemWeight;
    }
}

class BasketEntry {
    Item item;
    int count;

    public BasketEntry(Item item, int count) {
        this.item = item;
        this.count = count;
    }
}

class Basket {
    List<BasketEntry> contents = new ArrayList<>();

    public void add(Item item, int count) {
        if (count > item.getStock()) {
            System.out.println("Quantity exceeds available stock.");
            return;
        }
        contents.add(new BasketEntry(item, count));
    }

    public List<BasketEntry> getContents() {
        return contents;
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public void empty() {
        contents.clear();
    }
}

class Client {
    String clientName;
    double funds;
    Basket basket = new Basket();

    public Client(String clientName, double funds) {
        this.clientName = clientName;
        this.funds = funds;
    }

    public Basket getBasket() {
        return basket;
    }

    public boolean pay(double amount) {
        if (funds >= amount) {
            funds -= amount;
            return true;
        }
        return false;
    }

    public double getFunds() {
        return funds;
    }
}

class DeliveryService {
    public static void deliver(List<Transportable> items) {
        if (items.isEmpty()) return;

        double totalWeight = 0;
        System.out.println("** Shipment notice **");
        Map<String, Integer> countMap = new HashMap<>();

        for (Transportable item : items) {
            countMap.put(item.getItemName(), countMap.getOrDefault(item.getItemName(), 0) + 1);
            totalWeight += item.getItemWeight();
        }

        for (String name : countMap.keySet()) {
            double weight = 0;
            for (Transportable item : items) {
                if (item.getItemName().equals(name)) {
                    weight = item.getItemWeight();
                    break;
                }
            }
            System.out.printf("%dx %-12s %.0fg\n", countMap.get(name), name, weight * 1000);
        }

        System.out.printf("Total package weight %.1fkg\n\n", totalWeight);
    }
}

class PaymentService {
    public static void processPayment(Client client) {
        Basket basket = client.getBasket();
        if (basket.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        double subtotal = 0;
        double shipping = 0;
        List<Transportable> transportables = new ArrayList<>();
        List<String> skippedItems = new ArrayList<>();
        List<BasketEntry> successfulItems = new ArrayList<>();

        for (BasketEntry entry : basket.getContents()) {
            Item i = entry.item;
            int qty = entry.count;

            if (i.isPastDue()) {
                skippedItems.add(i.getItemName() + " (expired)");
                continue;
            }

            if (qty > i.getStock()) {
                skippedItems.add(i.getItemName() + " (out of stock)");
                continue;
            }

            subtotal += i.getItemCost() * qty;
            successfulItems.add(entry);

            if (i.requiresTransport()) {
                TransportableItem ti = (TransportableItem) i;
                for (int j = 0; j < qty; j++) {
                    transportables.add(ti);
                }
                shipping += 10 * (transportables.size() > 0 ? 1 : 0);
            }
        }

        if (successfulItems.isEmpty()) {
            System.out.println("No valid products in cart to proceed with checkout.");
            return;
        }

        double total = subtotal + shipping;

        if (!client.pay(total)) {
            System.out.println("Insufficient balance.");
            return;
        }

        DeliveryService.deliver(transportables);

        System.out.println("** Checkout receipt **");
        for (BasketEntry entry : successfulItems) {
            System.out.printf("%dx %-12s %.0f\n", entry.count, entry.item.getItemName(), entry.item.getItemCost() * entry.count);
            entry.item.reduceStock(entry.count);
        }

        System.out.println("----------------------");
        System.out.printf("Subtotal         %.0f\n", subtotal);
        System.out.printf("Shipping         %.0f\n", shipping);
        System.out.printf("Amount           %.0f\n", total);
        System.out.printf("Balance left     %.0f\n", client.getFunds());

        if (!skippedItems.isEmpty()) {
            System.out.println("\n⚠️ Skipped items:");
            for (String msg : skippedItems) {
                System.out.println("- " + msg);
            }
        }

        basket.empty();
    }
}

public class App {
    public static void main(String[] args) {
        PerishableItem cheese = new PerishableItem("Cheese", 100, 10, false);
        PerishableItem biscuits = new PerishableItem("Biscuits", 150, 5, false);
        TransportableItem tv = new TransportableItem("TV", 5000, 2, 0.2);
        Item scratchCard = new Item("ScratchCard", 50, 20);
        PerishableItem expired = new PerishableItem("Expired", 200, 5, true);

        Client client = new Client("Ahmed", 10000);
        Client poorClient = new Client("Poor", 100);

        client.getBasket().add(cheese, 2);
        client.getBasket().add(biscuits, 1);
        client.getBasket().add(scratchCard, 1);
        PaymentService.processPayment(client);

        PaymentService.processPayment(new Client("Empty", 1000));

        poorClient.getBasket().add(tv, 1);
        PaymentService.processPayment(poorClient);

        client.getBasket().add(cheese, 20);
        PaymentService.processPayment(client);

        client.getBasket().add(expired, 1);
        PaymentService.processPayment(client);
    }
}