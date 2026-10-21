package iq.haji.alhagebb;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Ui {
    private Ui() {}

    public static final int BG = 0xFF12151A;
    public static final int CARD = 0xFF1C2027;
    public static final int CARD2 = 0xFF262B34;
    public static final int GOLD = 0xFFD4AF37;
    public static final int GREEN = 0xFF2E9E5B;
    public static final int RED = 0xFFD64545;
    public static final int TEXT = 0xFFF5F5F5;
    public static final int MUTED = 0xFF9AA3AF;
    public static final int DARK = 0xFF12151A;

    public interface Cb { void on(String value); }

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static String money(long v) {
        DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        return f.format(v) + " د.ع";
    }

    public static String dateTime(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(ms));
    }

    public static void toast(Context c, String m) {
        Toast.makeText(c, m, Toast.LENGTH_SHORT).show();
    }

    public static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        return t;
    }

    public static void pressAnim(View v) {
        v.setOnTouchListener((view, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).start();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
            }
            return false;
        });
    }

    public static Button button(Context c, String label, int bg, int fg, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(rounded(c, bg, 12));
        b.setMinHeight(dp(c, 44));
        b.setMinimumHeight(dp(c, 44));
        b.setPadding(dp(c, 14), dp(c, 6), dp(c, 14), dp(c, 6));
        b.setStateListAnimator(null);
        b.setOnClickListener(l);
        pressAnim(b);
        return b;
    }

    public static EditText field(Context c, String hint, int inputType) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(15);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setBackground(rounded(c, CARD2, 10));
        e.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        return e;
    }

    public static void askText(Context c, String title, String hint, String initial, int inputType, Cb cb) {
        EditText e = field(c, hint, inputType);
        if (initial != null) e.setText(initial);
        FrameLayout f = new FrameLayout(c);
        int p = dp(c, 20);
        f.setPadding(p, p / 2, p, 0);
        f.addView(e);
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setView(f)
                .setPositiveButton("موافق", (d, w) -> cb.on(e.getText().toString().trim()))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    public static LinearLayoutParamsBuilder lp() {
        return new LinearLayoutParamsBuilder();
    }

    /** منشئ مختصر لـ LinearLayout.LayoutParams */
    public static final class LinearLayoutParamsBuilder {
        private int w = ViewGroup.LayoutParams.MATCH_PARENT;
        private int h = ViewGroup.LayoutParams.WRAP_CONTENT;
        private float weight = 0;
        private int[] m = {0, 0, 0, 0};

        public LinearLayoutParamsBuilder size(int w, int h) { this.w = w; this.h = h; return this; }
        public LinearLayoutParamsBuilder weight(float wt) { this.weight = wt; return this; }
        public LinearLayoutParamsBuilder margin(int px) { m = new int[]{px, px, px, px}; return this; }
        public LinearLayoutParamsBuilder margin(int l, int t, int r, int b) { m = new int[]{l, t, r, b}; return this; }

        public android.widget.LinearLayout.LayoutParams build() {
            android.widget.LinearLayout.LayoutParams p = new android.widget.LinearLayout.LayoutParams(w, h, weight);
            p.setMargins(m[0], m[1], m[2], m[3]);
            return p;
        }
    }

    /** يضيف حشوات أشرطة النظام (مطلوب مع Android 15 edge-to-edge). */
    public static void applyInsets(View root) {
        final int l = root.getPaddingLeft(), t = root.getPaddingTop(),
                r = root.getPaddingRight(), b = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(l + bars.left, t + bars.top, r + bars.right, b + Math.max(bars.bottom, ime.bottom));
            return insets;
        });
    }
}
