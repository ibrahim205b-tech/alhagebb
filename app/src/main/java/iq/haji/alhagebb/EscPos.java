package iq.haji.alhagebb;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * يرسم المحتوى كصورة (حتى تظهر العربية متصلة وباتجاه RTL بصرف النظر عن خط الطابعة)
 * ثم يحوله إلى أوامر ESC/POS (GS v 0) لطابعة 58mm بعرض 384 نقطة.
 */
public final class EscPos {
    public static final int W = 384;
    private static final int PAGE_MAX = 1100;
    private static final int BAND = 128;

    private EscPos() {}

    private static final class Row {
        StaticLayout a, b;
        int ax, bx, h;
        boolean line;
    }

    private static TextPaint paint(int size, boolean bold) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.BLACK);
        p.setTextSize(size);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    private static StaticLayout lay(String s, TextPaint p, int w, Layout.Alignment al) {
        if (s == null) s = "";
        return StaticLayout.Builder.obtain(s, 0, s.length(), p, Math.max(w, 1))
                .setAlignment(al)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(false)
                .build();
    }

    private static List<Row> layout(Doc doc) {
        TextPaint nor = paint(24, false), bol = paint(24, true), tit = paint(34, true);
        List<Row> rows = new ArrayList<>();
        for (Doc.E e : doc.list) {
            Row r = new Row();
            switch (e.t) {
                case TITLE:
                    r.a = lay(e.a, tit, W, Layout.Alignment.ALIGN_CENTER);
                    r.h = r.a.getHeight() + 8;
                    break;
                case CENTER:
                    r.a = lay(e.a, nor, W, Layout.Alignment.ALIGN_CENTER);
                    r.h = r.a.getHeight() + 4;
                    break;
                case TEXT:
                case TEXTB:
                    r.a = lay(e.a, e.t == Doc.T.TEXTB ? bol : nor, W, Layout.Alignment.ALIGN_NORMAL);
                    r.h = r.a.getHeight() + 4;
                    break;
                case KV:
                case KVB: {
                    TextPaint p = e.t == Doc.T.KVB ? bol : nor;
                    int lw = (int) Math.ceil(Layout.getDesiredWidth(e.b, p)) + 2;
                    lw = Math.min(lw, W * 2 / 3);
                    r.b = lay(e.b, p, lw, Layout.Alignment.ALIGN_NORMAL);
                    r.bx = 0;
                    r.a = lay(e.a, p, W - lw - 10, Layout.Alignment.ALIGN_NORMAL);
                    r.ax = lw + 10;
                    r.h = Math.max(r.a.getHeight(), r.b.getHeight()) + 4;
                    break;
                }
                case LINE:
                    r.line = true;
                    r.h = 16;
                    break;
                default:
                    r.h = 14;
            }
            rows.add(r);
        }
        return rows;
    }

    private static Row textRow(String s, TextPaint p) {
        Row r = new Row();
        r.a = lay(s, p, W, Layout.Alignment.ALIGN_CENTER);
        r.h = r.a.getHeight() + 8;
        return r;
    }

    /** يقسّم المحتوى إلى صفحات متتابعة (كل صفحة صورة) مع ترقيم عند التعدد. */
    public static List<Bitmap> pages(Doc doc) {
        List<Row> rows = layout(doc);
        List<List<Row>> groups = new ArrayList<>();
        List<Row> cur = new ArrayList<>();
        int h = 0;
        for (Row r : rows) {
            if (!cur.isEmpty() && h + r.h > PAGE_MAX) {
                groups.add(cur);
                cur = new ArrayList<>();
                h = 0;
            }
            cur.add(r);
            h += r.h;
        }
        if (!cur.isEmpty() || groups.isEmpty()) groups.add(cur);

        String head = "";
        for (Doc.E e : doc.list) if (e.t == Doc.T.TITLE) head = e.a;
        TextPaint small = paint(22, false);

        List<Bitmap> out = new ArrayList<>();
        int n = groups.size();
        for (int i = 0; i < n; i++) {
            List<Row> all = new ArrayList<>();
            if (i > 0) all.add(textRow("(تابع) " + head, small));
            all.addAll(groups.get(i));
            if (n > 1) all.add(textRow("صفحة " + (i + 1) + " من " + n, small));
            out.add(draw(all));
        }
        return out;
    }

    private static Bitmap draw(List<Row> rows) {
        int total = 24;
        for (Row r : rows) total += r.h;
        Bitmap bm = Bitmap.createBitmap(W, total, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        cv.drawColor(Color.WHITE);
        Paint lp = new Paint();
        lp.setColor(Color.BLACK);
        lp.setStrokeWidth(2);
        lp.setPathEffect(new DashPathEffect(new float[]{8, 6}, 0));
        int y = 12;
        for (Row r : rows) {
            if (r.line) {
                cv.drawLine(0, y + r.h / 2f, W, y + r.h / 2f, lp);
            } else {
                if (r.a != null) {
                    cv.save();
                    cv.translate(r.ax, y);
                    r.a.draw(cv);
                    cv.restore();
                }
                if (r.b != null) {
                    cv.save();
                    cv.translate(r.bx, y);
                    r.b.draw(cv);
                    cv.restore();
                }
            }
            y += r.h;
        }
        return bm;
    }

    private static void raster(ByteArrayOutputStream o, Bitmap b) {
        int w = b.getWidth(), h = b.getHeight(), wb = (w + 7) / 8;
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        for (int y0 = 0; y0 < h; y0 += BAND) {
            int rows = Math.min(BAND, h - y0);
            o.write(0x1D);
            o.write(0x76);
            o.write(0x30);
            o.write(0x00);
            o.write(wb & 0xFF);
            o.write((wb >> 8) & 0xFF);
            o.write(rows & 0xFF);
            o.write((rows >> 8) & 0xFF);
            for (int r = 0; r < rows; r++) {
                int base = (y0 + r) * w;
                for (int xb = 0; xb < wb; xb++) {
                    int v = 0;
                    for (int bit = 0; bit < 8; bit++) {
                        int x = xb * 8 + bit;
                        boolean black = false;
                        if (x < w) {
                            int p = px[base + x];
                            int lum = (Color.red(p) * 30 + Color.green(p) * 59 + Color.blue(p) * 11) / 100;
                            black = lum < 150;
                        }
                        v = (v << 1) | (black ? 1 : 0);
                    }
                    o.write(v);
                }
            }
        }
    }

    /** cutter=true يرسل أمر القص (فقط إذا فعّله المستخدم لطابعته). */
    public static byte[] encode(Doc doc, boolean cutter) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(0x1B);
        o.write(0x40); // ESC @
        List<Bitmap> pages = pages(doc);
        for (int i = 0; i < pages.size(); i++) {
            Bitmap bm = pages.get(i);
            raster(o, bm);
            bm.recycle();
            if (cutter) {
                o.write(0x1D);
                o.write(0x56);
                o.write(0x42);
                o.write(0x00); // GS V 66 0
            } else {
                o.write(0x1B);
                o.write(0x64);
                o.write(i == pages.size() - 1 ? 4 : 2); // ESC d n
            }
        }
        return o.toByteArray();
    }
}
