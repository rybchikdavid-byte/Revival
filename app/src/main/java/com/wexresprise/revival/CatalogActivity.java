package com.wexresprise.revival;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class CatalogActivity extends Activity {

    private YearTheme theme;
    private FrameLayout root;
    private GridView grid;
    private LinearLayout tabs;
    private int kind = Catalog.KIND_SHIRT;

    private Bitmap custom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            theme = YearTheme.of(Prefs.year(this));
            custom = loadCustom();
            render();
        } catch (Throwable t) {
            try {
                CrashLog.save(this, Thread.currentThread(), t);
            } catch (Throwable ignored) {
            }
            showError(t);
        }
    }

    private void showError(Throwable error) {
        try {
            android.widget.ScrollView scroll = new android.widget.ScrollView(this);
            scroll.setBackgroundColor(0xFF12141A);
            TextView text = new TextView(this);
            text.setText("Каталог не открылся.\n\n" + android.util.Log.getStackTraceString(error)
                    + "\n\nЛог: Downloads/Revival-crash.txt");
            text.setTextColor(0xFFD6DCE4);
            text.setTextSize(12f);
            text.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
            scroll.addView(text);
            scroll.setOnClickListener(v -> finish());
            setContentView(scroll);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Throwable t) {
            CrashLog.reportUi(this, t);
            return true;
        }
    }

    private void render() {
        root = new FrameLayout(this);
        root.setBackgroundColor(theme.bg);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        // шапка
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 10));

        TextView back = Ui.label(this, "‹", 26, theme.text, true);
        back.setGravity(Gravity.CENTER);
        back.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 4));
        back.setOnClickListener(v -> finish());
        header.addView(back);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(Ui.label(this, "Каталог", 20, theme.text, true));
        titles.addView(Ui.label(this, "надевается сразу, без аккаунта", 11, theme.sub, false));
        header.addView(titles, Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView own = Ui.button(this, theme, "Своя картинка", false);
        own.setTextSize(12);
        own.setPadding(Ui.dp(this, 12), Ui.dp(this, 9), Ui.dp(this, 12), Ui.dp(this, 9));
        own.setOnClickListener(v -> pickImage());
        header.addView(own);

        col.addView(header);

        tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), Ui.dp(this, 8));
        for (int i = 0; i < Catalog.TITLES.length; i++) {
            final int k = i;
            TextView t = Ui.label(this, Catalog.TITLES[i], 13,
                    i == kind ? Ui.pickText(theme.accent) : theme.sub, i == kind);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
            t.setBackground(Ui.round(i == kind ? theme.accent : AvatarArt.blend(theme.panel, theme.bg, 0.25f),
                    Ui.dp(this, theme.radius + 8), 0, 0));
            LinearLayout.LayoutParams lp = Ui.llp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.leftMargin = Ui.dp(this, 3);
            lp.rightMargin = Ui.dp(this, 3);
            t.setOnClickListener(v -> {
                kind = k;
                render();
            });
            tabs.addView(t, lp);
        }
        col.addView(tabs);

        grid = new GridView(this);
        grid.setNumColumns(3);
        grid.setHorizontalSpacing(Ui.dp(this, 8));
        grid.setVerticalSpacing(Ui.dp(this, 8));
        grid.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 24));
        grid.setClipToPadding(false);
        grid.setVerticalScrollBarEnabled(false);
        grid.setAdapter(new Adapter());
        grid.setOnItemClickListener((parent, view, position, id) -> {
            if (kind == Catalog.KIND_SHIRT && position == Catalog.count(kind)) {
                pickImage();
                return;
            }
            equip(position);
        });
        col.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private void equip(int index) {
        switch (kind) {
            case Catalog.KIND_SHIRT:
                Prefs.setShirt(this, index);
                Prefs.setCustomShirt(this, null);
                custom = null;
                break;
            case Catalog.KIND_PANTS:
                Prefs.setPants(this, index);
                break;
            case Catalog.KIND_FACE:
                Prefs.setFace(this, index);
                break;
            default:
                Prefs.setHair(this, index);
                break;
        }
        Toast.makeText(this, Catalog.name(kind, index) + " — надето", Toast.LENGTH_SHORT).show();
    }

    private void pickImage() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(i, 21);
        } catch (Exception e) {
            Toast.makeText(this, "Нет файлового менеджера", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 21 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap src = BitmapFactory.decodeStream(in);
            if (src == null) throw new Exception("bad bitmap");
            int max = 1024;
            int w = src.getWidth();
            int h = src.getHeight();
            if (w > max || h > max) {
                float k = Math.min((float) max / w, (float) max / h);
                Bitmap scaled = Bitmap.createScaledBitmap(src, Math.round(w * k), Math.round(h * k), true);
                if (scaled != src) src.recycle();
                src = scaled;
            }
            File dir = new File(getFilesDir(), "skins");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, "shirt.png");
            try (OutputStream os = new FileOutputStream(out)) {
                src.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            Prefs.setCustomShirt(this, out.getAbsolutePath());
            custom = src;
            Toast.makeText(this, "Своя футболка надета", Toast.LENGTH_SHORT).show();
            render();
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось взять картинку", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap loadCustom() {
        String p = Prefs.customShirt(this);
        if (p == null) return null;
        return BitmapFactory.decodeFile(p);
    }

    private class Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            return Catalog.count(kind) + (kind == Catalog.KIND_SHIRT ? 1 : 0);
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell = new LinearLayout(CatalogActivity.this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(Ui.dp(CatalogActivity.this, 8), Ui.dp(CatalogActivity.this, 10),
                    Ui.dp(CatalogActivity.this, 8), Ui.dp(CatalogActivity.this, 10));

            boolean isCustom = kind == Catalog.KIND_SHIRT && position == Catalog.count(kind);
            boolean selected = !isCustom && isSelected(position);
            cell.setBackground(Ui.round(AvatarArt.blend(theme.panel, theme.bg, 0.15f),
                    Ui.dp(CatalogActivity.this, theme.radius + 10),
                    selected ? theme.accent : AvatarArt.blend(theme.panel, theme.bg, 0.55f),
                    Ui.dp(CatalogActivity.this, selected ? 2 : 1)));

            if (isCustom) {
                if (custom == null) {
                    TextView plus = Ui.label(CatalogActivity.this, "+", 40, theme.accent, true);
                    plus.setGravity(Gravity.CENTER);
                    plus.setLayoutParams(Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(CatalogActivity.this, 96)));
                    cell.addView(plus);
                } else {
                    android.widget.ImageView iv = new android.widget.ImageView(CatalogActivity.this);
                    iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    iv.setImageBitmap(custom);
                    iv.setBackground(Ui.round(theme.panel, Ui.dp(CatalogActivity.this, 8), 0, 0));
                    cell.addView(iv, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(CatalogActivity.this, 96)));
                }
                TextView name = Ui.label(CatalogActivity.this,
                        custom == null ? "Своя" : "Своя ✓", 11, theme.text, true);
                name.setGravity(Gravity.CENTER);
                name.setPadding(0, Ui.dp(CatalogActivity.this, 8), 0, 0);
                cell.addView(name);
                return cell;
            }

            PreviewView pv = new PreviewView(CatalogActivity.this);
            pv.set(kind, position, Prefs.avatar(CatalogActivity.this));
            cell.addView(pv, Ui.llp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(CatalogActivity.this, 96)));

            TextView name = Ui.label(CatalogActivity.this, Catalog.name(kind, position), 11, theme.text, false);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, Ui.dp(CatalogActivity.this, 8), 0, 0);
            cell.addView(name);
            return cell;
        }
    }

    private boolean isSelected(int index) {
        switch (kind) {
            case Catalog.KIND_SHIRT:
                return Prefs.customShirt(this) == null && Prefs.shirt(this) == index;
            case Catalog.KIND_PANTS:
                return Prefs.pants(this) == index;
            case Catalog.KIND_FACE:
                return Prefs.face(this) == index;
            default:
                return Prefs.hair(this) == index;
        }
    }
}
