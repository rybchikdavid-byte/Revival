package com.wexresprise.revival.place;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Чтение мест в формате .rbxlx (XML). */
public final class PlaceLoader {

    private PlaceLoader() {
    }

    public static List<PlacePart> load(InputStream in) throws Exception {
        List<PlacePart> out = new ArrayList<>();
        XmlPullParser p = Xml.newPullParser();
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        p.setInput(in, null);

        String prev = "";
        String parent = "";
        String cls = null;
        PlacePart cur = null;
        String prop = null;
        String propType = null;
        String child = null;
        StringBuilder childText = new StringBuilder();
        StringBuilder scalar = new StringBuilder();
        Map<String, String> children = new HashMap<>();

        int ev = p.getEventType();
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                String name = p.getName();
                parent = prev;
                prev = name;

                if ("Item".equals(name)) {
                    String c = p.getAttributeValue(null, "class");
                    cur = new PlacePart();
                    cur.cls = c == null ? "" : c;
                    cls = cur.cls;
                    prop = null;
                    children.clear();
                } else if (cur != null) {
                    String pname = p.getAttributeValue(null, "name");
                    boolean propertyStart = pname != null && "Properties".equals(parent) && prop == null;
                    if (propertyStart) {
                        prop = pname;
                        propType = name;
                        scalar.setLength(0);
                        children.clear();
                    } else if (prop != null) {
                        child = name;
                        childText.setLength(0);
                    }
                }
                if ("Properties".equals(name)) {
                    prop = null;
                }
            } else if (ev == XmlPullParser.TEXT && prop != null) {
                if (child != null) {
                    childText.append(p.getText());
                } else {
                    scalar.append(p.getText());
                }
            } else if (ev == XmlPullParser.END_TAG) {
                String name = p.getName();
                if (prop != null && child != null && name.equals(child)) {
                    children.put(child, childText.toString().trim());
                    child = null;
                } else if (prop != null && name.equals(propType)) {
                    apply(cur, prop, propType, scalar.toString().trim(), children);
                    prop = null;
                    propType = null;
                } else if ("Item".equals(name)) {
                    if (cur != null && isPart(cls)) {
                        if (cur.sx > 0 && cur.sy > 0 && cur.sz > 0) out.add(cur);
                    }
                    cur = null;
                    cls = null;
                    prop = null;
                    children.clear();
                }
                prev = name;
            }
            ev = p.next();
        }
        return out;
    }

    private static boolean isPart(String cls) {
        if (cls == null) return false;
        switch (cls) {
            case "Part":
            case "WedgePart":
            case "CornerWedgePart":
            case "TrussPart":
            case "SpawnLocation":
            case "MeshPart":
            case "UnionOperation":
                return true;
            default:
                return false;
        }
    }

    private static void apply(PlacePart part, String prop, String type, String value, Map<String, String> children) {
        if (part == null) return;
        switch (prop) {
            case "size":
            case "Size": {
                part.sx = f(children.get("X"), part.sx);
                part.sy = f(children.get("Y"), part.sy);
                part.sz = f(children.get("Z"), part.sz);
                break;
            }
            case "CFrame":
            case "CoordinateFrame": {
                if (!children.isEmpty()) {
                    part.x = f(children.get("X"), 0);
                    part.y = f(children.get("Y"), 0);
                    part.z = f(children.get("Z"), 0);
                    float r00 = f(children.get("R00"), 1);
                    float r02 = f(children.get("R02"), 0);
                    if (children.containsKey("R00") || children.containsKey("R02")) {
                        part.yaw = (float) Math.toDegrees(Math.atan2(r02, r00));
                    }
                } else if (!value.isEmpty()) {
                    String[] n = value.split("[\\s,]+");
                    if (n.length >= 3) {
                        part.x = f(n[0], 0);
                        part.y = f(n[1], 0);
                        part.z = f(n[2], 0);
                    }
                    if (n.length >= 5) {
                        part.yaw = (float) Math.toDegrees(Math.atan2(f(n[3], 0), f(n[4], 1)));
                    }
                }
                break;
            }
            case "position": {
                part.x = f(children.get("X"), 0);
                part.y = f(children.get("Y"), 0);
                part.z = f(children.get("Z"), 0);
                break;
            }
            case "Color3uint8": {
                try {
                    long v = Long.parseLong(value);
                    part.color = (int) v | 0xFF000000;
                } catch (Exception ignored) {
                }
                break;
            }
            case "Color3": {
                int r = (int) (f(children.get("R"), 0.6f) * 255);
                int g = (int) (f(children.get("G"), 0.6f) * 255);
                int b = (int) (f(children.get("B"), 0.6f) * 255);
                part.color = 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
                break;
            }
            case "BrickColor": {
                try {
                    part.color = brickColor(Integer.parseInt(value));
                } catch (Exception ignored) {
                }
                break;
            }
            default:
                break;
        }
    }

    private static int brickColor(int id) {
        switch (id) {
            case 1: return 0xFFF2F3F3;
            case 21: return 0xFFC4281C;
            case 23: return 0xFF0D69AC;
            case 24: return 0xFFF5CD30;
            case 28: return 0xFF287F47;
            case 37: return 0xFF4B974B;
            case 194: return 0xFFA3A2A5;
            case 199: return 0xFF635F62;
            case 208: return 0xFFE5E4DF;
            case 1009: return 0xFFFFF27E;
            default: return 0xFF9AA0A8;
        }
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static float f(String s, float def) {
        if (s == null) return def;
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
