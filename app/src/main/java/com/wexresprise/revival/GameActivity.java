package com.wexresprise.revival;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
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
        theme = YearTheme.of(Prefs.year(this));
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
        loadPlaceFromIntent(getIntent());
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private void buildUi() {
        root = new FrameLayout(this);

        gl = new GLSurfaceViewBridge(this);
        renderer = new WorldRenderer(world);
        renderer.setSky(0xFF8FC9EE);
        gl.setRenderer(renderer);
        root.addView(gl, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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
    }

    private View iconButton(IconView.Kind kind, Runnable action) {
        FrameLayout b = new FrameLayout(this);
        b.setBackground(Ui.round(0xB33B424E, dp(14), 0x33FFFFFF, dp(1)));
        b.setClickable(true);
        IconView icon = new IconView(this);
        icon.setKind(kind);
        icon.setIconColor(Color.WHITE);
        b.addView(icon, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        b.setOnClickListener(v -> action.run());
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
        hint.setOnClickListener(v -> focusChat());
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
        send.setOnClickListener(v -> sendChat());
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
        chatBar.setVisibility(View.VISIBLE);
        chatInput.requestFocus();
        InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (im != null) im.showSoftInput(chatInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void sendChat() {
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
        session.sendChat(text);
        handler.postDelayed(() -> chatBar.setVisibility(View.GONE), 1800);
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
        if (menu != null) return;
        menu = new MenuOverlay(this, theme);
        root.addView(menu, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void closeMenu() {
        if (menu != null) {
            root.removeView(menu);
            menu = null;
        }
    }

    private void openDesign() {
        if (design != null) return;
        closeMenu();
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
    }

    private void closeDesign() {
        if (design != null) {
            root.removeView(design);
            design = null;
        }
    }

    private void rebuildChrome() {
        closeMenu();
        closeDesign();
        buildUi();
    }

    public void openCatalog() {
        startActivity(new Intent(this, CatalogActivity.class));
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
                world.parts.clear();
                world.parts.addAll(parts);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Карта загружена: " + parts.size() + " деталей",
                            Toast.LENGTH_LONG).show();
                    addSystemLine("Карта: " + parts.size() + " деталей");
                });
            } catch (org.xmlpull.v1.XmlPullParserException e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Это .rbxl (двоичный). Нужен .rbxlx", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Не удалось прочитать карту", Toast.LENGTH_LONG).show());
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

    public void hostRoom() throws Exception {
        if (session != null && session.role != RevivalSession.SOLO) return;
        if (session != null) session.close();
        session = RevivalSession.host(Prefs.nick(this), Prefs.avatar(this));
        session.listener = this;
        addSystemLine("Ты хост. Код: " + session.code);
    }

    public void joinRoom(String code) throws Exception {
        if (session != null && session.role != RevivalSession.SOLO) session.close();
        session = RevivalSession.join(code, Prefs.nick(this), Prefs.avatar(this));
        session.listener = this;
        addSystemLine("Подключаемся к " + code + "…");
    }

    public void leaveRoom() {
        if (session != null) session.close();
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

    // ---------------------------------------------------------- жизненный цикл

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
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
    }

    @Override
    protected void onPause() {
        running = false;
        handler.removeCallbacks(positionLoop);
        if (gl != null) gl.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.listener = null;
            session.close();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (menu != null) {
            closeMenu();
        } else if (design != null) {
            closeDesign();
        } else {
            openMenu();
        }
    }

    // ------------------------------------------------------------- сетевое

    @Override
    public void onRoster() {
        if (menu != null) menu.onRosterChanged();
    }

    @Override
    public void onChat(String nick, String text) {
        addChatLine(nick, text);
    }

    @Override
    public void onError(String message) {
        addSystemLine("! " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLookChanged() {
    }
}
