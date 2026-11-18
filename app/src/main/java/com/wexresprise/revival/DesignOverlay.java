package com.wexresprise.revival;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Выбор стиля: 21 год, применяется сразу. */
public class DesignOverlay extends FrameLayout {

    public interface Listener {
        void onYear(int year);
        void onClose();
    }

    private final YearTheme theme;
    private final Listener listener;

    public DesignOverlay(Context c, YearTheme theme, Listener listener) {
        super(c);
        this.theme = theme;
        this.listener = listener;
        setBackgroundColor(0xE607090D);
        setClickable(true);
        build();
    }

    @Override
    public void draw(android.graphics.Canvas canvas) {
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
        LinearLayout panel = new LinearLayout(getContext());
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(Ui.round(AvatarArt.blend(theme.panel, theme.bg, 0.10f), dp(14),
                AvatarArt.blend(theme.accent, theme.bg, 0.55f), dp(1)));
        panel.setPadding(dp(16), dp(14), dp(16), dp(10));
        panel.setClickable(true);

        LinearLayout head = new LinearLayout(getContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(getContext());
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(Ui.label(getContext(), "Design customization", 20, theme.text, true));
        titles.addView(Ui.label(getContext(), "стиль любого года, применяется сразу", 12, theme.sub, false));
        head.addView(titles, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = Ui.label(getContext(), "✕", 15, theme.text, true);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.round(AvatarArt.blend(theme.panel, theme.bg, 0.4f), dp(12), 0, 0));
        close.setPadding(dp(12), dp(5), dp(12), dp(7));
        close.setOnClickListener(v -> listener.onClose());
        head.addView(close);
        panel.addView(head);

        ScrollView scroll = new ScrollView(getContext());
        LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, dp(12), 0, 0);

        YearTheme[] all = YearTheme.all();
        LinearLayout row = null;
        for (int i = 0; i < all.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rl = Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                rl.bottomMargin = dp(8);
                grid.addView(row, rl);
            }
            View chip = chip(all[i]);
            LinearLayout.LayoutParams cl = Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cl.leftMargin = dp(3);
            cl.rightMargin = dp(3);
            row.addView(chip, cl);
        }
        scroll.addView(grid);
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        addView(panel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View chip(YearTheme y) {
        LinearLayout chip = new LinearLayout(getContext());
        chip.setOrientation(LinearLayout.VERTICAL);
        boolean active = y.year == theme.year;
        chip.setBackground(Ui.round(y.bg, dp(8),
                active ? y.accent : AvatarArt.blend(y.panel, y.bg, 0.5f), dp(active ? 2 : 1)));
        chip.setPadding(dp(9), dp(8), dp(9), dp(8));

        View bar = new View(getContext());
        bar.setBackground(Ui.round(y.accent, dp(3), 0, 0));
        chip.addView(bar, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, dp(5)));

        TextView year = Ui.label(getContext(), String.valueOf(y.year), 15, y.text, true);
        year.setPadding(0, dp(6), 0, 0);
        chip.addView(year);

        chip.addView(Ui.label(getContext(), y.label, 10,
                AvatarArt.blend(y.text, y.bg, 0.45f), false));

        chip.setOnClickListener(v -> listener.onYear(y.year));
        return chip;
    }
}
