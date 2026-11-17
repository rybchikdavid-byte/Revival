package com.wexresprise.revival;

public final class Catalog {

    public static final int KIND_SHIRT = 0;
    public static final int KIND_PANTS = 1;
    public static final int KIND_FACE = 2;
    public static final int KIND_HAIR = 3;

    public static final String[] TITLES = {"Футболки", "Штаны", "Лица", "Волосы"};

    public static final String[] SHIRT_NAMES = {
            "Классика", "Полоска", "Звезда", "Ромбы", "Молния", "Клетка", "Сердце", "Пиксель"
    };
    public static final int[] SHIRT_COLORS = {
            0xFF3B6EA5, 0xFFD94A3D, 0xFF2E8B57, 0xFF6B4FA0,
            0xFFE0A72E, 0xFF30343B, 0xFFC64B8C, 0xFF4AA3A0
    };

    public static final String[] PANTS_NAMES = {
            "Синие", "Чёрные", "Серые", "Хаки", "Красные", "Джинс"
    };
    public static final int[] PANTS_COLORS = {
            0xFF2F4F7A, 0xFF23262B, 0xFF5A616B, 0xFF6E6A46,
            0xFF8C3030, 0xFF3A5A86
    };

    public static final String[] FACE_NAMES = {
            "Улыбка", "Широкая", "Подмигивает", "Язык",
            "Злая", "Крутая", "Сонная", "Робот"
    };

    public static final String[] HAIR_NAMES = {
            "Короткие", "Ёжик", "Блонд", "Рыжие",
            "Кепка", "Афро", "Ирокез", "Лысый"
    };
    public static final int[] HAIR_COLORS = {
            0xFF4A3525, 0xFF1E1B18, 0xFFD9B45B, 0xFFA8452A,
            0xFF2E5C8A, 0xFF2A211C, 0xFF8E2F5E, 0x00000000
    };

    private Catalog() {
    }

    public static int count(int kind) {
        switch (kind) {
            case KIND_SHIRT: return SHIRT_NAMES.length;
            case KIND_PANTS: return PANTS_NAMES.length;
            case KIND_FACE: return FACE_NAMES.length;
            default: return HAIR_NAMES.length;
        }
    }

    public static String name(int kind, int index) {
        switch (kind) {
            case KIND_SHIRT: return SHIRT_NAMES[clamp(index, SHIRT_NAMES.length)];
            case KIND_PANTS: return PANTS_NAMES[clamp(index, PANTS_NAMES.length)];
            case KIND_FACE: return FACE_NAMES[clamp(index, FACE_NAMES.length)];
            default: return HAIR_NAMES[clamp(index, HAIR_NAMES.length)];
        }
    }

    public static int shirtColor(int index) {
        return SHIRT_COLORS[clamp(index, SHIRT_COLORS.length)];
    }

    public static int pantsColor(int index) {
        return PANTS_COLORS[clamp(index, PANTS_COLORS.length)];
    }

    public static int hairColor(int index) {
        return HAIR_COLORS[clamp(index, HAIR_COLORS.length)];
    }

    private static int clamp(int v, int n) {
        if (v < 0) return 0;
        if (v >= n) return n - 1;
        return v;
    }
}
