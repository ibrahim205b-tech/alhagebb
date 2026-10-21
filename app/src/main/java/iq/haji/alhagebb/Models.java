package iq.haji.alhagebb;

public final class Models {
    private Models() {}

    public static class Category {
        public long id;
        public String name;
        public int sortOrder;
        public boolean active;
    }

    public static class Dish {
        public long id;
        public long categoryId;
        public String name;
        public long price;
        public boolean active;
    }

    public static class Driver {
        public long id;
        public String name;
        public String phone;
        public boolean active;
    }

    public static class CartLine {
        public long dishId;
        public String name;
        public String categoryName;
        public long unitPrice;
        public int qty;

        public long total() {
            return unitPrice * qty;
        }
    }

    public static class OrderRow {
        public long id;
        public long createdAt;
        public String dayKey;
        public String phone;
        public String address;
        public String driverName;
        public long subtotal;
        public long discount;
        public long deliveryFee;
        public long total;
        public boolean cancelled;
        public String cancelReason;
        public long cancelledAt;
    }

    public static class OrderItem {
        public long id;
        public long orderId;
        public long dishId;
        public String dishName;
        public String categoryName;
        public long unitPrice;
        public int qty;
        public long lineTotal;
    }
}
