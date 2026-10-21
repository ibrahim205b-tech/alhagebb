package iq.haji.alhagebb;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** محتوى منظّم واحد يُستخدم للمعاينة على الشاشة وللطباعة، فيتطابقان دائماً. */
public class Doc {
    public enum T { TITLE, CENTER, KV, KVB, TEXT, TEXTB, LINE, SPACE }

    public static final class E {
        public final T t;
        public final String a;
        public final String b;

        E(T t, String a, String b) {
            this.t = t;
            this.a = a == null ? "" : a;
            this.b = b == null ? "" : b;
        }
    }

    public final List<E> list = new ArrayList<>();

    public Doc title(String s) { list.add(new E(T.TITLE, s, "")); return this; }
    public Doc center(String s) { list.add(new E(T.CENTER, s, "")); return this; }
    public Doc kv(String label, String value) { list.add(new E(T.KV, label, value)); return this; }
    public Doc kvb(String label, String value) { list.add(new E(T.KVB, label, value)); return this; }
    public Doc text(String s) { list.add(new E(T.TEXT, s, "")); return this; }
    public Doc textb(String s) { list.add(new E(T.TEXTB, s, "")); return this; }
    public Doc line() { list.add(new E(T.LINE, "", "")); return this; }
    public Doc space() { list.add(new E(T.SPACE, "", "")); return this; }

    public View toView(Context c) {
        final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
        final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(c, 12);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(Ui.rounded(c, Ui.CARD, 14));
        for (E e : list) {
            switch (e.t) {
                case TITLE: {
                    TextView t = Ui.text(c, e.a, 20, Ui.GOLD, true);
                    t.setGravity(Gravity.CENTER);
                    box.addView(t, new LinearLayout.LayoutParams(MP, WC));
                    break;
                }
                case CENTER: {
                    TextView t = Ui.text(c, e.a, 13, Ui.MUTED, false);
                    t.setGravity(Gravity.CENTER);
                    box.addView(t, new LinearLayout.LayoutParams(MP, WC));
                    break;
                }
                case KV:
                case KVB: {
                    boolean b = e.t == T.KVB;
                    LinearLayout r = new LinearLayout(c);
                    r.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 2));
                    r.addView(Ui.text(c, e.a, 14, Ui.TEXT, b), new LinearLayout.LayoutParams(0, WC, 1f));
                    r.addView(Ui.text(c, e.b, 14, b ? Ui.GOLD : Ui.TEXT, b), new LinearLayout.LayoutParams(WC, WC));
                    box.addView(r, new LinearLayout.LayoutParams(MP, WC));
                    break;
                }
                case TEXT:
                case TEXTB: {
                    TextView t = Ui.text(c, e.a, 14, Ui.TEXT, e.t == T.TEXTB);
                    t.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 2));
                    box.addView(t, new LinearLayout.LayoutParams(MP, WC));
                    break;
                }
                case LINE: {
                    View v = new View(c);
                    v.setBackgroundColor(Ui.MUTED);
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MP, 1);
                    p.setMargins(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
                    box.addView(v, p);
                    break;
                }
                default: {
                    box.addView(new View(c), new LinearLayout.LayoutParams(MP, Ui.dp(c, 8)));
                }
            }
        }
        return box;
    }
}
