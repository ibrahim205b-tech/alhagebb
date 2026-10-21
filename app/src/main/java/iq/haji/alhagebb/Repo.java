package iq.haji.alhagebb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Repo {
    public static final String ACTOR_CASHIER = "جهاز الكاشير (الهوية غير معروفة)";
    public static final String ACTOR_MANAGER = "مدير (تم التحقق بالرمز)";

    private static Repo instance;
    private final Db helper;

    private Repo(Context ctx) {
        helper = new Db(ctx);
    }

    public static synchronized Repo get(Context ctx) {
        if (instance == null) instance = new Repo(ctx.getApplicationContext());
        return instance;
    }

    private SQLiteDatabase w() {
        return helper.getWritableDatabase();
    }

    // ---------- الإعدادات ----------
    public String getSetting(String key, String def) {
        try (Cursor c = w().rawQuery("SELECT value FROM settings WHERE key=?", new String[]{key})) {
            return c.moveToFirst() ? c.getString(0) : def;
        }
    }

    public void putSetting(String key, String value) {
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("value", value);
        w().insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String currentDayKey() {
        String k = getSetting("day_key", null);
        if (k == null) {
            k = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            putSetting("day_key", k);
        }
        return k;
    }

    // ---------- سجل التدقيق ----------
    public void audit(String action, String details, String actor) {
        ContentValues v = new ContentValues();
        v.put("ts", System.currentTimeMillis());
        v.put("action", action);
        v.put("details", details);
        v.put("actor", actor);
        w().insert("audit_log", null, v);
    }

    // ---------- الأقسام ----------
    private Models.Category readCat(Cursor c) {
        Models.Category x = new Models.Category();
        x.id = c.getLong(0);
        x.name = c.getString(1);
        x.sortOrder = c.getInt(2);
        x.active = c.getInt(3) == 1;
        return x;
    }

    public List<Models.Category> categories(boolean activeOnly) {
        List<Models.Category> out = new ArrayList<>();
        String sql = "SELECT id,name,sort_order,active FROM categories" +
                (activeOnly ? " WHERE active=1" : "") + " ORDER BY sort_order,id";
        try (Cursor c = w().rawQuery(sql, null)) {
            while (c.moveToNext()) out.add(readCat(c));
        }
        return out;
    }

    public long saveCategory(long id, String name, String actor) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("اسم القسم مطلوب");
        SQLiteDatabase db = w();
        try (Cursor c = db.rawQuery("SELECT id FROM categories WHERE name=? AND id<>?",
                new String[]{name, String.valueOf(id)})) {
            if (c.moveToFirst()) throw new IllegalArgumentException("اسم القسم مستخدم مسبقاً");
        }
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("name", name);
            if (id == 0) {
                int next = 0;
                try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(sort_order),-1)+1 FROM categories", null)) {
                    if (c.moveToFirst()) next = c.getInt(0);
                }
                v.put("sort_order", next);
                id = db.insertOrThrow("categories", null, v);
                audit("إضافة قسم", name, actor);
            } else {
                db.update("categories", v, "id=?", new String[]{String.valueOf(id)});
                audit("تعديل قسم", "#" + id + " → " + name, actor);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return id;
    }

    public void setCategoryActive(long id, boolean active, String actor) {
        ContentValues v = new ContentValues();
        v.put("active", active ? 1 : 0);
        w().update("categories", v, "id=?", new String[]{String.valueOf(id)});
        audit(active ? "تفعيل قسم" : "تعطيل قسم", "#" + id, actor);
    }

    public void moveCategory(long id, int dir, String actor) {
        List<Models.Category> list = categories(false);
        int idx = -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).id == id) idx = i;
        int to = idx + dir;
        if (idx < 0 || to < 0 || to >= list.size()) return;
        Models.Category tmp = list.get(idx);
        list.set(idx, list.get(to));
        list.set(to, tmp);
        SQLiteDatabase db = w();
        db.beginTransaction();
        try {
            for (int i = 0; i < list.size(); i++) {
                ContentValues v = new ContentValues();
                v.put("sort_order", i);
                db.update("categories", v, "id=?", new String[]{String.valueOf(list.get(i).id)});
            }
            audit("تغيير ترتيب الأقسام", tmp.name, actor);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public String categoryName(long id) {
        try (Cursor c = w().rawQuery("SELECT name FROM categories WHERE id=?", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }

    // ---------- الأطباق ----------
    private Models.Dish readDish(Cursor c) {
        Models.Dish d = new Models.Dish();
        d.id = c.getLong(0);
        d.categoryId = c.getLong(1);
        d.name = c.getString(2);
        d.price = c.getLong(3);
        d.active = c.getInt(4) == 1;
        return d;
    }

    public List<Models.Dish> dishes(long categoryId, boolean activeOnly) {
        List<Models.Dish> out = new ArrayList<>();
        String sql = "SELECT id,category_id,name,price,active FROM dishes WHERE category_id=?" +
                (activeOnly ? " AND active=1" : "") + " ORDER BY name";
        try (Cursor c = w().rawQuery(sql, new String[]{String.valueOf(categoryId)})) {
            while (c.moveToNext()) out.add(readDish(c));
        }
        return out;
    }

    public Models.Dish dish(long id) {
        try (Cursor c = w().rawQuery("SELECT id,category_id,name,price,active FROM dishes WHERE id=?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? readDish(c) : null;
        }
    }

    public long saveDish(long id, long categoryId, String name, long price, String actor) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("اسم الطبق مطلوب");
        if (price < 0) throw new IllegalArgumentException("السعر غير صالح");
        SQLiteDatabase db = w();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("category_id", categoryId);
            v.put("name", name);
            v.put("price", price);
            if (id == 0) {
                id = db.insertOrThrow("dishes", null, v);
                audit("إضافة طبق", name + " — " + price, actor);
            } else {
                Models.Dish old = dish(id);
                db.update("dishes", v, "id=?", new String[]{String.valueOf(id)});
                if (old != null && old.price != price) {
                    ContentValues h = new ContentValues();
                    h.put("dish_id", id);
                    h.put("dish_name", name);
                    h.put("old_price", old.price);
                    h.put("new_price", price);
                    h.put("changed_at", System.currentTimeMillis());
                    h.put("actor", actor);
                    db.insertOrThrow("price_history", null, h);
                    audit("تغيير سعر", name + ": " + old.price + " → " + price, actor);
                } else {
                    audit("تعديل طبق", "#" + id + " " + name, actor);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return id;
    }

    public void setDishActive(long id, boolean active, String actor) {
        ContentValues v = new ContentValues();
        v.put("active", active ? 1 : 0);
        w().update("dishes", v, "id=?", new String[]{String.valueOf(id)});
        audit(active ? "تفعيل طبق" : "تعطيل طبق", "#" + id, actor);
    }

    // ---------- السائقون (للقراءة في نموذج الطلب؛ الإدارة في الجزء الثاني) ----------
    public List<Models.Driver> drivers(boolean activeOnly) {
        List<Models.Driver> out = new ArrayList<>();
        String sql = "SELECT id,name,phone,active FROM drivers" +
                (activeOnly ? " WHERE active=1" : "") + " ORDER BY name";
        try (Cursor c = w().rawQuery(sql, null)) {
            while (c.moveToNext()) {
                Models.Driver d = new Models.Driver();
                d.id = c.getLong(0);
                d.name = c.getString(1);
                d.phone = c.getString(2);
                d.active = c.getInt(3) == 1;
                out.add(d);
            }
        }
        return out;
    }

    // ---------- الطلبات ----------
    /**
     * يحفظ الطلب وتفاصيله في معاملة واحدة. إذا وُجد طلب بنفس client_token
     * يُرجع رقمه دون إنشاء طلب جديد (منع تكرار الحفظ).
     */
    public long saveOrder(String token, List<Models.CartLine> lines, String phone, String address,
                          Long driverId, String driverName, long discount, long deliveryFee) {
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("الطلب فارغ");
        if (discount < 0 || deliveryFee < 0) throw new IllegalArgumentException("قيمة غير صالحة");
        long subtotal = 0;
        for (Models.CartLine l : lines) {
            if (l.qty <= 0 || l.unitPrice < 0) throw new IllegalArgumentException("كمية أو سعر غير صالح");
            subtotal += l.total();
        }
        if (discount > subtotal) throw new IllegalArgumentException("الخصم أكبر من المجموع");
        long total = subtotal - discount + deliveryFee;

        SQLiteDatabase db = w();
        db.beginTransaction();
        try {
            try (Cursor c = db.rawQuery("SELECT id FROM orders WHERE client_token=?", new String[]{token})) {
                if (c.moveToFirst()) {
                    long existing = c.getLong(0);
                    db.setTransactionSuccessful();
                    return existing;
                }
            }
            ContentValues o = new ContentValues();
            o.put("client_token", token);
            o.put("created_at", System.currentTimeMillis());
            o.put("day_key", currentDayKey());
            o.put("phone", phone == null ? "" : phone.trim());
            o.put("address", address == null ? "" : address.trim());
            if (driverId != null) o.put("driver_id", driverId);
            o.put("driver_name", driverName == null ? "" : driverName);
            o.put("subtotal", subtotal);
            o.put("discount", discount);
            o.put("delivery_fee", deliveryFee);
            o.put("total", total);
            long orderId = db.insertOrThrow("orders", null, o);
            for (Models.CartLine l : lines) {
                ContentValues i = new ContentValues();
                i.put("order_id", orderId);
                i.put("dish_id", l.dishId);
                i.put("dish_name", l.name);
                i.put("category_name", l.categoryName);
                i.put("unit_price", l.unitPrice);
                i.put("qty", l.qty);
                i.put("line_total", l.total());
                db.insertOrThrow("order_items", null, i);
            }
            audit("إنشاء طلب", "#" + orderId + " المجموع " + total, ACTOR_CASHIER);
            db.setTransactionSuccessful();
            return orderId;
        } finally {
            db.endTransaction();
        }
    }

    private Models.OrderRow readOrder(Cursor c) {
        Models.OrderRow o = new Models.OrderRow();
        o.id = c.getLong(0);
        o.createdAt = c.getLong(1);
        o.dayKey = c.getString(2);
        o.phone = c.getString(3);
        o.address = c.getString(4);
        o.driverName = c.getString(5);
        o.subtotal = c.getLong(6);
        o.discount = c.getLong(7);
        o.deliveryFee = c.getLong(8);
        o.total = c.getLong(9);
        o.cancelled = c.getInt(10) == 1;
        o.cancelReason = c.getString(11);
        o.cancelledAt = c.getLong(12);
        return o;
    }

    private static final String ORDER_COLS =
            "id,created_at,day_key,phone,address,driver_name,subtotal,discount,delivery_fee,total," +
                    "cancelled,cancel_reason,cancelled_at";

    /** البحث برقم الطلب أو الهاتف أو التاريخ (yyyy-MM-dd). النص الفارغ يعرض آخر الطلبات. */
    public List<Models.OrderRow> searchOrders(String q) {
        List<Models.OrderRow> out = new ArrayList<>();
        q = q == null ? "" : q.trim();
        String sql;
        String[] args;
        if (q.isEmpty()) {
            sql = "SELECT " + ORDER_COLS + " FROM orders ORDER BY id DESC LIMIT 200";
            args = new String[0];
        } else {
            sql = "SELECT " + ORDER_COLS + " FROM orders WHERE CAST(id AS TEXT)=? OR phone LIKE ? OR " +
                    "strftime('%Y-%m-%d', created_at/1000, 'unixepoch', 'localtime')=? " +
                    "ORDER BY id DESC LIMIT 200";
            args = new String[]{q, "%" + q + "%", q};
        }
        try (Cursor c = w().rawQuery(sql, args)) {
            while (c.moveToNext()) out.add(readOrder(c));
        }
        return out;
    }

    public Models.OrderRow order(long id) {
        try (Cursor c = w().rawQuery("SELECT " + ORDER_COLS + " FROM orders WHERE id=?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? readOrder(c) : null;
        }
    }

    public List<Models.OrderItem> orderItems(long orderId) {
        List<Models.OrderItem> out = new ArrayList<>();
        try (Cursor c = w().rawQuery("SELECT id,order_id,dish_id,dish_name,category_name,unit_price,qty,line_total " +
                "FROM order_items WHERE order_id=? ORDER BY id", new String[]{String.valueOf(orderId)})) {
            while (c.moveToNext()) {
                Models.OrderItem i = new Models.OrderItem();
                i.id = c.getLong(0);
                i.orderId = c.getLong(1);
                i.dishId = c.getLong(2);
                i.dishName = c.getString(3);
                i.categoryName = c.getString(4);
                i.unitPrice = c.getLong(5);
                i.qty = c.getInt(6);
                i.lineTotal = c.getLong(7);
                out.add(i);
            }
        }
        return out;
    }

    /** إلغاء طلب محفوظ مع السبب. لا يُحذف الطلب أبداً. */
    public void cancelOrder(long id, String reason, String actor) {
        reason = reason == null ? "" : reason.trim();
        if (reason.isEmpty()) throw new IllegalArgumentException("سبب الإلغاء مطلوب");
        SQLiteDatabase db = w();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("cancelled", 1);
            v.put("cancel_reason", reason);
            v.put("cancelled_at", System.currentTimeMillis());
            int n = db.update("orders", v, "id=? AND cancelled=0", new String[]{String.valueOf(id)});
            if (n == 0) throw new IllegalStateException("الطلب ملغى مسبقاً أو غير موجود");
            audit("إلغاء طلب", "#" + id + " — " + reason, actor);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
