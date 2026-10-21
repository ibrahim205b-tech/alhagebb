package iq.haji.alhagebb;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Repo repo;
    private long selectedCategory = -1;
    private List<Models.Category> categories = new ArrayList<>();
    private List<Models.Driver> drivers = new ArrayList<>();
    private final LinkedHashMap<Long, Models.CartLine> cart = new LinkedHashMap<>();
    private String token = UUID.randomUUID().toString();
    private boolean saving = false;

    private LinearLayout categoryColumn;
    private GridLayout dishGrid;
    private TextView dishEmpty;
    private LinearLayout cartList;
    private TextView cartEmpty;
    private EditText phoneField, addressField, discountField, deliveryField;
    private Spinner driverSpinner;
    private TextView subtotalView, totalView;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        repo = Repo.get(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private TextWatcher watcher(Runnable r) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { r.run(); }
        };
    }

    private static long num(EditText e) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try { return Long.parseLong(s); } catch (NumberFormatException x) { return -1; }
    }

    // ---------------- بناء الواجهة ----------------
    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int p = Ui.dp(this, 10);
        root.setPadding(p, p, p, p);

        // الشريط العلوي
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(Ui.text(this, "مطعم الحجي", 24, Ui.GOLD, true),
                Ui.lp().size(0, WC).weight(1).build());
        int m = Ui.dp(this, 4);
        bar.addView(Ui.button(this, "الطلبات", Ui.CARD2, Ui.TEXT,
                v -> startActivity(new Intent(this, OrdersActivity.class))),
                Ui.lp().size(WC, WC).margin(m, 0, m, 0).build());
        bar.addView(Ui.button(this, "الإدارة", Ui.CARD2, Ui.GOLD,
                v -> startActivity(new Intent(this, ManageActivity.class))),
                Ui.lp().size(WC, WC).margin(m, 0, m, 0).build());
        root.addView(bar, Ui.lp().size(MP, WC).margin(0, 0, 0, p).build());

        // الجسم: الأقسام | الأطباق | الطلب
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);

        ScrollView catScroll = new ScrollView(this);
        categoryColumn = new LinearLayout(this);
        categoryColumn.setOrientation(LinearLayout.VERTICAL);
        catScroll.addView(categoryColumn, new ViewGroup.LayoutParams(MP, WC));
        body.addView(catScroll, Ui.lp().size(Ui.dp(this, 140), MP).build());

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        dishEmpty = Ui.text(this, "", 16, Ui.MUTED, false);
        dishEmpty.setGravity(Gravity.CENTER);
        dishEmpty.setPadding(p, p * 3, p, p);
        mid.addView(dishEmpty, Ui.lp().size(MP, WC).build());
        ScrollView dishScroll = new ScrollView(this);
        dishGrid = new GridLayout(this);
        dishGrid.setColumnCount(3);
        dishScroll.addView(dishGrid, new ViewGroup.LayoutParams(MP, WC));
        mid.addView(dishScroll, Ui.lp().size(MP, 0).weight(1).build());
        body.addView(mid, Ui.lp().size(0, MP).weight(1).margin(p, 0, p, 0).build());

        body.addView(buildCartPanel(), Ui.lp().size(Ui.dp(this, 340), MP).build());
        root.addView(body, Ui.lp().size(MP, 0).weight(1).build());

        Ui.applyInsets(root);
        setContentView(root);
        renderCart();
    }

    private LinearLayout buildCartPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(Ui.rounded(this, Ui.CARD, 16));
        int p = Ui.dp(this, 12);
        panel.setPadding(p, p, p, p);
        int g = Ui.dp(this, 6);

        panel.addView(Ui.text(this, "الطلب الحالي", 18, Ui.GOLD, true));

        ScrollView sv = new ScrollView(this);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        cartEmpty = Ui.text(this, "لم تُضف أي أطباق بعد", 14, Ui.MUTED, false);
        cartEmpty.setGravity(Gravity.CENTER);
        cartEmpty.setPadding(0, p * 2, 0, p * 2);
        inner.addView(cartEmpty, Ui.lp().size(MP, WC).build());
        cartList = new LinearLayout(this);
        cartList.setOrientation(LinearLayout.VERTICAL);
        inner.addView(cartList, Ui.lp().size(MP, WC).build());
        sv.addView(inner, new ViewGroup.LayoutParams(MP, WC));
        panel.addView(sv, Ui.lp().size(MP, 0).weight(1).margin(0, g, 0, g).build());

        phoneField = Ui.field(this, "رقم هاتف الزبون", InputType.TYPE_CLASS_PHONE);
        addressField = Ui.field(this, "العنوان (عند الحاجة)", InputType.TYPE_CLASS_TEXT);
        panel.addView(phoneField, Ui.lp().size(MP, WC).margin(0, g / 2, 0, g / 2).build());
        panel.addView(addressField, Ui.lp().size(MP, WC).margin(0, g / 2, 0, g / 2).build());

        driverSpinner = new Spinner(this);
        panel.addView(driverSpinner, Ui.lp().size(MP, WC).margin(0, g / 2, 0, g / 2).build());

        LinearLayout row = new LinearLayout(this);
        discountField = Ui.field(this, "خصم", InputType.TYPE_CLASS_NUMBER);
        deliveryField = Ui.field(this, "أجور التوصيل", InputType.TYPE_CLASS_NUMBER);
        row.addView(discountField, Ui.lp().size(0, WC).weight(1).margin(0, 0, g / 2, 0).build());
        row.addView(deliveryField, Ui.lp().size(0, WC).weight(1).margin(g / 2, 0, 0, 0).build());
        panel.addView(row, Ui.lp().size(MP, WC).margin(0, g / 2, 0, g).build());
        discountField.addTextChangedListener(watcher(this::updateTotals));
        deliveryField.addTextChangedListener(watcher(this::updateTotals));

        subtotalView = Ui.text(this, "", 14, Ui.MUTED, false);
        totalView = Ui.text(this, "", 22, Ui.GREEN, true);
        panel.addView(subtotalView);
        panel.addView(totalView);

        LinearLayout btns = new LinearLayout(this);
        saveButton = Ui.button(this, "حفظ الطلب", Ui.GREEN, Ui.TEXT, v -> onSave());
        Button newBtn = Ui.button(this, "طلب جديد", Ui.CARD2, Ui.TEXT, v -> onNewOrder());
        btns.addView(saveButton, Ui.lp().size(0, WC).weight(2).margin(0, 0, g / 2, 0).build());
        btns.addView(newBtn, Ui.lp().size(0, WC).weight(1).margin(g / 2, 0, 0, 0).build());
        panel.addView(btns, Ui.lp().size(MP, WC).margin(0, g, 0, 0).build());
        return panel;
    }

    // ---------------- البيانات ----------------
    private void refreshData() {
        categories = repo.categories(true);
        drivers = repo.drivers(true);
        boolean found = false;
        for (Models.Category c : categories) if (c.id == selectedCategory) found = true;
        if (!found) selectedCategory = categories.isEmpty() ? -1 : categories.get(0).id;
        renderCategories();
        renderDishes();
        refreshDrivers();
    }

    private void refreshDrivers() {
        long keep = -1;
        int pos = driverSpinner.getSelectedItemPosition();
        // السائق المحدد سابقاً (إن وُجد) نحافظ عليه بعد التحديث
        List<String> names = new ArrayList<>();
        names.add("بدون سائق");
        int newPos = 0;
        for (int i = 0; i < drivers.size(); i++) names.add(drivers.get(i).name);
        if (pos > 0 && pos - 1 < drivers.size()) keep = drivers.get(pos - 1).id;
        driverSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names));
        for (int i = 0; i < drivers.size(); i++) if (drivers.get(i).id == keep) newPos = i + 1;
        driverSpinner.setSelection(newPos);
    }

    private void renderCategories() {
        categoryColumn.removeAllViews();
        for (Models.Category c : categories) {
            boolean sel = c.id == selectedCategory;
            Button b = Ui.button(this, c.name, sel ? Ui.GOLD : Ui.CARD2, sel ? Ui.DARK : Ui.TEXT, v -> {
                selectedCategory = c.id;
                renderCategories();
                renderDishes();
            });
            categoryColumn.addView(b, Ui.lp().size(MP, WC).margin(0, 0, 0, Ui.dp(this, 6)).build());
        }
    }

    private void renderDishes() {
        dishGrid.removeAllViews();
        List<Models.Dish> list = selectedCategory < 0
                ? new ArrayList<>() : repo.dishes(selectedCategory, true);
        if (list.isEmpty()) {
            dishEmpty.setVisibility(View.VISIBLE);
            dishEmpty.setText(categories.isEmpty()
                    ? "لا توجد أقسام بعد.\nأضف الأقسام والأطباق من «الإدارة»."
                    : "لا توجد أطباق فعّالة في هذا القسم.");
            return;
        }
        dishEmpty.setVisibility(View.GONE);
        int mg = Ui.dp(this, 4);
        for (Models.Dish d : list) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setBackground(Ui.rounded(this, Ui.CARD, 12));
            card.setPadding(mg * 2, mg * 3, mg * 2, mg * 3);
            TextView n = Ui.text(this, d.name, 15, Ui.TEXT, true);
            n.setGravity(Gravity.CENTER);
            TextView pr = Ui.text(this, Ui.money(d.price), 14, Ui.GOLD, false);
            pr.setGravity(Gravity.CENTER);
            card.addView(n, Ui.lp().size(MP, WC).build());
            card.addView(pr, Ui.lp().size(MP, WC).build());
            card.setOnClickListener(v -> addDish(d));
            Ui.pressAnim(card);
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            gp.width = 0;
            gp.height = WC;
            gp.setMargins(mg, mg, mg, mg);
            dishGrid.addView(card, gp);
        }
    }

    // ---------------- السلة ----------------
    private void addDish(Models.Dish d) {
        Models.CartLine l = cart.get(d.id);
        if (l == null) {
            l = new Models.CartLine();
            l.dishId = d.id;
            l.name = d.name;
            l.categoryName = repo.categoryName(d.categoryId);
            l.unitPrice = d.price; // السعر وقت الإضافة
            l.qty = 0;
            cart.put(d.id, l);
        }
        l.qty++;
        renderCart();
    }

    private void renderCart() {
        cartList.removeAllViews();
        cartEmpty.setVisibility(cart.isEmpty() ? View.VISIBLE : View.GONE);
        int g = Ui.dp(this, 4);
        for (Models.CartLine l : cart.values()) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setBackground(Ui.rounded(this, Ui.CARD2, 10));
            item.setPadding(g * 2, g * 2, g * 2, g * 2);

            LinearLayout top = new LinearLayout(this);
            top.addView(Ui.text(this, l.name, 15, Ui.TEXT, true), Ui.lp().size(0, WC).weight(1).build());
            top.addView(Ui.text(this, Ui.money(l.total()), 14, Ui.GOLD, true));
            item.addView(top);

            LinearLayout ctl = new LinearLayout(this);
            ctl.setGravity(Gravity.CENTER_VERTICAL);
            ctl.addView(Ui.text(this, Ui.money(l.unitPrice) + " × " + l.qty, 12, Ui.MUTED, false),
                    Ui.lp().size(0, WC).weight(1).build());
            ctl.addView(smallBtn("−", v -> { l.qty--; if (l.qty <= 0) cart.remove(l.dishId); renderCart(); }));
            ctl.addView(smallBtn("+", v -> { l.qty++; renderCart(); }));
            ctl.addView(smallBtn("حذف", v -> { cart.remove(l.dishId); renderCart(); }));
            item.addView(ctl);

            cartList.addView(item, Ui.lp().size(MP, WC).margin(0, 0, 0, g).build());
        }
        updateTotals();
    }

    private Button smallBtn(String label, View.OnClickListener l) {
        Button b = Ui.button(this, label, Ui.CARD, Ui.TEXT, l);
        b.setMinWidth(Ui.dp(this, 40));
        b.setMinimumWidth(Ui.dp(this, 40));
        b.setMinHeight(Ui.dp(this, 36));
        b.setMinimumHeight(Ui.dp(this, 36));
        b.setTextSize(13);
        b.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        LinearLayout.LayoutParams lp = Ui.lp().size(WC, WC).margin(Ui.dp(this, 3), 0, 0, 0).build();
        b.setLayoutParams(lp);
        return b;
    }

    private long subtotal() {
        long s = 0;
        for (Models.CartLine l : cart.values()) s += l.total();
        return s;
    }

    private void updateTotals() {
        long sub = subtotal();
        long disc = Math.max(0, num(discountField));
        long fee = Math.max(0, num(deliveryField));
        long total = Math.max(0, sub - disc) + fee;
        subtotalView.setText("المجموع الفرعي: " + Ui.money(sub));
        totalView.setText("الإجمالي: " + Ui.money(total));
    }

    private void resetOrder() {
        cart.clear();
        phoneField.setText("");
        addressField.setText("");
        discountField.setText("");
        deliveryField.setText("");
        driverSpinner.setSelection(0);
        token = UUID.randomUUID().toString();
        renderCart();
    }

    private void onNewOrder() {
        if (cart.isEmpty()) {
            resetOrder();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("طلب جديد")
                .setMessage("سيتم تفريغ الطلب الحالي غير المحفوظ. هل تريد المتابعة؟")
                .setPositiveButton("نعم", (d, w) -> resetOrder())
                .setNegativeButton("لا", null)
                .show();
    }

    // ---------------- الحفظ ----------------
    private void onSave() {
        if (saving) return;
        if (cart.isEmpty()) {
            Ui.toast(this, "أضف طبقاً واحداً على الأقل");
            return;
        }
        long disc = num(discountField);
        long fee = num(deliveryField);
        if (disc < 0 || fee < 0) {
            Ui.toast(this, "قيمة الخصم أو أجور التوصيل غير صالحة");
            return;
        }
        if (disc > subtotal()) {
            Ui.toast(this, "الخصم أكبر من المجموع");
            return;
        }
        String phone = phoneField.getText().toString().trim();
        if (!phone.isEmpty() && !phone.matches("^\\+?[0-9 ]{7,15}$")) {
            Ui.toast(this, "رقم الهاتف غير صالح");
            return;
        }
        String address = addressField.getText().toString().trim();
        if (disc > 0) {
            // الخصم عملية حساسة: تحتاج رمز المدير
            Security.require(this, () -> doSave(disc, fee, phone, address), null);
        } else {
            doSave(disc, fee, phone, address);
        }
    }

    private void doSave(long disc, long fee, String phone, String address) {
        if (saving) return;
        saving = true;
        saveButton.setEnabled(false);
        try {
            Long driverId = null;
            String driverName = "";
            int pos = driverSpinner.getSelectedItemPosition();
            if (pos > 0 && pos - 1 < drivers.size()) {
                driverId = drivers.get(pos - 1).id;
                driverName = drivers.get(pos - 1).name;
            }
            long id = repo.saveOrder(token, new ArrayList<>(cart.values()), phone, address,
                    driverId, driverName, disc, fee);
            if (disc > 0) {
                repo.audit("خصم على طلب", "#" + id + " قيمة " + disc, Repo.ACTOR_MANAGER);
            }
            Ui.toast(this, "تم حفظ الطلب رقم " + id);
            resetOrder();
        } catch (Exception e) {
            Ui.toast(this, "تعذر حفظ الطلب: " + e.getMessage());
        } finally {
            saving = false;
            saveButton.setEnabled(true);
        }
    }
}
