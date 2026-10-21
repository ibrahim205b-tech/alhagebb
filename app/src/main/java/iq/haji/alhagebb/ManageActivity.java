package iq.haji.alhagebb;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ManageActivity extends AppCompatActivity {
    private static final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Repo repo;
    private LinearLayout root, catColumn, dishColumn;
    private TextView dishHeader;
    private long selectedCat = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        repo = Repo.get(this);
        buildUi();
        root.setVisibility(View.INVISIBLE);
        setContentView(root);
        // الإدارة كلها خلف رمز المدير
        Security.require(this, () -> {
            root.setVisibility(View.VISIBLE);
            reload();
        }, this::finish);
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int p = Ui.dp(this, 12);
        root.setPadding(p, p, p, p);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(Ui.text(this, "الإدارة — الأقسام والأطباق والأسعار", 22, Ui.GOLD, true),
                Ui.lp().size(0, WC).weight(1).build());
        bar.addView(Ui.button(this, "رجوع", Ui.CARD2, Ui.TEXT, v -> finish()));
        root.addView(bar, Ui.lp().size(MP, WC).margin(0, 0, 0, p).build());

        LinearLayout body = new LinearLayout(this);

        // لوحة الأقسام
        LinearLayout catPanel = new LinearLayout(this);
        catPanel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout catHead = new LinearLayout(this);
        catHead.setGravity(Gravity.CENTER_VERTICAL);
        catHead.addView(Ui.text(this, "الأقسام", 18, Ui.TEXT, true), Ui.lp().size(0, WC).weight(1).build());
        catHead.addView(Ui.button(this, "+ قسم", Ui.GOLD, Ui.DARK, v -> editCategory(null)));
        catPanel.addView(catHead, Ui.lp().size(MP, WC).margin(0, 0, 0, p / 2).build());
        ScrollView cs = new ScrollView(this);
        catColumn = new LinearLayout(this);
        catColumn.setOrientation(LinearLayout.VERTICAL);
        cs.addView(catColumn, new ViewGroup.LayoutParams(MP, WC));
        catPanel.addView(cs, Ui.lp().size(MP, 0).weight(1).build());
        body.addView(catPanel, Ui.lp().size(0, MP).weight(1).margin(0, 0, p / 2, 0).build());

        // لوحة الأطباق
        LinearLayout dishPanel = new LinearLayout(this);
        dishPanel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout dishHead = new LinearLayout(this);
        dishHead.setGravity(Gravity.CENTER_VERTICAL);
        dishHeader = Ui.text(this, "الأطباق", 18, Ui.TEXT, true);
        dishHead.addView(dishHeader, Ui.lp().size(0, WC).weight(1).build());
        dishHead.addView(Ui.button(this, "+ طبق", Ui.GOLD, Ui.DARK, v -> editDish(null)));
        dishPanel.addView(dishHead, Ui.lp().size(MP, WC).margin(0, 0, 0, p / 2).build());
        ScrollView ds = new ScrollView(this);
        dishColumn = new LinearLayout(this);
        dishColumn.setOrientation(LinearLayout.VERTICAL);
        ds.addView(dishColumn, new ViewGroup.LayoutParams(MP, WC));
        dishPanel.addView(ds, Ui.lp().size(MP, 0).weight(1).build());
        body.addView(dishPanel, Ui.lp().size(0, MP).weight(1).margin(p / 2, 0, 0, 0).build());

        root.addView(body, Ui.lp().size(MP, 0).weight(1).build());
        Ui.applyInsets(root);
    }

    private void reload() {
        List<Models.Category> cats = repo.categories(false);
        boolean found = false;
        for (Models.Category c : cats) if (c.id == selectedCat) found = true;
        if (!found) selectedCat = cats.isEmpty() ? -1 : cats.get(0).id;
        renderCats(cats);
        renderDishes();
    }

    private Button mini(String label, View.OnClickListener l) {
        Button b = Ui.button(this, label, Ui.CARD2, Ui.TEXT, l);
        b.setMinWidth(Ui.dp(this, 40));
        b.setMinimumWidth(Ui.dp(this, 40));
        b.setMinHeight(Ui.dp(this, 38));
        b.setMinimumHeight(Ui.dp(this, 38));
        b.setTextSize(13);
        b.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        b.setLayoutParams(Ui.lp().size(WC, WC).margin(Ui.dp(this, 3), 0, 0, 0).build());
        return b;
    }

    private void renderCats(List<Models.Category> cats) {
        catColumn.removeAllViews();
        if (cats.isEmpty()) {
            TextView e = Ui.text(this, "لا توجد أقسام. اضغط «+ قسم».", 14, Ui.MUTED, false);
            e.setPadding(0, Ui.dp(this, 20), 0, 0);
            catColumn.addView(e);
            return;
        }
        for (Models.Category c : cats) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            boolean sel = c.id == selectedCat;
            row.setBackground(Ui.rounded(this, sel ? 0xFF3A3320 : Ui.CARD, 10));
            int q = Ui.dp(this, 8);
            row.setPadding(q, q, q, q);
            TextView name = Ui.text(this, c.name + (c.active ? "" : " (معطل)"), 15,
                    c.active ? Ui.TEXT : Ui.MUTED, true);
            name.setOnClickListener(v -> { selectedCat = c.id; reload(); });
            row.addView(name, Ui.lp().size(0, WC).weight(1).build());
            row.addView(mini("▲", v -> { repo.moveCategory(c.id, -1, Repo.ACTOR_MANAGER); reload(); }));
            row.addView(mini("▼", v -> { repo.moveCategory(c.id, 1, Repo.ACTOR_MANAGER); reload(); }));
            row.addView(mini("تعديل", v -> editCategory(c)));
            row.addView(mini(c.active ? "تعطيل" : "تفعيل", v -> {
                repo.setCategoryActive(c.id, !c.active, Repo.ACTOR_MANAGER);
                reload();
            }));
            catColumn.addView(row, Ui.lp().size(MP, WC).margin(0, 0, 0, Ui.dp(this, 6)).build());
        }
    }

    private void renderDishes() {
        dishColumn.removeAllViews();
        if (selectedCat < 0) {
            dishHeader.setText("الأطباق");
            return;
        }
        dishHeader.setText("أطباق: " + repo.categoryName(selectedCat));
        List<Models.Dish> dishes = repo.dishes(selectedCat, false);
        if (dishes.isEmpty()) {
            TextView e = Ui.text(this, "لا توجد أطباق. اضغط «+ طبق».", 14, Ui.MUTED, false);
            e.setPadding(0, Ui.dp(this, 20), 0, 0);
            dishColumn.addView(e);
            return;
        }
        for (Models.Dish d : dishes) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.rounded(this, Ui.CARD, 10));
            int q = Ui.dp(this, 8);
            row.setPadding(q, q, q, q);
            row.addView(Ui.text(this, d.name + (d.active ? "" : " (معطل)"), 15,
                    d.active ? Ui.TEXT : Ui.MUTED, true), Ui.lp().size(0, WC).weight(1).build());
            row.addView(Ui.text(this, Ui.money(d.price), 14, Ui.GOLD, false));
            row.addView(mini("تعديل", v -> editDish(d)));
            row.addView(mini(d.active ? "تعطيل" : "تفعيل", v -> {
                repo.setDishActive(d.id, !d.active, Repo.ACTOR_MANAGER);
                reload();
            }));
            dishColumn.addView(row, Ui.lp().size(MP, WC).margin(0, 0, 0, Ui.dp(this, 6)).build());
        }
    }

    private void editCategory(Models.Category c) {
        Ui.askText(this, c == null ? "قسم جديد" : "تعديل القسم", "اسم القسم",
                c == null ? "" : c.name, InputType.TYPE_CLASS_TEXT, name -> {
                    try {
                        long id = repo.saveCategory(c == null ? 0 : c.id, name, Repo.ACTOR_MANAGER);
                        if (c == null) selectedCat = id;
                        reload();
                    } catch (Exception e) {
                        Ui.toast(this, e.getMessage());
                    }
                });
    }

    private void editDish(Models.Dish d) {
        final List<Models.Category> cats = repo.categories(false);
        if (cats.isEmpty()) {
            Ui.toast(this, "أضف قسماً أولاً");
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        form.setPadding(p, p / 2, p, 0);

        EditText name = Ui.field(this, "اسم الطبق", InputType.TYPE_CLASS_TEXT);
        EditText price = Ui.field(this, "السعر", InputType.TYPE_CLASS_NUMBER);
        Spinner sp = new Spinner(this);
        List<String> names = new ArrayList<>();
        int sel = 0;
        for (int i = 0; i < cats.size(); i++) {
            names.add(cats.get(i).name);
            long target = d != null ? d.categoryId : selectedCat;
            if (cats.get(i).id == target) sel = i;
        }
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        sp.setSelection(sel);
        if (d != null) {
            name.setText(d.name);
            price.setText(String.valueOf(d.price));
        }
        int g = Ui.dp(this, 6);
        form.addView(name, Ui.lp().size(MP, WC).margin(0, g, 0, g).build());
        form.addView(price, Ui.lp().size(MP, WC).margin(0, g, 0, g).build());
        form.addView(sp, Ui.lp().size(MP, WC).margin(0, g, 0, g).build());

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(d == null ? "طبق جديد" : "تعديل الطبق")
                .setView(form)
                .setPositiveButton("حفظ", null)
                .setNegativeButton("إلغاء", null)
                .create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long pr;
            try {
                pr = Long.parseLong(price.getText().toString().trim());
            } catch (NumberFormatException e) {
                Ui.toast(this, "السعر غير صالح");
                return;
            }
            try {
                long catId = cats.get(sp.getSelectedItemPosition()).id;
                repo.saveDish(d == null ? 0 : d.id, catId, name.getText().toString(), pr, Repo.ACTOR_MANAGER);
                selectedCat = catId;
                dlg.dismiss();
                reload();
            } catch (Exception e) {
                Ui.toast(this, e.getMessage());
            }
        }));
        dlg.show();
    }
}
