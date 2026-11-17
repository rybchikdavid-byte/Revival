package com.wexresprise.revival;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Показывает лог падения. Живёт в отдельном процессе, поэтому переживает краш. */
public class CrashActivity extends Activity {

    public static final String EXTRA_DIAG = "show_diag";

    private String log = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashLog.inCrashProcess = true;
        try {
            String crash = CrashLog.latest(this);
            if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_DIAG, false)) {
                String diag = Diag.all(this);
                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(crash)) {
                    sb.append(crash).append("\n\n======== дневник ========\n\n");
                }
                sb.append(diag);
                log = sb.toString();
            } else {
                log = crash;
            }
            if (TextUtils.isEmpty(log)) log = Diag.all(this);
            if (TextUtils.isEmpty(log)) {
                log = "Логов пока нет.\n\nЕсли приложение вылетит, сюда попадёт причина.";
            }
        } catch (Throwable t) {
            log = "Не удалось прочитать лог: " + t;
        }
        build();
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF12141A);
        root.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("Revival · лог");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titles.addView(title);
        TextView sub = new TextView(this);
        sub.setText("также лежит в Downloads/Revival-crash.txt");
        sub.setTextColor(0xFF8E99A8);
        sub.setTextSize(12);
        titles.addView(sub);
        head.addView(titles, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = button("✕", 0xFF3E4550);
        close.setOnClickListener(v -> finish());
        head.addView(close, new LinearLayout.LayoutParams(dp(44), dp(40)));
        root.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        bodyLp.topMargin = dp(10);
        root.addView(body, bodyLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0B0D12);
        TextView text = new TextView(this);
        text.setText(log);
        text.setTextColor(0xFFD6DCE4);
        text.setTextSize(10.5f);
        text.setTypeface(Typeface.MONOSPACE);
        text.setTextIsSelectable(true);
        text.setPadding(dp(10), dp(8), dp(10), dp(12));
        scroll.addView(text, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(scroll, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sideLp = new LinearLayout.LayoutParams(dp(178),
                ViewGroup.LayoutParams.MATCH_PARENT);
        sideLp.leftMargin = dp(10);
        side.setLayoutParams(sideLp);

        side.addView(action("Отправить", () -> shareLog()), lp());
        side.addView(action("Скопировать", () -> copyLog()), lp());
        side.addView(action("В Downloads", () -> saveLog()), lp());
        side.addView(action("Диагностика", () -> {
            Intent i = new Intent(this, CrashActivity.class);
            i.putExtra(EXTRA_DIAG, true);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        }), lp());
        side.addView(action("Безопасный режим", () -> {
            relaunch(true);
        }), lp());
        side.addView(action("Запустить снова", () -> relaunch(false)), lp());
        side.addView(action("Выйти", () -> finish()), lp());

        body.addView(side);
        setContentView(root);
    }

    private LinearLayout.LayoutParams lp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        p.bottomMargin = dp(6);
        return p;
    }

    private TextView action(String text, Runnable r) {
        TextView t = button(text, 0xFF2A303A);
        t.setTextSize(14);
        t.setOnClickListener(v -> {
            try {
                r.run();
            } catch (Throwable e) {
                Toast.makeText(this, "Ошибка: " + e, Toast.LENGTH_LONG).show();
            }
        });
        return t;
    }

    private TextView button(String text, int bg) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(15);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.round(bg, dp(4), 0, 0));
        return t;
    }

    private void shareLog() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Revival log");
        send.putExtra(Intent.EXTRA_TEXT, log);
        startActivity(Intent.createChooser(send, "Отправить лог"));
    }

    private void copyLog() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Revival log", log));
            Toast.makeText(this, "Лог скопирован", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLog() {
        String where = CrashLog.writeDownload(this, "Revival-log.txt", log);
        Toast.makeText(this, where == null ? "Не удалось сохранить" : "Сохранено: " + where,
                Toast.LENGTH_LONG).show();
        if (where == null) {
            Toast.makeText(this, "Используй «Отправить»", Toast.LENGTH_SHORT).show();
        }
    }

    private void relaunch(boolean safe) {
        try {
            Prefs.setSafeMode(this, safe);
            Intent i = new Intent(this, GameActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, "Не удалось запустить: " + t, Toast.LENGTH_LONG).show();
        }
        finish();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Throwable t) {
            Toast.makeText(this, "Ошибка экрана лога: " + t, Toast.LENGTH_LONG).show();
            return true;
        }
    }
}
