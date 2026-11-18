package com.wexresprise.revival;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.wexresprise.revival.net.RemotePlayer;
import com.wexresprise.revival.net.RevivalSession;

import java.util.List;

public class MenuOverlay extends FrameLayout {

    public static final int TAB_PEOPLE = 0;
    public static final int TAB_SETTINGS = 1;
    public static final int TAB_HELP = 2;

    private final GameActivity game;
    private final YearTheme theme;
    private final int accent = 0xFF1E9BFF;

    private LinearLayout content;
    private LinearLayout tabsRow;
    private int tab = TAB_SETTINGS;
    private FrameLayout dialogLayer;

    public MenuOverlay(GameActivity g, YearTheme t) {
        super(g);
        game = g;
        theme = t;
        setBackgroundColor(0xC2080A0F);
        setClickable(true);
        build();
    }

    @Override
    public void draw(Canvas canvas) {
        try {
            super.draw(canvas);
        } catch (Throwable t) {
            CrashLog.reportUi(getContext(), t);
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Throwable t) {
            CrashLog.reportUi(getContext(), t);
            return true;
        }
    }

    private int dp(float v) {
        return Ui.dp(getContext(), v);
    }

    private void build() {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(6), dp(10), dp(6), dp(6));

        // крестик
        FrameLayout x = new FrameLayout(getContext());
        x.setBackground(Ui.round(0xFFFFFFFF, dp(14), 0, 0));
        IconView xi = new IconView(getContext());
        xi.setKind(IconView.Kind.CLOSE);
        xi.setIconColor(0xFF1A1D22);
        x.addView(xi, new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
        x.setOnClickListener(v -> game.closeMenu());
        LinearLayout.LayoutParams xlp = Ui.llp(dp(56), dp(56));
        xlp.leftMargin = dp(6);
        xlp.bottomMargin = dp(14);
        col.addView(x, xlp);

        // вкладки
        tabsRow = new LinearLayout(getContext());
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        tabsRow.setBackground(Ui.round(0xCC3B424E, dp(4), 0, 0));
        col.addView(tabsRow, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(62)));

        // действия
        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams alp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        alp.topMargin = dp(10);
        col.addView(actions, alp);

        actions.addView(action("Leave", () -> game.leaveGame()), weight(1, dp(4)));
        actions.addView(action("Reset Character", () -> game.resetCharacter()), weight(2, dp(4)));
        actions.addView(action("Resume", () -> game.closeMenu()), weight(1, dp(4)));

        // контент
        content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        clp.topMargin = dp(8);
        col.addView(content, clp);

        addView(col, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        buildTabs();
        rebuild();
    }

