package iq.haji.alhagebb;

/** دوال نقية (بلا اعتماد على Android) قابلة للاختبار. */
public final class Util {
    private Util() {}

    /**
     * يوحّد رقم الهاتف. +964xxxxxxxxxx و00964xxxxxxxxxx تتحول إلى 0xxxxxxxxxx.
     * يُرجع null إذا كان الرقم مجهولاً أو غير صالح.
     */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String s = raw.replaceAll("[^0-9+]", "");
        if (s.startsWith("00")) s = "+" + s.substring(2);
        if (s.startsWith("+964")) s = "0" + s.substring(4);
        if (s.indexOf('+') > 0) return null;
        String digits = s.replace("+", "");
        if (digits.length() < 7 || digits.length() > 15) return null;
        return s;
    }

    /** خلية CSV نصية مع منع حقن الصيغ في Excel. */
    public static String csvText(String v) {
        if (v == null) v = "";
        if (!v.isEmpty()) {
            char c = v.charAt(0);
            if (c == '=' || c == '+' || c == '-' || c == '@') v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    /** خلية CSV لقيمة رقمية أو هاتف (بلا تعديل). */
    public static String csvRaw(String v) {
        if (v == null) v = "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
