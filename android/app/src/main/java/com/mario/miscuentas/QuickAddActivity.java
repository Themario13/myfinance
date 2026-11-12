package com.mario.miscuentas;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

/** Ventanita para anotar desde el widget sin abrir la app. */
public class QuickAddActivity extends Activity {
    private String type = "exp";
    private Button bExp, bInc;
    private TextView title;
    private EditText amount;

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String t = getIntent().getStringExtra("type");
        if ("inc".equals(t)) type = "inc";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(18));
        root.setBackground(round(0xFF1D2026, 26));

        title = new TextView(this);
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFFEEF1F4);
        root.addView(title);

        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setPadding(0, dp(12), 0, dp(6));
        bExp = segBtn("Gasto");
        bInc = segBtn("Ingreso");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp.setMargins(0, 0, dp(6), 0);
        seg.addView(bExp, lp);
        seg.addView(bInc, new LinearLayout.LayoutParams(0, dp(42), 1f));
        bExp.setOnClickListener(v -> setType("exp"));
        bInc.setOnClickListener(v -> setType("inc"));
        seg.setVisibility(View.GONE);
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

        final EditText note = new EditText(this);
        note.setHint("Concepto (ej: Mercadona)");
        note.setSingleLine(true);
        note.setImeOptions(EditorInfo.IME_ACTION_DONE);
        note.setPadding(dp(14), dp(12), dp(14), dp(12));
        note.setBackground(round(0xFF22262D, 14));
        note.setTextColor(0xFFEEF1F4);
        note.setHintTextColor(0xFF5D6470);
        root.addView(note);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0, dp(16), 0, 0);
        Button cancel = new Button(this);
        cancel.setText("Cancelar");
        cancel.setAllCaps(false);
        cancel.setTextColor(0xFFEEF1F4);
        cancel.setBackground(round(0xFF2A2F37, 14));
        Button ok = new Button(this);
        ok.setText("Guardar");
        ok.setAllCaps(false);
        ok.setTypeface(Typeface.DEFAULT_BOLD);
        ok.setTextColor(0xFF0B1712);
        ok.setBackground(round(0xFF3EE6A8, 14));
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, dp(48), 1f);
        l1.setMargins(0, 0, dp(8), 0);
        btns.addView(cancel, l1);
        btns.addView(ok, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(btns);

        TextView open = new TextView(this);
        open.setText("Abrir la app");
        open.setTextColor(0xFF3EE6A8);
        open.setGravity(Gravity.CENTER);
        open.setPadding(0, dp(14), 0, 0);
        open.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
        root.addView(open);

        cancel.setOnClickListener(v -> finish());
        ok.setOnClickListener(v -> save(note.getText().toString()));
        note.setOnEditorActionListener((v, id, e) -> {
            save(note.getText().toString());
            return true;
        });

        setContentView(root);
        if (getWindow() != null) {
            getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(420)), ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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
        boolean e = "exp".equals(t);
        title.setText(e ? "Nuevo gasto" : "Nuevo ingreso");
        bExp.setBackground(round(e ? 0xFF323741 : 0xFF22262D, 12));
        bExp.setTextColor(e ? 0xFFEEF1F4 : 0xFF8B93A1);
        bInc.setBackground(round(!e ? 0xFF323741 : 0xFF22262D, 12));
        bInc.setTextColor(!e ? 0xFFEEF1F4 : 0xFF8B93A1);
        amount.setTextColor(e ? 0xFFFF6B6B : 0xFF3EE6A8);
    }

    private void save(String note) {
        String msg = QuickStore.record(this, type, amount.getText().toString() + " " + note);
        if (msg == null) {
            Toast.makeText(this, "Pon un importe", Toast.LENGTH_SHORT).show();
            return;
        }
        if (QuickStore.notifEnabled(this)) QuickStore.showNotification(this, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }
}
