package com.wexresprise.revival;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import com.wexresprise.revival.net.NetCode;
import com.wexresprise.revival.net.RevivalSession;

public class MultiplayerActivity extends Activity {

    private YearTheme theme;
    private EditText nickInput;
    private EditText codeInput;
    private TextView status;
    private TextView codeLabel;
    private TextView hostButton;
    private LinearLayout hostBox;
    private RevivalSession session;
    private boolean started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = YearTheme.of(Prefs.year(this));
        build();
    }

    private void build() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(theme.bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 28));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = Ui.label(this, "‹", 26, theme.text, true);
        back.setGravity(Gravity.CENTER);
        back.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 14), Ui.dp(this, 4));
        back.setOnClickListener(v -> finish());
        header.addView(back);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(Ui.label(this, "Мультиплеер", 22, theme.text, true));
        titles.addView(Ui.label(this, "код вместо логина", 11, theme.sub, false));
        header.addView(titles, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        col.addView(header);

        // ник
        col.addView(sectionLabel("Никнейм"));
        nickInput = input(Prefs.nick(this), "Русский, английский, пробелы, символы");
        nickInput.setText(Prefs.nick(this));
        nickInput.setSelection(nickInput.getText().length());
        col.addView(nickInput);

        // хост
        col.addView(sectionLabel("Создать мир"));
        TextView hostHint = Ui.label(this,
                "Ты становишься хостом. Появится код — передай его другу.",
                12, theme.sub, false);
        hostHint.setPadding(0, 0, 0, Ui.dp(this, 10));
        col.addView(hostHint);

        hostButton = Ui.button(this, theme, "Хост", true);
        hostButton.setOnClickListener(v -> startHosting());
        col.addView(hostButton);

        hostBox = new LinearLayout(this);
        hostBox.setOrientation(LinearLayout.VERTICAL);
        hostBox.setVisibility(View.GONE);
        hostBox.setBackground(Ui.round(AvatarArt.blend(theme.panel, theme.bg, 0.15f),
                Ui.dp(this, theme.radius + 10), AvatarArt.blend(theme.accent, theme.bg, 0.4f), Ui.dp(this, 1)));
        hostBox.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));

        codeLabel = Ui.label(this, "——", 24, theme.text, true);
        codeLabel.setGravity(Gravity.CENTER);
        codeLabel.setLetterSpacing(0.12f);
        codeLabel.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        hostBox.addView(codeLabel);

        TextView copy = Ui.label(this, "скопировать код", 12, theme.accent, true);
        copy.setGravity(Gravity.CENTER);
        copy.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        copy.setOnClickListener(v -> copyCode());
        hostBox.addView(copy);

        status = Ui.label(this, "Ждём игрока…", 11, theme.sub, false);
        status.setGravity(Gravity.CENTER);
        hostBox.addView(status);

        LinearLayout.LayoutParams hbLp = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hbLp.topMargin = Ui.dp(this, 12);
        col.addView(hostBox, hbLp);

        // вход
        col.addView(sectionLabel("Войти по коду"));
        codeInput = input("", "RVL-XXXXX-XXXXX");
        codeInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        col.addView(codeInput);

        TextView join = Ui.button(this, theme, "Подключиться", false);
        join.setOnClickListener(v -> joinGame());
        LinearLayout.LayoutParams jl = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        jl.topMargin = Ui.dp(this, 10);
        col.addView(join, jl);

        TextView hint = Ui.label(this,
                "Телефоны должны быть в одной Wi-Fi сети. Аккаунт не нужен.",
                11, theme.sub, false);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(this, 18), 0, 0);
        col.addView(hint);

        scroll.addView(col);
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private TextView sectionLabel(String text) {
        TextView t = Ui.label(this, text, 12, theme.sub, true);
        t.setLetterSpacing(0.06f);
        t.setPadding(0, Ui.dp(this, 22), 0, Ui.dp(this, 8));
        return t;
    }

    private EditText input(String value, String hint) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setHint(hint);
        e.setTextColor(theme.text);
        e.setHintTextColor(AvatarArt.blend(theme.sub, theme.bg, 0.25f));
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setBackground(Ui.round(AvatarArt.blend(theme.panel, theme.bg, 0.20f),
                Ui.dp(this, theme.radius + 8), AvatarArt.blend(theme.panel, theme.bg, 0.6f), Ui.dp(this, 1)));
        e.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        return e;
    }

    private String nick() {
        String n = nickInput.getText().toString();
        if (n.trim().isEmpty()) n = "Player";
        Prefs.setNick(this, n);
        return n;
    }

    private void startHosting() {
        if (session != null) return;
        String n = nick();
        try {
            session = RevivalSession.host(n, Prefs.avatar(this));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть порт: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        session.setReadyCallback(() -> {
            started = true;
            startActivity(new Intent(this, GameActivity.class));
            finish();
        });

        hostBox.setVisibility(View.VISIBLE);
        hostButton.setEnabled(false);
        hostButton.setAlpha(0.5f);
        codeLabel.setText(session.code);
        status.setText("Адрес: " + session.address + "\nЖдём игрока…");
        codeInput.setText(session.code);
    }

    private void joinGame() {
        if (session != null) return;
        String n = nick();
        String code = codeInput.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "Введи код", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            session = RevivalSession.join(code, n, Prefs.avatar(this));
        } catch (Exception e) {
            Toast.makeText(this, "Не подключиться: " + e.getMessage(), Toast.LENGTH_LONG).show();
            session = null;
            return;
        }
        session.setReadyCallback(() -> {
            started = true;
            startActivity(new Intent(this, GameActivity.class));
            finish();
        });
        Toast.makeText(this, "Подключаемся…", Toast.LENGTH_SHORT).show();
    }

    private void copyCode() {
        if (session == null) return;
        android.content.ClipboardManager cm = (android.content.ClipboardManager)
                getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Revival", session.code));
            Toast.makeText(this, "Код скопирован", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (!started && session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }
}
