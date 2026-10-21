package iq.haji.alhagebb;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Base64;

import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import androidx.appcompat.app.AlertDialog;

/** رمز المدير: لا يُخزَّن نصاً؛ يُحفظ ملح عشوائي + PBKDF2 فقط. */
public final class Security {
    private static final String PREFS = "alhagebb_sec";
    private static final int ITER = 120_000;

    private Security() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static byte[] hash(char[] pin, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(pin, salt, ITER, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    public static boolean hasPin(Context c) {
        return prefs(c).contains("hash");
    }

    public static void setPin(Context c, String pin) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] h = hash(pin.toCharArray(), salt);
            prefs(c).edit()
                    .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString("hash", Base64.encodeToString(h, Base64.NO_WRAP))
                    .apply();
        } catch (Exception e) {
            throw new IllegalStateException("تعذر حفظ الرمز", e);
        }
    }

    public static boolean verify(Context c, String pin) {
        try {
            String s = prefs(c).getString("salt", null);
            String h = prefs(c).getString("hash", null);
            if (s == null || h == null) return false;
            byte[] calc = hash(pin.toCharArray(), Base64.decode(s, Base64.NO_WRAP));
            byte[] saved = Base64.decode(h, Base64.NO_WRAP);
            if (calc.length != saved.length) return false;
            int diff = 0;
            for (int i = 0; i < calc.length; i++) diff |= calc[i] ^ saved[i];
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** يطلب رمز المدير. عند أول استخدام يطلب إنشاء رمز (4 أرقام على الأقل). */
    public static void require(Activity a, Runnable ok, Runnable fail) {
        final boolean creating = !hasPin(a);
        final android.widget.EditText e = Ui.field(a, creating ? "رمز جديد (4 أرقام على الأقل)" : "رمز المدير",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        android.widget.FrameLayout f = new android.widget.FrameLayout(a);
        int p = Ui.dp(a, 20);
        f.setPadding(p, p / 2, p, 0);
        f.addView(e);
        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle(creating ? "إنشاء رمز المدير" : "التحقق من المدير")
                .setView(f)
                .setPositiveButton("موافق", null)
                .setNegativeButton("إلغاء", (dd, w) -> { if (fail != null) fail.run(); })
                .setOnCancelListener(dd -> { if (fail != null) fail.run(); })
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String pin = e.getText().toString().trim();
            if (creating) {
                if (pin.length() < 4) {
                    Ui.toast(a, "الرمز يجب ألا يقل عن 4 أرقام");
                    return;
                }
                setPin(a, pin);
                Repo.get(a).audit("إنشاء رمز المدير", "تم إنشاء الرمز", Repo.ACTOR_MANAGER);
                d.dismiss();
                ok.run();
            } else if (verify(a, pin)) {
                d.dismiss();
                ok.run();
            } else {
                Ui.toast(a, "الرمز غير صحيح");
                e.setText("");
            }
        }));
        d.setCancelable(true);
        d.show();
    }
}
