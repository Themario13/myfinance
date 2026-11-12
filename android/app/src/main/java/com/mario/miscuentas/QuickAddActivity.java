package com.mario.miscuentas;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Ventanita para anotar un gasto o ingreso (desde la notificación o el widget) eligiendo categoría. */
public class QuickAddActivity extends Activity {
    private String type = "exp";
    private String cat = null;
    private Button bExp, bInc;
    private TextView title;
    private EditText amount, note;
    private LinearLayout grid;
    private final List<String[]> cats = new ArrayList<>(); // id, nombre, icono, tipo

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable roundStroke(int color, int stroke, int radius) {
        GradientDrawable g = round(color, radius);
        g.setStroke(dp(2), stroke);
        return g;
    }

    private void loadCats() {
        String raw = QuickStore.prefs(this).getString("cats", "");
        try {
            JSONArray a = new JSONArray(raw == null || raw.isEmpty() ? "[]" : raw);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                cats.add(new String[] { o.optString("id"), o.optString("name"), o.optString("icon"), o.optString("type", "exp") });
            }
        } catch (Exception ignored) {
        }
        if (cats.isEmpty()) {
            cats.add(new String[] { "super", "Súper", "🛒", "exp" });
            cats.add(new String[] { "ocio", "Comer fuera y ocio", "🍽️", "exp" });
            cats.add(new String[] { "transporte", "Transporte", "🚗", "exp" });
            cats.add(new String[] { "otros", "Otros", "📦", "exp" });
            cats.add(new String[] { "devol", "Devoluciones de amigos", "🤝", "inc" });
            cats.add(new String[] { "otrosi", "Otros ingresos", "💰", "inc" });
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if ("inc".equals(getIntent().getStringExtra("type"))) type = "inc";
        loadCats();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(round(0xFF1D2026, 26));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(16));
        scroll.addView(root);

        title = new TextView(this);
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFFEEF1F4);
        root.addView(title);

        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setPadding(0, dp(12), 0, dp(4));
        bExp = segBtn("Gasto");
        bInc = segBtn("Ingreso");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        lp.setMargins(0, 0, dp(6), 0);
        seg.addView(bExp, lp);
        seg.addView(bInc, new LinearLayout.LayoutParams(0, dp(40), 1f));
        bExp.setOnClickListener(v -> setType("exp"));
        bInc.setOnClickListener(v -> setType("inc"));
        root.addView(seg);

        amount = new EditText(this);
        amount.setHint("0,00 €");
        amount.setTextSize(34);
        amount.setTypeface(Typeface.DEFAULT_BOLD);
        amount.setGravity(Gravity.CENTER);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setBackground(null);
        amount.setHintTextColor(0xFF3A3F48);
        root.addView(amount);

        note = new EditText(this);
        note.setHint("Concepto (opcional)");
        note.setSingleLine(true);
        note.setImeOptions(EditorInfo.IME_ACTION_DONE);
        note.setPadding(dp(14), dp(11), dp(14), dp(11));
        note.setBackground(round(0xFF22262D, 14));
        note.setTextColor(0xFFEEF1F4);
        note.setHintTextColor(0xFF5D6470);
        note.setTextSize(15);
        root.addView(note);

        TextView catLbl = new TextView(this);
        catLbl.setText("Categoría");
        catLbl.setTextColor(0xFF8B93A1);
        catLbl.setTextSize(12.5f);
        catLbl.setTypeface(Typeface.DEFAULT_BOLD);
        catLbl.setPadding(dp(2), dp(14), 0, dp(6));
        root.addView(catLbl);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        root.addView(grid);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0, dp(14), 0, 0);
        Button cancel = new Button(this);
        cancel.setText("Cancelar");
        cancel.setAllCaps(false);
        cancel.setTextColor(0xFFEEF1F4);
        cancel.setBackground(round(0xFF2A2F37, 14));
        cancel.setStateListAnimator(null);
        Button ok = new Button(this);
        ok.setText("Guardar");
        ok.setAllCaps(false);
        ok.setTypeface(Typeface.DEFAULT_BOLD);
        ok.setTextColor(0xFF0B1712);
        ok.setBackground(round(0xFF3EE6A8, 14));
        ok.setStateListAnimator(null);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, dp(48), 1f);
        l1.setMargins(0, 0, dp(8), 0);
        btns.addView(cancel, l1);
        btns.addView(ok, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(btns);

        TextView open = new TextView(this);
        open.setText("Abrir la app");
        open.setTextColor(0xFF3EE6A8);
        open.setGravity(Gravity.CENTER);
        open.setPadding(0, dp(12), 0, dp(2));
        open.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
        root.addView(open);

        cancel.setOnClickListener(v -> finish());
        ok.setOnClickListener(v -> save());
        note.setOnEditorActionListener((v, id, e) -> {
            save();
            return true;
        });

        setContentView(scroll);
        if (getWindow() != null) {
            getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(24), dp(440)), ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        setType(type);
        amount.requestFocus();
    }

    private Button segBtn(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setStateListAnimator(null);
        return b;
    }

    private void setType(String t) {
        type = t;
        cat = null;
        boolean e = "exp".equals(t);
        title.setText(e ? "Nuevo gasto" : "Nuevo ingreso");
        bExp.setBackground(round(e ? 0xFF323741 : 0xFF22262D, 12));
        bExp.setTextColor(e ? 0xFFEEF1F4 : 0xFF8B93A1);
        bInc.setBackground(round(!e ? 0xFF323741 : 0xFF22262D, 12));
        bInc.setTextColor(!e ? 0xFFEEF1F4 : 0xFF8B93A1);
        amount.setTextColor(e ? 0xFFFF6B6B : 0xFF3EE6A8);
        buildGrid();
    }

    /** Rejilla de categorías, 4 por fila. */
    private void buildGrid() {
        grid.removeAllViews();
        List<String[]> list = new ArrayList<>();
        for (String[] c : cats) if (type.equals(c[3])) list.add(c);
        LinearLayout row = null;
        for (int i = 0; i < list.size(); i++) {
            if (i % 4 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row);
            }
            final String[] c = list.get(i);
            boolean on = c[0].equals(cat);
            TextView cell = new TextView(this);
            cell.setText(c[2] + "\n" + c[1]);
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(10.5f);
            cell.setMaxLines(3);
            cell.setTextColor(on ? 0xFFEEF1F4 : 0xFF8B93A1);
            cell.setPadding(dp(3), dp(8), dp(3), dp(8));
            cell.setBackground(on ? roundStroke(0x1F3EE6A8, 0xFF3EE6A8, 13) : round(0xFF22262D, 13));
            cell.setOnClickListener(v -> {
                cat = c[0].equals(cat) ? null : c[0];
                buildGrid();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p.setMargins(dp(3), dp(3), dp(3), dp(3));
            row.addView(cell, p);
        }
        // relleno para que la última fila no se estire
        if (row != null) {
            int rest = (4 - list.size() % 4) % 4;
            for (int k = 0; k < rest; k++) {
                View sp = new View(this);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 1, 1f);
                p.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(sp, p);
            }
        }
    }

    private void save() {
        QuickStore.Parsed p = QuickStore.parse(amount.getText().toString());
        if (p.amount <= 0) {
            Toast.makeText(this, "Pon un importe", Toast.LENGTH_SHORT).show();
            return;
        }
        String concept = note.getText().toString().trim();
        String msg = QuickStore.recordFull(this, type, p.amount, concept, cat);
        if (QuickStore.notifEnabled(this)) QuickStore.showNotification(this, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }
}
