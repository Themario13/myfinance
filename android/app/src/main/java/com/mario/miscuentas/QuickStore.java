package com.mario.miscuentas;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Almacén compartido entre la app, la notificación fija, los avisos y el widget. */
public class QuickStore {
    static final String PREFS = "quickadd";
    static final String CHANNEL = "quick_add_v3";
    static final String CHANNEL_ALERT = "alerts_v1";
    static final int NOTIF_ID = 4242;
    static final int ALERT_ID = 4243;
    static final String KEY_TEXT = "quick_text";
    static final String ACTION_ADD = "com.mario.miscuentas.QUICK_ADD";
    static final int MINT = 0xFF3EE6A8;
    static final Locale ES = new Locale("es", "ES");

    static class Parsed {
        double amount = 0;
        String note = "";
    }

    /** Resumen del mes listo para pintar. */
    static class Summary {
        String main;
        String sub;
        int progress;
        boolean bad;
        boolean warn;
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** "12,50 mercadona" -> 12.5 + "mercadona" */
    static Parsed parse(String input) {
        Parsed p = new Parsed();
        if (input == null) return p;
        String s = input.trim();
        Matcher m = Pattern.compile("(\\d+(?:[.,]\\d{1,2})?)").matcher(s);
        if (m.find()) {
            try {
                p.amount = Double.parseDouble(m.group(1).replace(',', '.'));
            } catch (NumberFormatException e) {
                p.amount = 0;
            }
            s = s.substring(0, m.start()) + " " + s.substring(m.end());
        }
        p.note = s.replace("€", " ").replaceAll("\\s+", " ").trim();
        return p;
    }

    static String money(double a) {
        return String.format(ES, "%.2f €", a);
    }

    static String money0(double a) {
        if (Math.abs(a) >= 100) return String.format(ES, "%,.0f €", a);
        return String.format(ES, "%.2f €", a);
    }

    static String curYM() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    }

