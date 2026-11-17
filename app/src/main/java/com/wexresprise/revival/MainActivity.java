package com.wexresprise.revival;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private YearTheme theme;
    private FrameLayout root;
    private AvatarView avatarView;
    private Bitmap customShirt;
    private TextView nickLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = YearTheme.of(Prefs.year(this));
        getWindow().setStatusBarColor(theme.bg);
        getWindow().setNavigationBarColor(theme.bg);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        YearTheme now = YearTheme.of(Prefs.year(this));
        if (now.year != theme.year) {
            theme = now;
            render();
        } else if (avatarView != null) {
            customShirt = loadCustomShirt();
            avatarView.setAvatar(Prefs.avatar(this), customShirt);
            if (nickLabel != null) nickLabel.setText(Prefs.nick(this));
        }
    }

    private void render() {
        customShirt = loadCustomShirt();

        root = new FrameLayout(this);
        root.addView(new BackdropView(this, theme), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 22), Ui.dp(this, 26), Ui.dp(this, 22), Ui.dp(this, 28));

        // шапка
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView logo = Ui.label(this, "REVIVAL", 26, theme.text, true);
        logo.setLetterSpacing(0.12f);
        titles.addView(logo);
        TextView sub = Ui.label(this, "дизайн " + theme.year + " · " + theme.label, 12, theme.sub, false);
        sub.setLetterSpacing(0.04f);
        titles.addView(sub);
        header.addView(titles, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView design = Ui.button(this, theme, "Design customization", false);
        design.setTextSize(12);
        design.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        design.setBackground(Ui.round(AvatarArt.blend(theme.panel, theme.bg, 0.35f),
                Ui.dp(this, theme.radius + 10), AvatarArt.blend(theme.accent, theme.bg, 0.35f), Ui.dp(this, 1)));
        design.setOnClickListener(v -> showDesignPanel());
        header.addView(design);

        col.addView(header);

        // карточка персонажа
        FrameLayout card = new FrameLayout(this);
        card.setBackground(Ui.round(AvatarArt.blend(theme.panel, theme.bg, 0.18f),
                Ui.dp(this, theme.radius + 12), AvatarArt.blend(theme.accent, theme.bg, 0.60f), Ui.dp(this, 1)));
        LinearLayout.LayoutParams cardLp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 300));
        cardLp.topMargin = Ui.dp(this, 22);
        col.addView(card, cardLp);

        avatarView = new AvatarView(this);
        avatarView.setAvatar(Prefs.avatar(this), customShirt);
        FrameLayout.LayoutParams avLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        avLp.setMargins(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 58));
        card.addView(avatarView, avLp);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nameRow.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 12), 0);

        nickLabel = Ui.label(this, Prefs.nick(this), 16, theme.text, true);
        nickLabel.setSingleLine(true);
        nickLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameRow.addView(nickLabel, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView rename = Ui.label(this, "ник", 12, theme.accent, true);
        rename.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        rename.setBackground(Ui.round(AvatarArt.blend(theme.accent, theme.bg, 0.82f),
                Ui.dp(this, 14), 0, 0));
        rename.setOnClickListener(v -> askNick());
        nameRow.addView(rename);

        FrameLayout.LayoutParams nrLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48), Gravity.BOTTOM);
        card.addView(nameRow, nrLp);

        // большая кнопка игры
        TextView play = Ui.label(this, "ИГРАТЬ", 18, Ui.pickText(theme.accent), true);
        play.setGravity(Gravity.CENTER);
        play.setLetterSpacing(0.10f);
        play.setBackground(Ui.round(theme.accent, Ui.dp(this, theme.radius + 10), 0, 0));
        play.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 18));
        play.setOnClickListener(v -> startGame(null));
        LinearLayout.LayoutParams playLp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        playLp.topMargin = Ui.dp(this, 18);
        col.addView(play, playLp);

        // ряд кнопок
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = Ui.dp(this, 12);
        col.addView(row, rowLp);

        TextView multi = Ui.button(this, theme, "Мультиплеер", false);
        multi.setOnClickListener(v -> startActivity(new Intent(this, MultiplayerActivity.class)));
        LinearLayout.LayoutParams mlp = Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mlp.rightMargin = Ui.dp(this, 6);
        row.addView(multi, mlp);

        TextView place = Ui.button(this, theme, "Открыть .rbxl", false);
        place.setOnClickListener(v -> pickPlace());
        LinearLayout.LayoutParams plp = Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        plp.leftMargin = Ui.dp(this, 6);
        row.addView(place, plp);

        // нижняя строка-подсказка
        TextView hint = Ui.label(this,
                "Без аккаунта · без логина · скин и ник любые",
                11, theme.sub, false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hLp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.topMargin = Ui.dp(this, 18);
        col.addView(hint, hLp);

        scroll.addView(col, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // боковая вкладка каталога
        TextView side = Ui.label(this, "КАТАЛОГ", 13, theme.text, true);
        side.setGravity(Gravity.CENTER);
        side.setLetterSpacing(0.28f);
        side.setBackground(Ui.round(AvatarArt.blend(theme.accent, theme.bg, 0.72f),
                Ui.dp(this, 10), AvatarArt.blend(theme.accent, theme.bg, 0.30f), Ui.dp(this, 1)));
        side.setRotation(-90f);
        side.setOnClickListener(v -> startActivity(new Intent(this, CatalogActivity.class)));
        int tabW = Ui.dp(this, 180);
        int tabH = Ui.dp(this, 44);
        FrameLayout.LayoutParams sideLp = new FrameLayout.LayoutParams(tabW, tabH,
                Gravity.START | Gravity.CENTER_VERTICAL);
        sideLp.leftMargin = -(tabW - tabH) / 2;
        root.addView(side, sideLp);

        setContentView(root);
    }

    private Bitmap loadCustomShirt() {
        String path = Prefs.customShirt(this);
        if (path == null) return null;
        try {
            return BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            return null;
        }
    }

    private void askNick() {
        EditText input = new EditText(this);
        input.setText(Prefs.nick(this));
        input.setTextColor(theme.text);
        input.setHintTextColor(theme.sub);
        input.setHint("Любой ник: русский, пробелы, символы");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSelection(input.getText().length());
        int pad = Ui.dp(this, 20);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Никнейм")
                .setView(input)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String v = input.getText().toString();
                    if (v.trim().isEmpty()) v = "Player";
                    Prefs.setNick(this, v);
                    if (nickLabel != null) nickLabel.setText(v);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void startGame(String placeUri) {
        Intent i = new Intent(this, GameActivity.class);
        if (placeUri != null) i.putExtra(GameActivity.EXTRA_PLACE_URI, placeUri);
        RevivalSessionHolder.clear();
        startActivity(i);
    }

    private void pickPlace() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, 41);
        } catch (Exception e) {
            startActivity(new Intent(this, GameActivity.class));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 41 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            startGame(data.getData().toString());
        }
    }

    private void showDesignPanel() {
        final FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#E6060709"));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(Ui.round(AvatarArt.blend(theme.panel, theme.bg, 0.10f),
                Ui.dp(this, 18), AvatarArt.blend(theme.accent, theme.bg, 0.55f), Ui.dp(this, 1)));
        panel.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 14));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout tt = new LinearLayout(this);
        tt.setOrientation(LinearLayout.VERTICAL);
        TextView t = Ui.label(this, "Design customization", 18, theme.text, true);
        tt.addView(t);
        TextView s = Ui.label(this, "выбери стиль любого года", 12, theme.sub, false);
        tt.addView(s);
        head.addView(tt, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = Ui.label(this, "✕", 16, theme.text, true);
        close.setGravity(Gravity.CENTER);
        close.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 6));
        close.setBackground(Ui.round(AvatarArt.blend(theme.panel, theme.bg, 0.4f),
                Ui.dp(this, 14), 0, 0));
        close.setOnClickListener(v -> root.removeView(overlay));
        head.addView(close);

        panel.addView(head);

        ScrollView scroll = new ScrollView(this);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, Ui.dp(this, 14), 0, 0);

        YearTheme[] all = YearTheme.all();
        LinearLayout row = null;
        for (int i = 0; i < all.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rl = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                rl.bottomMargin = Ui.dp(this, 10);
                grid.addView(row, rl);
            }
            View chip = yearChip(all[i], overlay);
            LinearLayout.LayoutParams cl = Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cl.leftMargin = Ui.dp(this, 4);
            cl.rightMargin = Ui.dp(this, 4);
            row.addView(chip, cl);
        }

        scroll.addView(grid);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        panel.addView(scroll, slp);

        TextView note = Ui.label(this, "Стиль применяется сразу и запоминается", 11, theme.sub, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, Ui.dp(this, 10), 0, 0);
        panel.addView(note);

        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        plp.setMargins(Ui.dp(this, 12), Ui.dp(this, 40), Ui.dp(this, 12), Ui.dp(this, 20));
        overlay.addView(panel, plp);
        overlay.setOnClickListener(v -> root.removeView(overlay));
        panel.setClickable(true);

        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View yearChip(YearTheme y, View overlay) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        boolean active = y.year == theme.year;
        chip.setBackground(Ui.round(y.bg, Ui.dp(this, 10),
                active ? y.accent : AvatarArt.blend(y.panel, y.bg, 0.5f), Ui.dp(this, active ? 2 : 1)));
        chip.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));

        View bar = new View(this);
        bar.setBackground(Ui.round(y.accent, Ui.dp(this, 4), 0, 0));
        chip.addView(bar, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 6)));

        TextView year = Ui.label(this, String.valueOf(y.year), 15, y.text, true);
        year.setPadding(0, Ui.dp(this, 8), 0, 0);
        chip.addView(year);

        TextView label = Ui.label(this, y.label, 10, AvatarArt.blend(y.text, y.bg, 0.45f), false);
        chip.addView(label);

        chip.setOnClickListener(v -> {
            Prefs.setYear(this, y.year);
            theme = y;
            root.removeView(overlay);
            getWindow().setStatusBarColor(theme.bg);
            getWindow().setNavigationBarColor(theme.bg);
            render();
        });
        return chip;
    }

    /** Пустой держатель, чтобы одиночная игра не унаследовала сессию. */
    static final class RevivalSessionHolder {
        static void clear() {
            com.wexresprise.revival.net.RevivalSession.current = null;
        }
    }
}
