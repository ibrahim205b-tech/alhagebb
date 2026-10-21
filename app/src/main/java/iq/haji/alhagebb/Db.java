package iq.haji.alhagebb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Db extends SQLiteOpenHelper {
    public static final String NAME = "alhagebb.db";
    public static final int VERSION = 1;

    public Db(Context ctx) {
        super(ctx.getApplicationContext(), NAME, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "sort_order INTEGER NOT NULL DEFAULT 0," +
                "active INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE dishes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category_id INTEGER NOT NULL REFERENCES categories(id)," +
                "name TEXT NOT NULL," +
                "price INTEGER NOT NULL CHECK(price >= 0)," +
                "active INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE drivers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "phone TEXT NOT NULL DEFAULT ''," +
                "active INTEGER NOT NULL DEFAULT 1)");

        // رقم الطلب = id وهو AUTOINCREMENT فلا يُعاد استخدامه أبداً
        db.execSQL("CREATE TABLE orders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "client_token TEXT NOT NULL UNIQUE," +
                "created_at INTEGER NOT NULL," +
                "day_key TEXT NOT NULL," +
                "phone TEXT NOT NULL DEFAULT ''," +
                "address TEXT NOT NULL DEFAULT ''," +
                "driver_id INTEGER," +
                "driver_name TEXT NOT NULL DEFAULT ''," +
                "subtotal INTEGER NOT NULL," +
                "discount INTEGER NOT NULL DEFAULT 0," +
                "delivery_fee INTEGER NOT NULL DEFAULT 0," +
                "total INTEGER NOT NULL," +
                "cancelled INTEGER NOT NULL DEFAULT 0," +
                "cancel_reason TEXT NOT NULL DEFAULT ''," +
                "cancelled_at INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE order_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "order_id INTEGER NOT NULL REFERENCES orders(id)," +
                "dish_id INTEGER NOT NULL," +
                "dish_name TEXT NOT NULL," +
                "category_name TEXT NOT NULL," +
                "unit_price INTEGER NOT NULL," +
                "qty INTEGER NOT NULL CHECK(qty > 0)," +
                "line_total INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_items_order ON order_items(order_id)");
        db.execSQL("CREATE INDEX idx_orders_created ON orders(created_at)");

        db.execSQL("CREATE TABLE price_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "dish_id INTEGER NOT NULL," +
                "dish_name TEXT NOT NULL," +
                "old_price INTEGER NOT NULL," +
                "new_price INTEGER NOT NULL," +
                "changed_at INTEGER NOT NULL," +
                "actor TEXT NOT NULL)");

        db.execSQL("CREATE TABLE audit_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ts INTEGER NOT NULL," +
                "action TEXT NOT NULL," +
                "details TEXT NOT NULL," +
                "actor TEXT NOT NULL)");

        db.execSQL("CREATE TABLE print_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ts INTEGER NOT NULL," +
                "kind TEXT NOT NULL," +
                "ref TEXT NOT NULL," +
                "result TEXT NOT NULL," +
                "error TEXT NOT NULL DEFAULT '')");

        db.execSQL("CREATE TABLE settings (" +
                "key TEXT PRIMARY KEY," +
                "value TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // لا ترقيات في الإصدار 1
    }
}