    private LinearLayout.LayoutParams weight(float w, int margin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, w);
        lp.leftMargin = margin;
        lp.rightMargin = margin;
        return lp;
    }

    private View action(String text, Runnable onClick) {
        TextView t = Ui.label(getContext(), text, 18, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.round(0xCC3B424E, dp(3), 0, 0));
        t.setOnClickListener(v -> onClick.run());
        return t;
    }

    private void buildTabs() {
        tabsRow.removeAllViews();
        addTab("People", IconView.Kind.PEOPLE, TAB_PEOPLE);
        addTab("Settings", IconView.Kind.GEAR, TAB_SETTINGS);
        addTab("Help", IconView.Kind.HELP, TAB_HELP);
    }

    private void addTab(String name, IconView.Kind icon, int index) {
        LinearLayout tabView = new LinearLayout(getContext());
        tabView.setOrientation(LinearLayout.VERTICAL);
        tabView.setClickable(true);
        tabView.setOnClickListener(v -> {
            tab = index;
            buildTabs();
            rebuild();
        });

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(12), 0, dp(10));

        IconView iv = new IconView(getContext());
        iv.setKind(icon);
        iv.setIconColor(Color.WHITE);
        row.addView(iv, Ui.llp(dp(22), dp(22)));

        TextView label = Ui.label(getContext(), name, 17, Color.WHITE, true);
        label.setPadding(dp(8), 0, 0, 0);
        row.addView(label);

        tabView.addView(row, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        View underline = new View(getContext());
        underline.setBackgroundColor(index == tab ? accent : Color.TRANSPARENT);
        tabView.addView(underline, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));
        tabsRow.addView(tabView, Ui.llp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    private void rebuild() {
        content.removeAllViews();
        View v;
        if (tab == TAB_PEOPLE) {
            v = peopleTab();
        } else if (tab == TAB_SETTINGS) {
            v = settingsTab();
        } else {
            v = helpTab();
        }
        content.addView(v, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View scroll(View inner) {
        ScrollView s = new ScrollView(getContext());
        s.setVerticalScrollBarEnabled(true);
        s.addView(inner);
        return s;
    }

    // ------------------------------------------------------------- People

    private View peopleTab() {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout invite = new LinearLayout(getContext());
        invite.setOrientation(LinearLayout.HORIZONTAL);
        invite.setGravity(Gravity.CENTER_VERTICAL);
        invite.setBackground(Ui.round(0x7A5A6472, dp(3), 0, 0));
        invite.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView env = Ui.label(getContext(), "✉", 22, Color.WHITE, true);
        invite.addView(env);
        TextView t = Ui.label(getContext(), "Invite friends to join", 17, Color.WHITE, true);
        t.setPadding(dp(14), 0, 0, 0);
        invite.addView(t);
        LinearLayout.LayoutParams ilp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.bottomMargin = dp(6);
        col.addView(invite, ilp);

        RevivalSession s = game.sessionOrNull();
        addPerson(col, game.nick(), "you", true);
        if (s != null) {
            for (RemotePlayer p : s.snapshot()) {
                if (p.id == s.localId) continue;
                addPerson(col, p.nick, s.role == RevivalSession.HOST ? "guest" : "in world", false);
            }
            if (s.snapshot().isEmpty()) {
                TextView hint = Ui.label(getContext(), s.role == RevivalSession.HOST
                        ? "Пока никого. Открой Settings → Multiplayer и позови друга по коду."
                        : "Ждём остальных…", 13, 0xFFB9C0CA, false);
                hint.setPadding(dp(8), dp(14), dp(8), 0);
                col.addView(hint);
            }
        }
        return scroll(col);
    }

    private void addPerson(LinearLayout col, String nick, String note, boolean self) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Ui.round(self ? 0xB33A414C : 0x8A3A414C, dp(3), 0, 0));
        row.setPadding(dp(10), dp(8), dp(14), dp(8));

        AvatarView av = new AvatarView(getContext());
        av.setHeadOnly(true);
        Avatar a = self ? Prefs.avatar(getContext()) : new Avatar();
        av.setAvatar(a, null);
        row.addView(av, Ui.llp(dp(48), dp(48)));

        LinearLayout texts = new LinearLayout(getContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(12), 0, 0, 0);
        texts.addView(Ui.label(getContext(), nick, 18, Color.WHITE, true));
        texts.addView(Ui.label(getContext(), "@" + nick.replace(" ", ""), 13, 0xFFB0B7C1, false));
        row.addView(texts, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (note != null) {
            row.addView(Ui.label(getContext(), note, 12, 0xFFAFB6C0, false));
        }

        LinearLayout.LayoutParams lp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        col.addView(row, lp);
    }

    // ----------------------------------------------------------- Settings

    private View settingsTab() {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);

        col.addView(boolRow("Shift Lock Switch", Prefs.shiftLock(getContext()),
                v -> Prefs.setShiftLock(getContext(), v)));

        col.addView(choiceRow("Camera Mode", new String[]{"Default (Classic)", "Follow", "Fixed"},
                Prefs.cameraMode(getContext()), v -> Prefs.setCameraMode(getContext(), v)));

        col.addView(choiceRow("Movement Mode", new String[]{"Default (Keyboard)", "Thumbstick"},
                Prefs.movementMode(getContext()), v -> Prefs.setMovementMode(getContext(), v)));

        col.addView(sliderRow("Mouse Sensitivity", Prefs.sensitivity(getContext()),
                v -> Prefs.setSensitivity(getContext(), v)));

        col.addView(sliderRow("Volume", Prefs.volume(getContext()),
                v -> Prefs.setVolume(getContext(), v)));

        col.addView(boolRow("Fullscreen", Prefs.fullscreen(getContext()),
                v -> {
                    Prefs.setFullscreen(getContext(), v);
                    game.applyFullscreen(v);
                }));

        col.addView(choiceRow("Graphics Mode", new String[]{"Automatic", "Manual"},
                Prefs.graphicsMode(getContext()), v -> Prefs.setGraphicsMode(getContext(), v)));

        col.addView(sliderRow("Graphics Quality", Prefs.graphicsQuality(getContext()),
                v -> Prefs.setGraphicsQuality(getContext(), v)));

        col.addView(boolRow("Performance Stats", Prefs.perfStats(getContext()),
                v -> Prefs.setPerfStats(getContext(), v)));

        col.addView(choiceRow("Character", new String[]{"R6"}, 0, v -> {
        }));

        col.addView(designRow());
        col.addView(linkRow("Load Map (.rbxlx)", "Load Map (.rbxlx)", () -> game.openPlacePicker()));
        col.addView(linkRow("Multiplayer", "Multiplayer", () -> showMultiplayer()));
        col.addView(boolRow("3D graphics", !Prefs.safeMode(getContext()),
                v -> game.setSafeMode(!v)));
        col.addView(linkRow("Crash log", "Открыть", () -> game.openDiagnostics()));

        return scroll(col);
    }

    private LinearLayout baseRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Ui.round(0xB33A414C, dp(3), 0, 0));
        row.setPadding(dp(14), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        lp.bottomMargin = dp(5);
        row.setLayoutParams(lp);
        return row;
    }

    private View boolRow(String name, boolean on, BoolChange cb) {
        return choiceRow(name, new String[]{"Off", "On"}, on ? 1 : 0, v -> cb.on(v == 1));
    }

    private View choiceRow(String name, String[] values, int current, IntChange cb) {
        LinearLayout row = baseRow();
        final int[] cur = {Math.max(0, Math.min(values.length - 1, current))};

        row.addView(Ui.label(getContext(), name, 17, Color.WHITE, true),
                Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));

        TextView prev = Ui.label(getContext(), "‹", 24, Color.WHITE, true);
        prev.setGravity(Gravity.CENTER);
        prev.setPadding(dp(10), 0, dp(10), dp(2));
        prev.setOnClickListener(v -> {
            cur[0] = (cur[0] - 1 + values.length) % values.length;
            cb.on(cur[0]);
            rebuild();
        });
        row.addView(prev);

        TextView value = Ui.label(getContext(), values[cur[0]], 16, 0xFFC3C9D2, false);
        value.setGravity(Gravity.CENTER);
        value.setSingleLine(true);
        row.addView(value, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f));

        TextView next = Ui.label(getContext(), "›", 24, Color.WHITE, true);
        next.setGravity(Gravity.CENTER);
        next.setPadding(dp(10), 0, dp(10), dp(2));
        next.setOnClickListener(v -> {
            cur[0] = (cur[0] + 1) % values.length;
            cb.on(cur[0]);
            rebuild();
        });
        row.addView(next);
        return row;
    }

    private View sliderRow(String name, int value, IntChange cb) {
        LinearLayout row = baseRow();
        row.addView(Ui.label(getContext(), name, 17, Color.WHITE, true),
                Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));

        TextView prev = Ui.label(getContext(), "‹", 24, Color.WHITE, true);
        prev.setGravity(Gravity.CENTER);
        prev.setPadding(dp(10), 0, dp(8), dp(2));
        row.addView(prev);

        SegBar bar = new SegBar(getContext());
        bar.set(value, 10, v -> cb.on(Math.max(1, v)));
        row.addView(bar, Ui.llp(0, dp(26), 1.6f));

        TextView next = Ui.label(getContext(), "›", 24, Color.WHITE, true);
        next.setGravity(Gravity.CENTER);
        next.setPadding(dp(8), 0, dp(10), dp(2));
        next.setOnClickListener(v -> {
            int nv = Math.min(10, bar.value + 1);
            bar.set(nv);
            cb.on(Math.max(1, nv));
        });
        row.addView(next);
        prev.setOnClickListener(v -> {
            int nv = Math.max(1, bar.value - 1);
            bar.set(nv);
            cb.on(nv);
        });
        return row;
    }

    private View designRow() {
        LinearLayout row = baseRow();
        row.addView(Ui.label(getContext(), "Design customization", 17, Color.WHITE, true),
                Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));
        TextView open = Ui.label(getContext(), "Стиль " + theme.year, 16, accent, true);
        open.setGravity(Gravity.CENTER);
        open.setBackground(Ui.round(0x664A5462, dp(6), accent, dp(1)));
        open.setPadding(dp(14), dp(8), dp(14), dp(8));
        open.setOnClickListener(v -> game.openDesign());
        row.addView(open, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View linkRow(String name, String button, Runnable action) {
        LinearLayout row = baseRow();
        row.addView(Ui.label(getContext(), name, 17, Color.WHITE, true),
                Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));
        TextView b = Ui.label(getContext(), button, 16, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.round(0x664A5462, dp(4), 0x66FFFFFF, dp(1)));
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        b.setOnClickListener(v -> action.run());
        row.addView(b, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    // --------------------------------------------------------------- Help

    private View helpTab() {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(6), dp(4), dp(6), dp(10));
        col.addView(helpLine("Управление",
                "Левая половина экрана — джойстик, правая — поворот камеры, кнопка справа снизу — прыжок."));
        col.addView(helpLine("Мультиплеер",
                "Settings → Multiplayer. Нажми Host, получи код и передай другу. Он вводит код в ROOM CODE и жмёт Join. Оба телефона в одной Wi-Fi сети."));
        col.addView(helpLine("Скин",
                "Кнопка с кирпичиком сверху слева открывает каталог: футболки, штаны, лица, волосы и своя картинка."));
        col.addView(helpLine("Карта",
                "Settings → Load Map (.rbxlx) — открывает XML-место. Двоичные .rbxl пока не читаются."));
        col.addView(helpLine("Дизайн",
                "Settings → Design customization — 21 стиль с 2006 по 2026 год."));
        col.addView(helpLine("Аккаунт",
                "Его нет. Логин не нужен нигде."));
        return scroll(col);
    }

    private View helpLine(String title, String text) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.round(0x8A3A414C, dp(3), 0, 0));
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.addView(Ui.label(getContext(), title, 16, Color.WHITE, true));
        TextView t = Ui.label(getContext(), text, 14, 0xFFC3C9D2, false);
        t.setPadding(0, dp(4), 0, 0);
        box.addView(t);
        LinearLayout.LayoutParams lp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        box.setLayoutParams(lp);
        return box;
    }

    // -------------------------------------------------------- Multiplayer

    public void showMultiplayer() {
        if (dialogLayer != null) removeView(dialogLayer);
        dialogLayer = new FrameLayout(getContext());
        dialogLayer.setBackgroundColor(0x88000000);
        dialogLayer.setClickable(true);

        LinearLayout panel = new LinearLayout(getContext());
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(Ui.round(0xF2252A31, dp(4), 0x66000000, dp(1)));
        panel.setPadding(dp(20), dp(16), dp(20), dp(18));
        panel.setClickable(true);

        LinearLayout titleRow = new LinearLayout(getContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(Ui.label(getContext(), "Multiplayer", 24, Color.WHITE, true),
                Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = Ui.label(getContext(), "✕", 15, Color.WHITE, true);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.round(0xFF3E4550, dp(2), 0, 0));
        close.setPadding(dp(9), dp(5), dp(9), dp(5));
        close.setOnClickListener(v -> closeMultiplayer());
        titleRow.addView(close);
        panel.addView(titleRow);

        RevivalSession s = game.sessionOrNull();
        if (s != null) {
            panel.addView(hostingView(s));
        } else {
            panel.addView(joinView());
        }

        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                Math.min(dp(520), (int) (Math.max(getWidth(), dp(520)) * 0.62f)),
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        dialogLayer.addView(panel, plp);
        addView(dialogLayer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void closeMultiplayer() {
        if (dialogLayer != null) {
            removeView(dialogLayer);
            dialogLayer = null;
        }
    }

    public boolean hasMultiplayerOpen() {
        return dialogLayer != null;
    }

    private View joinView() {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);

        col.addView(fieldLabel("Username"));
        EditText user = whiteInput(game.nick());
        user.setInputType(InputType.TYPE_CLASS_TEXT);
        col.addView(user, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        col.addView(fieldLabel("Display name (optional)"));
        EditText display = whiteInput("");
        display.setHint("Same as username");
        col.addView(display, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        LinearLayout.LayoutParams gap = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        gap.topMargin = dp(10);
        col.addView(new View(getContext()), gap);

        TextView collision = Ui.label(getContext(), "Player collisions: " + (Prefs.collisions(getContext()) ? "On" : "Off"),
                16, Color.WHITE, false);
        collision.setGravity(Gravity.CENTER);
        collision.setBackground(Ui.round(0x99404954, dp(2), 0, 0));
        collision.setPadding(0, dp(12), 0, dp(12));
        collision.setOnClickListener(v -> {
            boolean nv = !Prefs.collisions(getContext());
            Prefs.setCollisions(getContext(), nv);
            collision.setText("Player collisions: " + (nv ? "On" : "Off"));
        });
        col.addView(collision, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        TextView host = Ui.label(getContext(), "Host", 17, Color.WHITE, false);
        host.setGravity(Gravity.CENTER);
        host.setBackground(Ui.round(0x99404954, dp(2), 0, 0));
        host.setPadding(0, dp(12), 0, dp(12));
        host.setOnClickListener(v -> {
            String n = user.getText().toString().trim();
            if (n.isEmpty()) n = "Player";
            game.setNick(n);
            game.hostRoom();
        });
        LinearLayout.LayoutParams hlp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        hlp.topMargin = dp(8);
        col.addView(host, hlp);

        TextView or = Ui.label(getContext(), "or join a friend", 14, Color.WHITE, false);
        or.setGravity(Gravity.CENTER);
        or.setPadding(0, dp(16), 0, dp(8));
        col.addView(or);

        LinearLayout joinRow = new LinearLayout(getContext());
        joinRow.setOrientation(LinearLayout.HORIZONTAL);
        final EditText code = whiteInput("");
        code.setHint("ROOM CODE");
        code.setSingleLine(true);
        joinRow.addView(code, Ui.llp(0, dp(44), 1.6f));
        TextView join = Ui.label(getContext(), "Join", 17, Color.WHITE, false);
        join.setGravity(Gravity.CENTER);
        join.setBackground(Ui.round(0x99404954, dp(2), 0, 0));
        join.setOnClickListener(v -> {
            String c = code.getText().toString().trim();
            if (c.isEmpty()) return;
            try {
                String n = user.getText().toString().trim();
                if (n.isEmpty()) n = "Player";
                game.setNick(n);
                game.joinRoom(c);
                closeMultiplayer();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Не подключиться: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        LinearLayout.LayoutParams jlp = Ui.llp(0, dp(44), 1f);
        jlp.leftMargin = dp(8);
        joinRow.addView(join, jlp);
        col.addView(joinRow, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        return col;
    }

    private View hostingView(RevivalSession s) {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);

        TextView info = Ui.label(getContext(), "You are hosting. Send this code to your friends:",
                15, Color.WHITE, false);
        col.addView(info);

        FrameLayout box = new FrameLayout(getContext());
        box.setBackground(Ui.round(0xFF11141A, dp(2), 0, 0));
        TextView code = Ui.label(getContext(), s.code, 32, Color.WHITE, true);
        code.setGravity(Gravity.CENTER);
        code.setLetterSpacing(0.08f);
        code.setPadding(dp(10), dp(16), dp(10), dp(16));
        box.addView(code, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams blp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(8);
        col.addView(box, blp);

        TextView copy = Ui.label(getContext(), "Copy code", 17, Color.WHITE, false);
        copy.setGravity(Gravity.CENTER);
        copy.setBackground(Ui.round(0x99404954, dp(2), 0, 0));
        copy.setPadding(0, dp(12), 0, dp(12));
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(GameActivity.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Revival", s.code));
                Toast.makeText(getContext(), "Код скопирован", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams clp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        clp.topMargin = dp(6);
        col.addView(copy, clp);

        TextView collision = Ui.label(getContext(), "Player collisions: " + (Prefs.collisions(getContext()) ? "On" : "Off"),
                16, Color.WHITE, false);
        collision.setGravity(Gravity.CENTER);
        collision.setBackground(Ui.round(0x99404954, dp(2), 0, 0));
        collision.setPadding(0, dp(12), 0, dp(12));
        collision.setOnClickListener(v -> {
            boolean nv = !Prefs.collisions(getContext());
            Prefs.setCollisions(getContext(), nv);
            collision.setText("Player collisions: " + (nv ? "On" : "Off"));
        });
        LinearLayout.LayoutParams klp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        klp.topMargin = dp(6);
        col.addView(collision, klp);

        TextView players = Ui.label(getContext(), "Players", 15, Color.WHITE, false);
        players.setPadding(0, dp(14), 0, dp(6));
        col.addView(players);

        LinearLayout list = new LinearLayout(getContext());
        list.setOrientation(LinearLayout.VERTICAL);
        addPlayerLine(list, game.nick(), "you, host");
        for (RemotePlayer p : s.snapshot()) {
            if (p.id == s.localId) continue;
            addPlayerLine(list, p.nick, "guest");
        }
        col.addView(list);

        TextView leave = Ui.label(getContext(), "Leave", 17, Color.WHITE, false);
        leave.setGravity(Gravity.CENTER);
        leave.setBackground(Ui.round(0x99404954, dp(2), 0, 0));
        leave.setPadding(0, dp(12), 0, dp(12));
        leave.setOnClickListener(v -> {
            game.leaveRoom();
            closeMultiplayer();
        });
        LinearLayout.LayoutParams llp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        llp.topMargin = dp(10);
        col.addView(leave, llp);

        return col;
    }

    private void addPlayerLine(LinearLayout list, String nick, String note) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(Ui.round(0x66404954, dp(2), 0, 0));
        row.addView(Ui.label(getContext(), nick, 15, Color.WHITE, false));
        TextView n = Ui.label(getContext(), note, 13, 0xFFAAB2BD, false);
        n.setPadding(dp(10), 0, 0, 0);
        row.addView(n);
        LinearLayout.LayoutParams lp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        list.addView(row, lp);
    }

    public void redrawMultiplayer() {
        if (dialogLayer != null) showMultiplayer();
    }

    public void onHostingStarted() {
        if (dialogLayer != null) showMultiplayer();
    }

    public void onRosterChanged() {
        if (tab == TAB_PEOPLE) rebuild();
        RevivalSession s = game.sessionOrNull();
        if (dialogLayer != null && s != null && s.role == RevivalSession.HOST) {
            showMultiplayer();
        }
    }

    private EditText whiteInput(String value) {
        EditText e = new EditText(getContext());
        e.setText(value);
        e.setTextColor(0xFF111318);
        e.setHintTextColor(0xFF6F7681);
        e.setTextSize(17);
        e.setSingleLine(true);
        e.setBackgroundColor(0xFFFFFFFF);
        e.setPadding(dp(10), dp(6), dp(10), dp(6));
        return e;
    }

    private TextView fieldLabel(String text) {
        TextView t = Ui.label(getContext(), text, 15, Color.WHITE, false);
        t.setPadding(0, dp(10), 0, dp(6));
        return t;
    }

    // ------------------------------------------------------------- slider

    public interface IntChange {
        void on(int value);
    }

    public interface BoolChange {
        void on(boolean value);
    }

    static class SegBar extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int value = 5;
        private int max = 10;
        private IntChange listener;

        SegBar(android.content.Context c) {
            super(c);
        }

        void set(int v, int max, IntChange l) {
            this.value = v;
            this.max = max;
            this.listener = l;
            invalidate();
        }

        void set(int v) {
            value = Math.max(1, Math.min(max, v));
            invalidate();
        }

        @Override
        public void draw(Canvas canvas) {
            try {
                super.draw(canvas);
            } catch (Throwable t) {
                CrashLog.reportUi(getContext(), t);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int n = max;
            float gap = Ui.dp(getContext(), 3);
            float w = (getWidth() - gap * (n - 1)) / n;
            float h = Math.min(getHeight(), Ui.dp(getContext(), 18));
            float top = (getHeight() - h) / 2f;
            for (int i = 0; i < n; i++) {
                float left = i * (w + gap);
                p.setColor(i < value ? 0xFF1E9BFF : 0x664A5460);
                canvas.drawRoundRect(left, top, left + w, top + h, Ui.dp(getContext(), 2), Ui.dp(getContext(), 2), p);
            }
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent e) {
            try {
                if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN
                        || e.getActionMasked() == android.view.MotionEvent.ACTION_MOVE) {
                    float x = e.getX();
                    float cell = getWidth() / (float) max;
                    int v = (int) (x / cell) + 1;
                    v = Math.max(1, Math.min(max, v));
                    if (v != value) {
                        value = v;
                        invalidate();
                        if (listener != null) listener.on(value);
                    }
                }
            } catch (Throwable t) {
                CrashLog.reportUi(getContext(), t);
            }
            return true;
        }
    }
}
