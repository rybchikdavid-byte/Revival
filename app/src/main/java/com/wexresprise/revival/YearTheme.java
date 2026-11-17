package com.wexresprise.revival;

public final class YearTheme {

    public final int year;
    public final String label;
    public final int bg;
    public final int bg2;
    public final int panel;
    public final int panel2;
    public final int accent;
    public final int text;
    public final int sub;
    public final boolean chrome;
    public final int radius;

    private YearTheme(int year, String label, int bg, int bg2, int panel, int panel2,
                      int accent, int text, int sub, boolean chrome, int radius) {
        this.year = year;
        this.label = label;
        this.bg = bg;
        this.bg2 = bg2;
        this.panel = panel;
        this.panel2 = panel2;
        this.accent = accent;
        this.text = text;
        this.sub = sub;
        this.chrome = chrome;
        this.radius = radius;
    }

    public static final int[] YEARS = {
            2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014,
            2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023,
            2024, 2025, 2026
    };

    private static final YearTheme[] ALL = {
            new YearTheme(2006, "Classic Gray", 0xFF1B1F27, 0xFF252C38, 0xFF2C3543, 0xFF354050,
                    0xFF4C9A2A, 0xFFE9EDF2, 0xFF97A4B4, true, 2),
            new YearTheme(2007, "Green Bar", 0xFF20242C, 0xFF2B313C, 0xFF303845, 0xFF3A4352,
                    0xFFE0A72E, 0xFFECF0F5, 0xFF9DA9B8, true, 2),
            new YearTheme(2008, "Blue Web", 0xFF14202E, 0xFF1B2A3C, 0xFF233449, 0xFF2C3F57,
                    0xFF2F7FD1, 0xFFE6ECF4, 0xFF93A3B6, true, 2),
            new YearTheme(2009, "Builder", 0xFF101A15, 0xFF17241D, 0xFF1D2C24, 0xFF26382D,
                    0xFF4C9A2A, 0xFFE7EFE9, 0xFF94A79B, true, 3),
            new YearTheme(2010, "Studio", 0xFF0F1720, 0xFF16202B, 0xFF1B2836, 0xFF233343,
                    0xFF0078D7, 0xFFE5EBF3, 0xFF91A0B1, true, 3),
            new YearTheme(2011, "Old Blue", 0xFF11161E, 0xFF171D27, 0xFF1F2937, 0xFF283444,
                    0xFF1D6FD1, 0xFFE6EBF3, 0xFF95A1B2, true, 3),
            new YearTheme(2012, "Red Line", 0xFF171314, 0xFF1F191B, 0xFF2A2224, 0xFF362C2F,
                    0xFFC4281C, 0xFFF1E9EA, 0xFFB2A2A5, true, 3),
            new YearTheme(2013, "Sky", 0xFF101B24, 0xFF15232E, 0xFF1C2D3A, 0xFF243847,
                    0xFF00A2E8, 0xFFE4EEF5, 0xFF94A7B6, false, 4),
            new YearTheme(2014, "Ocean", 0xFF0E1826, 0xFF132030, 0xFF182A3E, 0xFF20344B,
                    0xFF1E90FF, 0xFFE4ECF6, 0xFF93A4BA, false, 4),
            new YearTheme(2015, "Mint", 0xFF0F1D1A, 0xFF14241F, 0xFF18302B, 0xFF1F3B34,
                    0xFF00B06B, 0xFFE3F0EA, 0xFF92ACA1, false, 4),
            new YearTheme(2016, "Revival", 0xFF191B1F, 0xFF1F2228, 0xFF23262C, 0xFF2B2F36,
                    0xFF00A2FF, 0xFFEDF1F7, 0xFF9AA6B6, false, 4),
            new YearTheme(2017, "Neon", 0xFF1B1D21, 0xFF212329, 0xFF26292E, 0xFF2E3238,
                    0xFF00B2EE, 0xFFEDF1F7, 0xFF9BA7B6, false, 6),
            new YearTheme(2018, "Slate", 0xFF15171A, 0xFF1A1D21, 0xFF202327, 0xFF272B30,
                    0xFF00A2FF, 0xFFECF0F5, 0xFF98A4B2, false, 5),
            new YearTheme(2019, "Amber", 0xFF121316, 0xFF17181C, 0xFF1C1E22, 0xFF24262B,
                    0xFFFFB000, 0xFFFFF3DE, 0xFFA79C88, false, 6),
            new YearTheme(2020, "Emerald", 0xFF0F1013, 0xFF131417, 0xFF191B1F, 0xFF202226,
                    0xFF00C896, 0xFFE2F4EE, 0xFF8FA8A1, false, 8),
            new YearTheme(2021, "Violet", 0xFF0D0D12, 0xFF111118, 0xFF17171F, 0xFF1E1E27,
                    0xFF7C5CFF, 0xFFEDEBFF, 0xFF9C97B8, false, 10),
            new YearTheme(2022, "Cyan", 0xFF0B0C11, 0xFF0F1016, 0xFF15161D, 0xFF1C1D25,
                    0xFF00C2FF, 0xFFE6F6FF, 0xFF8FA5B4, false, 12),
            new YearTheme(2023, "Rose", 0xFF0A0A0E, 0xFF0E0E13, 0xFF141419, 0xFF1B1B21,
                    0xFFFF4D6D, 0xFFFFE8EC, 0xFFB3989E, false, 14),
            new YearTheme(2024, "Indigo", 0xFF08090D, 0xFF0B0C11, 0xFF121318, 0xFF191A20,
                    0xFF4D8DFF, 0xFFE8EEFF, 0xFF929BAF, false, 16),
            new YearTheme(2025, "Lilac", 0xFF06060A, 0xFF090910, 0xFF101015, 0xFF17171D,
                    0xFFB06CFF, 0xFFF0E9FF, 0xFF9E93B5, false, 18),
            new YearTheme(2026, "Aurora", 0xFF04040A, 0xFF07070E, 0xFF0E0E15, 0xFF15151D,
                    0xFF00E0B8, 0xFFE4FFF9, 0xFF8DA9A4, false, 20)
    };

    public static YearTheme of(int year) {
        for (YearTheme t : ALL) {
            if (t.year == year) return t;
        }
        return ALL[10];
    }

    public static YearTheme[] all() {
        return ALL;
    }
}
