package iq.haji.alhagebb;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** اتصال Bluetooth Classic (SPP) بطابعة حرارية وإرسال أوامر ESC/POS. */
@SuppressLint("MissingPermission")
public final class PrinterManager {
    public interface Cb { void done(boolean ok, String message); }

    private static final class PrintException extends Exception {
        PrintException(String m) { super(m); }
    }

    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int CHUNK = 512;

    private static PrinterManager inst;

    private final Context app;
    private final SharedPreferences prefs;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile BluetoothSocket socket;

    private PrinterManager(Context c) {
        app = c.getApplicationContext();
        prefs = app.getSharedPreferences("alhagebb_printer", Context.MODE_PRIVATE);
    }

    public static synchronized PrinterManager get(Context c) {
        if (inst == null) inst = new PrinterManager(c);
        return inst;
    }

    public static boolean hasPermission(Context c) {
        if (Build.VERSION.SDK_INT >= 31) {
            return ContextCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public String savedAddress() { return prefs.getString("addr", null); }
    public String savedName() { return prefs.getString("name", ""); }
    public boolean cutter() { return prefs.getBoolean("cutter", false); }
    public void setCutter(boolean v) { prefs.edit().putBoolean("cutter", v).apply(); }

    public void saveDevice(String addr, String name) {
        prefs.edit().putString("addr", addr).putString("name", name == null ? "" : name).apply();
        exec.execute(this::closeQuiet);
    }

    public boolean isConnected() {
        BluetoothSocket s = socket;
        return s != null && s.isConnected();
    }

    private void closeQuiet() {
        BluetoothSocket s = socket;
        socket = null;
        if (s != null) {
            try { s.close(); } catch (IOException ignored) { }
        }
    }

    private void connectBlocking() throws PrintException {
        if (!hasPermission(app)) {
            throw new PrintException("صلاحية Bluetooth غير ممنوحة. افتح الإعدادات ← الطابعة وامنحها.");
        }
        BluetoothManager bm = (BluetoothManager) app.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter ad = bm == null ? null : bm.getAdapter();
        if (ad == null) throw new PrintException("هذا الجهاز لا يدعم Bluetooth.");
        if (!ad.isEnabled()) throw new PrintException("Bluetooth متوقف. شغّله ثم أعد المحاولة.");
        String addr = savedAddress();
        if (addr == null || !BluetoothAdapter.checkBluetoothAddress(addr)) {
            throw new PrintException("لم تُختر طابعة. اختر الطابعة من الإعدادات ← الطابعة.");
        }
        BluetoothDevice dev = ad.getRemoteDevice(addr);
        if (dev.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            throw new PrintException("هذه الطابعة تعمل بـ Bluetooth LE فقط، والتطبيق يدعم Classic/SPP.");
        }
        closeQuiet();
        ad.cancelDiscovery();
        for (int attempt = 0; attempt < 2; attempt++) {
            BluetoothSocket s = null;
            try {
                s = attempt == 0 ? dev.createRfcommSocketToServiceRecord(SPP)
                        : dev.createInsecureRfcommSocketToServiceRecord(SPP);
                s.connect();
                socket = s;
                return;
            } catch (IOException | SecurityException e) {
                if (s != null) {
                    try { s.close(); } catch (IOException ignored) { }
                }
            }
        }
        throw new PrintException("تعذر الاتصال بالطابعة. تأكد أنها مشغّلة وقريبة وغير متصلة بجهاز آخر.");
    }

    private void writeAll(byte[] data, int[] sent) throws IOException {
        BluetoothSocket s = socket;
        if (s == null) throw new IOException("no socket");
        OutputStream os = s.getOutputStream();
        int pos = 0;
        while (pos < data.length) {
            int n = Math.min(CHUNK, data.length - pos);
            os.write(data, pos, n);
            os.flush();
            pos += n;
            sent[0] = pos;
            if (pos < data.length) {
                try { Thread.sleep(12); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted");
                }
            }
        }
    }

    private void post(Cb cb, boolean ok, String msg) {
        if (cb != null) main.post(() -> cb.done(ok, msg));
    }

    public void connect(Cb cb) {
        exec.execute(() -> {
            try {
                connectBlocking();
                post(cb, true, "تم الاتصال بالطابعة " + savedName());
            } catch (PrintException e) {
                post(cb, false, e.getMessage());
            } catch (Exception e) {
                post(cb, false, "تعذر الاتصال بالطابعة");
            }
        });
    }

    public void disconnect(Cb cb) {
        exec.execute(() -> {
            closeQuiet();
            post(cb, true, "تم قطع الاتصال");
        });
    }

    /** يرسم المحتوى ويرسله للطابعة ويسجل المحاولة في print_log. لا يغيّر أي بيانات تجارية. */
    public void printDoc(final Doc doc, final String kind, final String ref, final Cb cb) {
        exec.execute(() -> {
            boolean ok = false;
            String msg;
            String err = "";
            try {
                byte[] data = EscPos.encode(doc, cutter());
                if (!isConnected()) connectBlocking();
                int[] sent = {0};
                try {
                    writeAll(data, sent);
                } catch (IOException e1) {
                    closeQuiet();
                    if (sent[0] == 0) {
                        connectBlocking();
                        sent[0] = 0;
                        try {
                            writeAll(data, sent);
                        } catch (IOException e2) {
                            closeQuiet();
                            throw new PrintException("انقطع الاتصال أثناء الإرسال. تحقق من الورق ثم أعد المحاولة.");
                        }
                    } else {
                        throw new PrintException("انقطع الاتصال أثناء الإرسال، وقد تكون الطباعة ناقصة. تحقق من الورق ثم أعد المحاولة.");
                    }
                }
                ok = true;
                msg = "تم إرسال البيانات إلى الطابعة";
            } catch (PrintException e) {
                msg = e.getMessage();
                err = msg;
            } catch (Exception e) {
                msg = "حدث خطأ غير متوقع أثناء الطباعة";
                err = e.getClass().getSimpleName();
            }
            try {
                Repo.get(app).logPrint(kind, ref, ok ? "تم إرسال البيانات" : "فشل", err);
            } catch (Exception ignored) { }
            post(cb, ok, msg);
        });
    }

    /** يعرض نتيجة الطباعة: رسالة قصيرة عند النجاح، ونافذة مع «إعادة المحاولة» عند الفشل. */
    public static void report(Activity a, boolean ok, String msg, Runnable retry) {
        if (a.isFinishing() || a.isDestroyed()) return;
        if (ok) {
            Ui.toast(a, msg);
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle("تعذرت الطباعة")
                .setMessage(msg + "\n\nالبيانات المحفوظة لم تتأثر.")
                .setNegativeButton("إغلاق", null);
        if (retry != null) b.setPositiveButton("إعادة المحاولة", (d, w) -> retry.run());
        b.show();
    }
}