    static int daysLeft() {
        Calendar cal = Calendar.getInstance();
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH) + 1;
    }

    static double getD(SharedPreferences p, String k) {
        try {
            return Double.parseDouble(p.getString(k, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static Summary summary(Context c) {
        SharedPreferences p = prefs(c);
        Summary s = new Summary();
        boolean hasPlan = p.getBoolean("hasPlan", false);
        String ym = p.getString("ym", "");
        if (!hasPlan || !curYM().equals(ym)) {
            s.main = "Mis Cuentas";
            String t = p.getString("summary", "");
            s.sub = (t == null || t.isEmpty() || !curYM().equals(ym)) ? "Pulsa «+ Gasto» y escribe: 12,50 mercadona" : t;
            s.progress = 0;
            return s;
        }
        double can = getD(p, "canSpend");
        double spendable = getD(p, "spendable");
        double goal = getD(p, "goal");
        String goalName = p.getString("goalName", "Ahorro");
        int dl = Math.max(1, daysLeft());
        if (can >= 0) {
            s.main = "Puedes gastar " + money0(can);
            s.sub = "≈ " + money0(can / dl) + " al día · " + dl + (dl == 1 ? " día" : " días") + (goal > 0 ? " · ahorro a salvo" : "");
        } else {
            s.bad = true;
            s.main = "Te pasas " + money0(-can);
            s.sub = goal > 0 ? "Te estás comiendo el " + goalName.toLowerCase(ES) + ": quedan " + money0(Math.max(0, goal + can)) + " de " + money0(goal)
                    : "Has gastado más de lo que entra este mes";
        }
        if (spendable <= 0) s.progress = 100;
        else s.progress = (int) Math.max(0, Math.min(100, Math.round((spendable - can) / spendable * 100)));
        s.warn = !s.bad && s.progress >= 85;
        return s;
    }

    static synchronized void addPending(Context c, String type, double amount, String note) {
        SharedPreferences p = prefs(c);
        try {
            JSONArray arr = new JSONArray(p.getString("pending", "[]"));
            JSONObject o = new JSONObject();
            o.put("type", type);
            o.put("amount", amount);
            o.put("note", note == null ? "" : note);
            o.put("ts", System.currentTimeMillis());
            arr.put(o);
            SharedPreferences.Editor e = p.edit().putString("pending", arr.toString());
            // ajustamos el resumen al momento, sin esperar a abrir la app
            if (p.getBoolean("hasPlan", false) && curYM().equals(p.getString("ym", ""))) {
                double can = getD(p, "canSpend");
                can += "inc".equals(type) ? amount : -amount;
                e.putString("canSpend", String.valueOf(can));
            }
            e.apply();
        } catch (JSONException ignored) {
        }
    }

    static synchronized String takePending(Context c) {
        SharedPreferences p = prefs(c);
        String s = p.getString("pending", "[]");
        p.edit().putString("pending", "[]").apply();
        return s;
    }

    static int pendingCount(Context c) {
        try {
            return new JSONArray(prefs(c).getString("pending", "[]")).length();
        } catch (JSONException e) {
            return 0;
        }
    }

    static boolean notifEnabled(Context c) {
        return prefs(c).getBoolean("notif", false);
    }

    static void ensureChannels(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm == null) return;
            for (String old : new String[] { "quick_add", "quick_add_v2" }) nm.deleteNotificationChannel(old);
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Resumen y anotar rápido", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Notificación fija con tu resumen del mes y el botón para anotar gastos");
            ch.setShowBadge(false);
            ch.setSound(null, null);
            ch.enableVibration(false);
            nm.createNotificationChannel(ch);
            NotificationChannel al = new NotificationChannel(CHANNEL_ALERT, "Avisos de ahorro", NotificationManager.IMPORTANCE_HIGH);
            al.setDescription("Te avisa cuando empiezas a comerte el ahorro del mes");
            nm.createNotificationChannel(al);
        }
    }

    private static PendingIntent replyIntent(Context c, String type, int req) {
        Intent i = new Intent(c, QuickAddReceiver.class);
        i.setAction(ACTION_ADD);
        i.putExtra("type", type);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
        return PendingIntent.getBroadcast(c, req, i, flags);
    }

    static PendingIntent openApp(Context c, int req) {
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, req, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static RemoteViews notifView(Context c, Summary s, String extra, boolean big) {
        RemoteViews v = new RemoteViews(c.getPackageName(), big ? R.layout.notif_big : R.layout.notif_small);
        v.setTextViewText(R.id.n_main, s.main);
        v.setTextViewText(R.id.n_sub, extra != null ? extra : s.sub);
        if (big) {
            v.setTextColor(R.id.n_main, s.bad ? 0xFFFF6B6B : 0xFFEEF1F4);
            v.setTextColor(R.id.n_sub, extra != null ? MINT : 0xFF8B93A1);
        } else if (s.bad) {
            v.setTextColor(R.id.n_main, 0xFFFF6B6B);
        }
        v.setProgressBar(R.id.n_bar_ok, 100, s.progress, false);
        v.setProgressBar(R.id.n_bar_warn, 100, s.progress, false);
        v.setProgressBar(R.id.n_bar_bad, 100, s.progress, false);
        v.setViewVisibility(R.id.n_bar_ok, !s.bad && !s.warn ? android.view.View.VISIBLE : android.view.View.GONE);
        v.setViewVisibility(R.id.n_bar_warn, s.warn ? android.view.View.VISIBLE : android.view.View.GONE);
        v.setViewVisibility(R.id.n_bar_bad, s.bad ? android.view.View.VISIBLE : android.view.View.GONE);
        if (big) v.setTextViewText(R.id.n_pct, s.progress + "% gastado");
        return v;
    }

    /** Muestra (o refresca) la notificación fija. extra = confirmación temporal tras anotar. */
    static void showNotification(Context c, String extra) {
        ensureChannels(c);
        Summary s = summary(c);

        RemoteInput riExp = new RemoteInput.Builder(KEY_TEXT).setLabel("Importe y concepto · ej: 12,50 mercadona").build();
        NotificationCompat.Action aExp = new NotificationCompat.Action.Builder(R.drawable.ic_stat_money, "+ Añadir gasto", replyIntent(c, "exp", 101))
                .addRemoteInput(riExp).setAllowGeneratedReplies(false).build();

        Notification n = new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_money)
                .setColor(MINT)
                .setContentTitle(s.main)
                .setContentText(extra != null ? extra : s.sub)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notifView(c, s, extra, false))
                .setCustomBigContentView(notifView(c, s, extra, true))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(openApp(c, 103))
                .addAction(aExp)
                .build();
        try {
            NotificationManagerCompat.from(c).notify(NOTIF_ID, n);
        } catch (SecurityException ignored) {
            // Sin permiso de notificaciones todavía
        }
    }

    static void hideNotification(Context c) {
        NotificationManagerCompat.from(c).cancel(NOTIF_ID);
    }

    /** Aviso fuerte cuando empiezas a comerte el ahorro (una vez por mes, se rearma si vuelves a ir bien). */
    static void checkAlert(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("hasPlan", false)) return;
        String ym = p.getString("ym", "");
        if (!curYM().equals(ym)) return;
        double can = getD(p, "canSpend");
        double goal = getD(p, "goal");
        String alerted = p.getString("alertYm", "");
        if (can >= 0) {
            if (ym.equals(alerted)) p.edit().putString("alertYm", "").apply();
            return;
        }
        if (ym.equals(alerted)) return;
        p.edit().putString("alertYm", ym).apply();
        ensureChannels(c);
        String goalName = p.getString("goalName", "ahorro").toLowerCase(ES);
        String text = "Llevas " + money0(-can) + " por encima de lo que puedes gastar este mes. "
                + (goal > 0 ? "Tu " + goalName + " baja a " + money0(Math.max(0, goal + can)) + " de " + money0(goal) + "." : "");
        Notification n = new NotificationCompat.Builder(c, CHANNEL_ALERT)
                .setSmallIcon(R.drawable.ic_stat_money)
                .setColor(0xFFFF6B6B)
                .setContentTitle("⚠️ Te estás comiendo el ahorro")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openApp(c, 104))
                .build();
        try {
            NotificationManagerCompat.from(c).notify(ALERT_ID, n);
        } catch (SecurityException ignored) {
        }
    }

    static void updateWidgets(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, QuickAddWidget.class));
        if (ids != null && ids.length > 0) QuickAddWidget.updateAll(c, m, ids);
    }

    /** Guarda una anotación, refresca widget y avisos. Devuelve el texto de confirmación o null si no hay importe. */
    static String record(Context c, String type, String input) {
        Parsed p = parse(input);
        if (p.amount <= 0) return null;
        addPending(c, type, p.amount, p.note);
        updateWidgets(c);
        checkAlert(c);
        return ("inc".equals(type) ? "✓ Ingreso anotado: " : "✓ Anotado: ") + money(p.amount) + (p.note.isEmpty() ? "" : " · " + p.note);
    }
}
