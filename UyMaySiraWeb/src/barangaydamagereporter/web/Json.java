package barangaydamagereporter.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Very small JSON helper (no library needed): builds JSON text and reads flat JSON objects. */
public final class Json {

    /** Text that is already JSON (for example a nested object or array). */
    public static final class Raw {
        private final String text;
        private Raw(String text) { this.text = text; }
    }

    private Json() { }

    public static Raw raw(String json) {
        return new Raw(json);
    }

    public static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    public static String value(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof Raw) {
            return ((Raw) o).text;
        }
        if (o instanceof Number || o instanceof Boolean) {
            return o.toString();
        }
        return quote(o.toString());
    }

    /** obj("name", "Juan", "age", 20) gives {"name":"Juan","age":20} */
    public static String obj(Object... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote(String.valueOf(keyValues[i]))).append(':').append(value(keyValues[i + 1]));
        }
        return sb.append('}').toString();
    }

    /** items must already be JSON text. */
    public static String array(List<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    /** Reads a flat JSON object like {"a":"x","b":5} into a map of text values. */
    public static Map<String, String> parse(String text) {
        Map<String, String> map = new HashMap<>();
        int[] pos = {0};
        skip(text, pos);
        expect(text, pos, '{');
        while (true) {
            skip(text, pos);
            if (peek(text, pos) == '}') {
                break;
            }
            String key = readString(text, pos);
            skip(text, pos);
            expect(text, pos, ':');
            skip(text, pos);
            String value;
            if (peek(text, pos) == '"') {
                value = readString(text, pos);
            } else {
                int start = pos[0];
                while (pos[0] < text.length() && text.charAt(pos[0]) != ',' && text.charAt(pos[0]) != '}') {
                    pos[0]++;
                }
                value = text.substring(start, pos[0]).trim();
                if (value.equals("null")) {
                    value = "";
                }
            }
            map.put(key, value);
            skip(text, pos);
            if (peek(text, pos) == ',') {
                pos[0]++;
            }
        }
        return map;
    }

    private static char peek(String t, int[] pos) {
        if (pos[0] >= t.length()) {
            throw new IllegalArgumentException("Invalid request data.");
        }
        return t.charAt(pos[0]);
    }

    private static void skip(String t, int[] pos) {
        while (pos[0] < t.length() && Character.isWhitespace(t.charAt(pos[0]))) {
            pos[0]++;
        }
    }

    private static void expect(String t, int[] pos, char c) {
        if (peek(t, pos) != c) {
            throw new IllegalArgumentException("Invalid request data.");
        }
        pos[0]++;
    }

    private static String readString(String t, int[] pos) {
        expect(t, pos, '"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = peek(t, pos);
            pos[0]++;
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                char e = peek(t, pos);
                pos[0]++;
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (pos[0] + 4 > t.length()) {
                            throw new IllegalArgumentException("Invalid request data.");
                        }
                        sb.append((char) Integer.parseInt(t.substring(pos[0], pos[0] + 4), 16));
                        pos[0] += 4;
                        break;
                    default: sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
    }
}
