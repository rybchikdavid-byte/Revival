package com.wexresprise.revival;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.wexresprise.revival.gl.WorldRenderer;
import com.wexresprise.revival.net.RevivalSession;
import com.wexresprise.revival.place.PlaceLoader;
import com.wexresprise.revival.place.PlacePart;

import java.io.InputStream;
import java.util.List;

public class GameActivity extends Activity implements RevivalSession.Listener, HudView.Callback {

    public static final String EXTRA_PLACE_URI = "place_uri";

    private GLSurfaceViewBridge gl;
    private WorldRenderer renderer;
    private FlatWorldView flat;
    private final World world = new World();
    private RevivalSession session;
    private YearTheme theme;

    private FrameLayout root;
    private MenuOverlay menu;
    private DesignOverlay design;

    private LinearLayout chatLog;
    private FrameLayout chatBar;
    private EditText chatInput;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private boolean destroyed;

    private final Runnable positionLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            RevivalSession s = session;
            if (s != null && s.role != RevivalSession.SOLO) {
                s.sendPosition(world.x, world.y, world.z, world.yaw);
            }
            handler.postDelayed(this, 120);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashLog.inCrashProcess = false;
        try {
            setup(savedInstanceState);
        } catch (Throwable t) {
            try {
                Diag.line(this, "onCreate failed: " + Log.getStackTraceString(t));
                CrashLog.save(this, Thread.currentThread(), t);
            } catch (Throwable ignored) {
            }
            showEmergency(t);
        }
    }

    private void setup(Bundle savedInstanceState) {
        theme = YearTheme.of(Prefs.year(this));
        if (Diag.fails(this) >= 3) {
            showRecovery();
            return;
        }
        session = RevivalSession.current;
        if (session == null) {
            session = RevivalSession.solo(Prefs.nick(this), Prefs.avatar(this));
        }
        session.listener = this;

        world.shirt = Prefs.shirt(this);
        world.pants = Prefs.pants(this);
        world.face = Prefs.face(this);
        world.hair = Prefs.hair(this);
        world.camYaw = 0f;

        applyFullscreen(Prefs.fullscreen(this));
        buildUi();
        addSystemLine("Revival: чат готов. Напиши /help, если что.");
        if (Prefs.safeMode(this)) {
            addSystemLine("Безопасный режим: 3D выключен. Settings → 3D graphics вернёт его.");
        }
        loadPlaceFromIntent(getIntent());
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private void buildUi() {
        root = new FrameLayout(this);
        boolean safe = Prefs.safeMode(this);

        if (safe) {
            flat = new FlatWorldView(this, world);
            root.addView(flat, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            gl = null;
            renderer = null;
        } else {
            flat = null;
            gl = new GLSurfaceViewBridge(this);
            renderer = new WorldRenderer(world);
            renderer.setSky(0xFF8FC9EE);
            renderer.setErrorListener(t -> runOnUiThread(() -> onRendererError(t)));
            gl.setRenderer(renderer);
            root.addView(gl, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        HudView hud = new HudView(this, world, renderer);
        hud.setCallback(this);
        root.addView(hud, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // верхние кнопки
        LinearLayout leftTop = new LinearLayout(this);
        leftTop.setOrientation(LinearLayout.HORIZONTAL);
        leftTop.addView(iconButton(IconView.Kind.BRICK, () -> openCatalog()));
        LinearLayout.LayoutParams gap = Ui.llp(dp(8), dp(1));
        leftTop.addView(new View(this), gap);
        leftTop.addView(iconButton(IconView.Kind.CHAT, this::focusChat));

        FrameLayout.LayoutParams leftLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        leftLp.setMargins(dp(12), dp(12), 0, 0);
        root.addView(leftTop, leftLp);

        LinearLayout rightTop = new LinearLayout(this);
        rightTop.setOrientation(LinearLayout.HORIZONTAL);
        rightTop.addView(iconButton(IconView.Kind.PALETTE, this::openDesign));
        LinearLayout.LayoutParams gap2 = Ui.llp(dp(8), dp(1));
        rightTop.addView(new View(this), gap2);
        rightTop.addView(iconButton(IconView.Kind.MORE, this::openMenu));

        FrameLayout.LayoutParams rightLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        rightLp.setMargins(0, dp(12), dp(12), 0);
        root.addView(rightTop, rightLp);

        buildChat();
        setContentView(root);
        maybeBanner();
    }

    private View iconButton(IconView.Kind kind, Runnable action) {
        FrameLayout b = new FrameLayout(this);
        b.setBackground(Ui.round(0xB33B424E, dp(14), 0x33FFFFFF, dp(1)));
        b.setClickable(true);
        IconView icon = new IconView(this);
        icon.setKind(kind);
        icon.setIconColor(Color.WHITE);
        b.addView(icon, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        b.setOnClickListener(v -> {
            try {
                action.run();
            } catch (Throwable t) {
                CrashLog.reportUi(this, t);
            }
        });
        b.setLayoutParams(Ui.llp(dp(56), dp(56)));
        return b;
    }

    private void buildChat() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(Ui.round(0x4D000000, dp(4), 0, 0));

        chatLog = new LinearLayout(this);
        chatLog.setOrientation(LinearLayout.VERTICAL);
        chatLog.setPadding(dp(10), dp(8), dp(10), dp(6));
        panel.addView(chatLog, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = Ui.label(this, "To chat click here", 14, 0xFFE7EBF0, true);
        hint.setPadding(dp(10), dp(8), dp(10), dp(8));
        hint.setBackground(Ui.round(0xCC8B98A8, dp(2), 0, 0));
        hint.setOnClickListener(v -> {
            try {
                focusChat();
            } catch (Throwable t) {
                CrashLog.reportUi(this, t);
            }
        });
        panel.addView(hint, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dp(340), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        lp.setMargins(dp(12), dp(80), 0, 0);
        root.addView(panel, lp);

        // полоса ввода
        chatBar = new FrameLayout(this);
        chatBar.setBackgroundColor(0xD910131A);
        chatBar.setPadding(dp(10), dp(6), dp(10), dp(6));
        chatBar.setVisibility(View.GONE);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        chatInput = new EditText(this);
        chatInput.setHint("Сообщение");
        chatInput.setTextColor(Color.WHITE);
        chatInput.setHintTextColor(0xFF8A93A0);
        chatInput.setTextSize(15);
        chatInput.setSingleLine(true);
        chatInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        chatInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        chatInput.setBackground(Ui.round(0xFF1B1F27, dp(8), 0xFF2E3642, dp(1)));
        chatInput.setPadding(dp(12), dp(6), dp(12), dp(6));
        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            sendChat();
            return true;
        });
        bar.addView(chatInput, Ui.llp(0, dp(44), 1f));

        TextView send = Ui.label(this, "➤", 18, Color.WHITE, true);
        send.setGravity(Gravity.CENTER);
        send.setBackground(Ui.round(0xFF1E9BFF, dp(8), 0, 0));
        send.setPadding(dp(16), dp(8), dp(16), dp(8));
        send.setOnClickListener(v -> {
            try {
                sendChat();
            } catch (Throwable t) {
                CrashLog.reportUi(this, t);
            }
        });
        LinearLayout.LayoutParams slp = Ui.llp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
        slp.leftMargin = dp(8);
        send.setLayoutParams(slp);
        bar.addView(send);

        chatBar.addView(bar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL));

        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        blp.setMargins(dp(210), 0, dp(210), dp(10));
        root.addView(chatBar, blp);
    }

    private void focusChat() {
        if (chatBar == null || chatInput == null) return;
        chatBar.setVisibility(View.VISIBLE);
        chatInput.requestFocus();
        InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (im != null) im.showSoftInput(chatInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void sendChat() {
        if (chatInput == null) return;
        String text = chatInput.getText().toString().trim();
        if (text.isEmpty()) return;
        chatInput.setText("");
        if (text.equals("/help") || text.equals("/?")) {
            addSystemLine("Команды: /help — помощь, /clear — очистить чат, /reset — вернуться на спавн.");
            return;
        }
        if (text.equals("/clear")) {
            chatLog.removeAllViews();
            return;
        }
        if (text.equals("/reset")) {
            resetCharacter();
            return;
        }
        RevivalSession s = session;
        if (s != null) s.sendChat(text);
        handler.postDelayed(() -> {
            if (chatBar != null) chatBar.setVisibility(View.GONE);
        }, 1800);
    }

    private void addChatLine(String nick, String text) {
        if (chatLog == null) return;
        TextView line = Ui.label(this, "[" + nick + "] " + text, 14, Color.WHITE, false);
        line.setPadding(0, dp(2), 0, dp(2));
        line.setShadowLayer(dp(2), 0, 1, 0xAA000000);
        chatLog.addView(line);
        while (chatLog.getChildCount() > 7) chatLog.removeViewAt(0);
    }

    private void addSystemLine(String text) {
        if (chatLog == null) return;
        TextView line = Ui.label(this, text, 13, 0xFFCFE6FF, false);
        line.setPadding(0, dp(2), 0, dp(2));
        line.setShadowLayer(dp(2), 0, 1, 0xAA000000);
        chatLog.addView(line);
        while (chatLog.getChildCount() > 7) chatLog.removeViewAt(0);
    }

    // -------------------------------------------------------------- меню

    private void openMenu() {
        if (menu != null || root == null) return;
        try {
            menu = new MenuOverlay(this, theme);
            root.addView(menu, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Throwable t) {
            menu = null;
            CrashLog.reportUi(this, t);
        }
    }

    public void closeMenu() {
        if (menu != null && root != null) {
            root.removeView(menu);
        }
        menu = null;
    }

    public void openDesign() {
        if (design != null || root == null) return;
        closeMenu();
        try {
            design = new DesignOverlay(this, theme, new DesignOverlay.Listener() {
                @Override
                public void onYear(int year) {
                    Prefs.setYear(GameActivity.this, year);
                    theme = YearTheme.of(year);
                    closeDesign();
                    rebuildChrome();
                    addSystemLine("Стиль: " + year + " · " + theme.label);
                }

                @Override
                public void onClose() {
                    closeDesign();
                }
            });
            root.addView(design, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Throwable t) {
            design = null;
            CrashLog.reportUi(this, t);
        }
    }

    private void closeDesign() {
        if (design != null && root != null) {
            root.removeView(design);
        }
        design = null;
    }

    private void rebuildChrome() {
        closeMenu();
        closeDesign();
        if (gl != null) {
            try {
                gl.onPause();
            } catch (Throwable ignored) {
            }
            gl = null;
        }
        renderer = null;
        try {
            buildUi();
        } catch (Throwable t) {
            CrashLog.reportUi(this, t);
        }
    }

    public void openCatalog() {
        try {
            startActivity(new Intent(this, CatalogActivity.class));
        } catch (Throwable t) {
            Toast.makeText(this, "Не открыть каталог: " + t, Toast.LENGTH_SHORT).show();
        }
    }

    public void openPlacePicker() {
        closeMenu();
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, 41);
        } catch (Exception e) {
            Toast.makeText(this, "Нет файлового менеджера", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 41 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        loadPlace(data.getData());
    }

    private void loadPlaceFromIntent(Intent intent) {
        if (intent == null) return;
        String uriString = intent.getStringExtra(EXTRA_PLACE_URI);
        if (uriString != null) loadPlace(Uri.parse(uriString));
    }

    private void loadPlace(final Uri uri) {
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new Exception("Файл не открылся");
                List<PlacePart> parts = PlaceLoader.load(in);
                runOnUiThread(() -> {
                    if (destroyed) return;
                    world.parts.clear();
                    world.parts.addAll(parts);
                    Toast.makeText(this, "Карта загружена: " + parts.size() + " деталей",
                            Toast.LENGTH_LONG).show();
                    addSystemLine("Карта: " + parts.size() + " деталей");
                });
            } catch (org.xmlpull.v1.XmlPullParserException e) {
                runOnUiThread(() -> {
                    if (!destroyed) Toast.makeText(this,
                            "Это .rbxl (двоичный). Нужен .rbxlx", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!destroyed) Toast.makeText(this,
                            "Не удалось прочитать карту", Toast.LENGTH_LONG).show();
                });
            }
        }, "revival-place").start();
    }

    // -------------------------------------------------- действия из меню

    public RevivalSession sessionOrNull() {
        return session;
    }

    public String nick() {
        return Prefs.nick(this);
    }

    public void setNick(String nick) {
        Prefs.setNick(this, nick);
    }

    /** Создаём комнату в фоне: ServerSocket не должен работать в UI-потоке. */
    public void hostRoom() {
        if (destroyed) return;
        if (session != null && session.role != RevivalSession.SOLO) return;
        final String nick = Prefs.nick(this);
        final Avatar avatar = Prefs.avatar(this);
        addSystemLine("Создаём комнату…");
        new Thread(() -> {
            try {
                final RevivalSession s = RevivalSession.host(nick, avatar);
                runOnUiThread(() -> {
                    if (destroyed) {
                        s.close();
                        return;
                    }
                    RevivalSession old = session;
                    session = s;
                    s.listener = this;
                    if (old != null && old != s) old.close();
                    addSystemLine("Ты хост. Код: " + s.code);
                    if (menu != null) menu.onHostingStarted();
                    maybeBanner();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    if (!destroyed) onError("Не удалось создать комнату: " + e.getMessage());
                });
            }
        }, "revival-host").start();
    }

    /** Подключение тоже в фоне: иначе NetworkOnMainThreadException. */
    public void joinRoom(final String code) {
        if (destroyed || code == null || code.trim().isEmpty()) return;
        final String nick = Prefs.nick(this);
        final Avatar avatar = Prefs.avatar(this);
        addSystemLine("Подключаемся к " + code.trim() + "…");
        new Thread(() -> {
            try {
                final RevivalSession s = RevivalSession.join(code.trim(), nick, avatar);
                runOnUiThread(() -> {
                    if (destroyed) {
                        s.close();
                        return;
                    }
                    RevivalSession old = session;
                    session = s;
                    s.listener = this;
                    if (old != null && old != s) old.close();
                    addSystemLine("Ты в комнате. Код: " + s.code);
                    maybeBanner();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    if (!destroyed) onError("Не подключиться: " + e.getMessage());
                });
            }
        }, "revival-join").start();
    }

    public void leaveRoom() {
        RevivalSession old = session;
        if (old != null) old.close();
        session = RevivalSession.solo(Prefs.nick(this), Prefs.avatar(this));
        session.listener = this;
        addSystemLine("Ты вышел из комнаты.");
    }

    public void resetCharacter() {
        world.x = 0;
        world.y = 0;
        world.z = 0;
        world.camYaw = 0;
        addSystemLine("Персонаж сброшен.");
    }

    public void leaveGame() {
        finish();
    }

    /** Переключает 2D-запасной режим и пересобирает экран. */
    public void setSafeMode(boolean on) {
        Prefs.setSafeMode(this, on);
        closeMenu();
        closeDesign();
        if (gl != null) {
            try {
                gl.onPause();
            } catch (Throwable ignored) {
            }
            gl = null;
        }
        renderer = null;
        try {
            buildUi();
            addSystemLine(on ? "3D выключен: включён безопасный режим."
                    : "3D включён.");
        } catch (Throwable t) {
            CrashLog.reportUi(this, t);
            showEmergency(t);
        }
    }

    public void openDiagnostics() {
        try {
            Intent i = new Intent(this, CrashActivity.class);
            i.putExtra(CrashActivity.EXTRA_DIAG, true);
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, "Лог: " + t, Toast.LENGTH_LONG).show();
        }
    }

    public void applyFullscreen(boolean on) {
        if (on) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    // ------------------------------------------------------ аварийный экран

    /** После трёх падений подряд не запускаем игру, чтобы не было цикла вылетов. */
    private void showRecovery() {
        try {
            ScrollView scroll = new ScrollView(this);
            scroll.setBackgroundColor(0xFF12141A);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp(20), dp(18), dp(20), dp(18));

            TextView title = new TextView(this);
            title.setText("Revival не смог запуститься");
            title.setTextColor(Color.WHITE);
            title.setTextSize(22);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            col.addView(title);

            TextView hint = new TextView(this);
            hint.setText("Подряд было " + Diag.fails(this) + " неудачных запуска, поэтому игра "
                    + "остановлена, а не вылетает снова и снова.\n\n"
                    + "Причина записана в Downloads/Revival-previous-run.txt "
                    + "и Downloads/Revival-crash.txt. Пришли их мне — я исправлю.");
            hint.setTextColor(0xFFC3CEDA);
            hint.setTextSize(14);
            hint.setPadding(0, dp(10), 0, dp(16));
            col.addView(hint);

            col.addView(recoveryButton("Открыть лог", () -> openDiagnostics()));
            col.addView(recoveryButton("Продолжить в 2D", () -> {
                Diag.resetFails(this);
                Prefs.setSafeMode(this, true);
                recreate();
            }));
            col.addView(recoveryButton("Попробовать 3D снова", () -> {
                Diag.resetFails(this);
                Prefs.setSafeMode(this, false);
                recreate();
            }));
            col.addView(recoveryButton("Закрыть", this::finish));

            scroll.addView(col);
            setContentView(scroll);
        } catch (Throwable ignored) {
        }
    }

    private TextView recoveryButton(String text, Runnable action) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.round(0xFF2A303A, dp(6), 0xff3a424e, dp(1)));
        t.setOnClickListener(v -> {
            try {
                action.run();
            } catch (Throwable e) {
                Toast.makeText(this, "Ошибка: " + e, Toast.LENGTH_LONG).show();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        lp.bottomMargin = dp(8);
        t.setLayoutParams(lp);
        return t;
    }

    private void showEmergency(Throwable error) {
        try {
            String stack = Log.getStackTraceString(error);
            if (stack.length() > 9000) stack = stack.substring(0, 9000) + "\n…";

            ScrollView scroll = new ScrollView(this);
            scroll.setBackgroundColor(0xFF12141A);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp(18), dp(14), dp(18), dp(14));

            TextView title = new TextView(this);
            title.setText("Revival: ошибка запуска");
            title.setTextColor(Color.WHITE);
            title.setTextSize(22);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            col.addView(title);

            TextView hint = new TextView(this);
            hint.setText("Приложение не закрылось. Причина сохранена в Downloads/Revival-crash.txt.");
            hint.setTextColor(0xFF9AA6B6);
            hint.setTextSize(13);
            hint.setPadding(0, dp(6), 0, dp(10));
            col.addView(hint);

            TextView logView = new TextView(this);
            logView.setText(stack);
            logView.setTextColor(0xFFD6DCE4);
            logView.setTextSize(10.5f);
            logView.setTypeface(Typeface.MONOSPACE);
            logView.setTextIsSelectable(true);
            logView.setBackgroundColor(0xFF0B0D12);
            logView.setPadding(dp(10), dp(8), dp(10), dp(8));
            col.addView(logView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            rowLp.topMargin = dp(10);
            col.addView(row, rowLp);

            row.addView(emergencyButton("Отправить", () -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, "Revival error:\n" + stack);
                startActivity(Intent.createChooser(send, "Отправить лог"));
            }), emergencyLp());

            row.addView(emergencyButton("В Downloads", () -> {
                String where = CrashLog.writeDownload(this, "Revival-log.txt", stack);
                Toast.makeText(this, where == null ? "Не удалось" : "Сохранено: " + where,
                        Toast.LENGTH_LONG).show();
            }), emergencyLp());

            row.addView(emergencyButton("Безопасный режим", () -> {
                Prefs.setSafeMode(this, true);
                recreate();
            }), emergencyLp());

            row.addView(emergencyButton("Перезапустить", () -> recreate()), emergencyLp());

            scroll.addView(col);
            setContentView(scroll);
        } catch (Throwable ignored) {
            // если даже аварийный экран не собрался — пусть система покажет свой диалог
        }
    }

    private LinearLayout.LayoutParams emergencyLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        return lp;
    }

    private TextView emergencyButton(String text, Runnable action) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(14);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.round(0xFF2A303A, dp(4), 0, 0));
        t.setOnClickListener(v -> {
            try {
                action.run();
            } catch (Throwable e) {
                Toast.makeText(this, "Ошибка: " + e, Toast.LENGTH_LONG).show();
            }
        });
        return t;
    }

    // ------------------------------------------------------------ баннер лога

    private void maybeBanner() {
        if (root == null || destroyed) return;
        boolean crash = CrashLog.hasUnseen(this) || Diag.bootNotice(this);
        boolean safe = Prefs.safeMode(this);
        if (!crash && !safe) return;

        String message = crash ? "⚠ Была ошибка — открыть лог" : "Безопасный режим · 3D выключен";
        TextView b = Ui.label(this, message, 12, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        b.setBackground(Ui.round(crash ? 0xCC8E2F2F : 0xCC2A4A3A, dp(12),
                crash ? 0x66FF8080 : 0x6655CC99, dp(1)));
        b.setOnClickListener(v -> {
            if (crash) {
                CrashLog.markSeen(this);
                Diag.clearBootNotice(this);
                openDiagnostics();
            } else {
                openMenu();
            }
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = dp(12);
        root.addView(b, lp);
    }

    // ---------------------------------------------------------- жизненный цикл

    @Override
    protected void onResume() {
        super.onResume();
        try {
            running = true;
            Diag.setRunning(this, true);
            if (gl != null) gl.onResume();
            world.shirt = Prefs.shirt(this);
            world.pants = Prefs.pants(this);
            world.face = Prefs.face(this);
            world.hair = Prefs.hair(this);
            if (session != null && session.role == RevivalSession.SOLO) {
                session.avatar = Prefs.avatar(this);
            }
            handler.removeCallbacks(positionLoop);
            handler.postDelayed(positionLoop, 120);
        } catch (Throwable t) {
            CrashLog.reportUi(this, t);
        }
    }

    @Override
    protected void onPause() {
        running = false;
        handler.removeCallbacks(positionLoop);
        try {
            Diag.setRunning(this, false);
            if (gl != null) gl.onPause();
        } catch (Throwable ignored) {
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        try {
            Diag.setRunning(this, false);
            if (session != null) {
                session.listener = null;
                session.close();
            }
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (root == null) {
            finish();
        } else if (menu != null) {
            closeMenu();
        } else if (design != null) {
            closeDesign();
        } else {
            openMenu();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Throwable t) {
            CrashLog.reportUi(this, t);
            return true;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        try {
            return super.dispatchKeyEvent(event);
        } catch (Throwable t) {
            CrashLog.reportUi(this, t);
            return true;
        }
    }

    // ------------------------------------------------------------- сетевое

    @Override
    public void onRoster() {
        try {
            if (menu != null) menu.onRosterChanged();
        } catch (Throwable t) {
            CrashLog.reportUi(this, t);
        }
    }

    @Override
    public void onChat(String nick, String text) {
        try {
            addChatLine(nick, text);
        } catch (Throwable t) {
            CrashLog.reportUi(this, t);
        }
    }

    @Override
    public void onError(String message) {
        try {
            addSystemLine("! " + message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            CrashLog.reportUi(this, t);
        }
    }

    private void onRendererError(Throwable t) {
        if (destroyed) return;
        try {
            Diag.line(this, "renderer error: " + Log.getStackTraceString(t));
            CrashLog.save(this, Thread.currentThread(), t);
        } catch (Throwable ignored) {
        }
        if (!Prefs.safeMode(this)) {
            Toast.makeText(this, "3D остановлен, включён безопасный режим", Toast.LENGTH_LONG).show();
            setSafeMode(true);
        } else {
            addSystemLine("3D ошибка: " + t);
        }
    }

    @Override
    public void onLookChanged() {
    }
}
