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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wexresprise.revival.gl.WorldRenderer;
import com.wexresprise.revival.net.RemotePlayer;
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

    private LinearLayout chatLog;
    private FrameLayout chatBar;
    private EditText chatInput;
    private TextView rosterLabel;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;

    private final Runnable positionLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            if (session != null && session.role != RevivalSession.SOLO) {
                session.sendPosition(world.x, world.y, world.z, world.yaw);
            }
            handler.postDelayed(this, 120);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        theme = YearTheme.of(Prefs.year(this));
        session = RevivalSession.current;
        if (session == null) {
            session = RevivalSession.solo(Prefs.nick(this), Prefs.avatar(this));
        }
        session.listener = this;

        world.shirt = session.avatar.shirt;
        world.pants = session.avatar.pants;
        world.face = session.avatar.face;
        world.hair = session.avatar.hair;
        world.camYaw = 0f;
        if (session.role == RevivalSession.CLIENT) {
            world.x = 3.5f;
            world.z = 3.5f;
        }

        setContentView(buildUi());
        loadPlaceFromIntent(getIntent());
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);

        gl = new GLSurfaceViewBridge(this);
        renderer = new WorldRenderer(world);
        renderer.setSky(AvatarArt.blend(theme.bg, 0xFF7FA8D8, 0.22f));
        gl.setRenderer(renderer);
        root.addView(gl, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        HudView hud = new HudView(this, world, renderer);
        hud.setCallback(this);
        root.addView(hud, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // верхняя полоса
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));

        TextView tag = Ui.label(this, "REVIVAL", 13, 0xFFFFFFFF, true);
        tag.setShadowLayer(Ui.dp(this, 3), 0, 1, 0xAA000000);
        top.addView(tag);

        rosterLabel = Ui.label(this, "", 12, 0xCCFFFFFF, false);
        LinearLayout.LayoutParams rlp = Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rlp.leftMargin = Ui.dp(this, 12);
        rosterLabel.setLayoutParams(rlp);
        rosterLabel.setShadowLayer(Ui.dp(this, 3), 0, 1, 0xAA000000);
        rosterLabel.setText(infoLine());
        top.addView(rosterLabel);

        TextView exit = Ui.label(this, "✕", 18, 0xFFFFFFFF, true);
        exit.setGravity(Gravity.CENTER);
        exit.setBackground(Ui.round(0x66000000, Ui.dp(this, 18), 0x55FFFFFF, Ui.dp(this, 1)));
        exit.setPadding(Ui.dp(this, 12), Ui.dp(this, 2), Ui.dp(this, 12), Ui.dp(this, 4));
        exit.setOnClickListener(v -> finish());
        top.addView(exit);

        root.addView(top, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        // кнопки справа снизу
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);

        TextView chatBtn = pill("ЧАТ", 0xCC11131A, 0xFFFFFFFF);
        chatBtn.setOnClickListener(v -> toggleChat());
        right.addView(chatBtn);

        TextView jumpBtn = pill("↑", 0xCC11131A, 0xFFFFFFFF);
        jumpBtn.setOnClickListener(v -> world.jumpRequested = true);
        LinearLayout.LayoutParams jlp = Ui.llp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        jlp.topMargin = Ui.dp(this, 10);
        jumpBtn.setLayoutParams(jlp);
        right.addView(jumpBtn);

        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        rp.setMargins(0, 0, Ui.dp(this, 18), Ui.dp(this, 18));
        root.addView(right, rp);

        // лог чата
        chatLog = new LinearLayout(this);
        chatLog.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(
                Ui.dp(this, 300), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        llp.setMargins(Ui.dp(this, 12), Ui.dp(this, 48), 0, 0);
        root.addView(chatLog, llp);

        // полоса ввода
        chatBar = new FrameLayout(this);
        chatBar.setBackgroundColor(0xE6101218);
        chatBar.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        chatBar.setVisibility(View.GONE);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        chatInput = new EditText(this);
        chatInput.setHint("Сообщение");
        chatInput.setTextColor(Color.WHITE);
        chatInput.setHintTextColor(0xFF7A8798);
        chatInput.setTextSize(15);
        chatInput.setSingleLine(true);
        chatInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        chatInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        chatInput.setBackground(Ui.round(0xFF1B1F27, Ui.dp(this, 8), 0xFF2E3642, Ui.dp(this, 1)));
        chatInput.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            sendChat();
            return true;
        });
        bar.addView(chatInput, Ui.llp(0, Ui.dp(this, 44), 1f));

        TextView send = Ui.label(this, "➤", 18, Color.WHITE, true);
        send.setGravity(Gravity.CENTER);
        send.setBackground(Ui.round(theme.accent, Ui.dp(this, 8), 0, 0));
        send.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        send.setOnClickListener(v -> sendChat());
        LinearLayout.LayoutParams slp = Ui.llp(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 44));
        slp.leftMargin = Ui.dp(this, 8);
        send.setLayoutParams(slp);
        bar.addView(send);

        chatBar.addView(bar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL));

        root.addView(chatBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        return root;
    }

    private TextView pill(String text, int bg, int fg) {
        TextView t = Ui.label(this, text, 14, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 10));
        t.setBackground(Ui.round(bg, Ui.dp(this, 22), 0x55FFFFFF, Ui.dp(this, 1)));
        return t;
    }

    private String infoLine() {
        if (session.role == RevivalSession.SOLO) return "одиночная игра";
        int n = session.players.size() + 1;
        String where = session.role == RevivalSession.HOST ? "хост" : "игрок";
        return where + " · " + session.nick + " · " + n + " в мире";
    }

    private void toggleChat() {
        boolean show = chatBar.getVisibility() != View.VISIBLE;
        chatBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            chatInput.requestFocus();
            android.view.inputmethod.InputMethodManager im = (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (im != null) im.showSoftInput(chatInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        } else {
            android.view.inputmethod.InputMethodManager im = (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (im != null) im.hideSoftInputFromWindow(chatInput.getWindowToken(), 0);
        }
    }

    private void sendChat() {
        String text = chatInput.getText().toString().trim();
        if (text.isEmpty()) return;
        session.sendChat(text);
        chatInput.setText("");
    }

    private void addChatLine(String nick, String text) {
        if (chatLog == null) return;
        TextView line = Ui.label(this, nick + ": " + text, 12, 0xFFFFFFFF, false);
        line.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        line.setBackground(Ui.round(0x66000000, Ui.dp(this, 6), 0, 0));
        line.setShadowLayer(Ui.dp(this, 2), 0, 1, 0xAA000000);
        chatLog.addView(line);
        while (chatLog.getChildCount() > 5) {
            chatLog.removeViewAt(0);
        }
    }

    private void loadPlaceFromIntent(Intent intent) {
        if (intent == null) return;
        String uriString = intent.getStringExtra(EXTRA_PLACE_URI);
        if (uriString == null) return;
        final Uri uri = Uri.parse(uriString);
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new Exception("Файл не открылся");
                List<PlacePart> parts = PlaceLoader.load(in);
                world.parts.clear();
                world.parts.addAll(parts);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Место загружено: " + parts.size() + " деталей",
                            Toast.LENGTH_LONG).show();
                    if (rosterLabel != null) rosterLabel.setText(infoLine());
                });
            } catch (Exception e) {
                final String msg = (e instanceof org.xmlpull.v1.XmlPullParserException)
                        || (e instanceof java.io.IOException && !(e instanceof java.io.FileNotFoundException))
                        ? "Это .rbxl (двоичный). Нужен .rbxlx — XML-версия места"
                        : "Не удалось прочитать место: " + e.getClass().getSimpleName();
                runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
            }
        }, "revival-place").start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        if (gl != null) gl.onResume();
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
            if (session.role != RevivalSession.SOLO) session.close();
        }
        super.onDestroy();
    }

    // ------------------------------------------------------------- callback

    @Override
    public void onRoster() {
        if (rosterLabel != null) rosterLabel.setText(infoLine());
    }

    @Override
    public void onChat(String nick, String text) {
        addChatLine(nick, text);
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        addChatLine("система", message);
    }

    @Override
    public void onLookChanged() {
    }
}
