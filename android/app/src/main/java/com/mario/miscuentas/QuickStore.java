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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Almacén compartido entre la app, la notificación y el widget. */
public class QuickStore {
    static final String PREFS = "quickadd";
    static final String CHANNEL = "quick_add";
    static final int NOTIF_ID = 4242;
    static final String KEY_TEXT = "quick_text";
    static final String ACTION_ADD = "com.mario.miscuentas.QUICK_ADD";

    static class Parsed {
        double amount = 0;
        String note = "";
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
            s = (s.substring(0, m.start()) + " " + s.substring(m.end()));
        }
        p.note = s.replace("€", " ").replaceAll("\\s+", " ").trim();
        return p;
    }

    static String money(double a) {
        return String.format(new Locale("es", "ES"), "%.2f €", a);
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
            p.edit().putString("pending", arr.toString()).apply();
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

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Anotar rápido", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Notificación fija para anotar gastos e ingresos");
            ch.setShowBadge(false);
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
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

    /** Muestra (o refresca) la notificación fija. extra = mensaje temporal tras anotar. */
    static void showNotification(Context c, String extra) {
        ensureChannel(c);
        String summary = prefs(c).getString("summary", "Pulsa «+ Gasto» o «+ Ingreso» y escribe, por ejemplo: 12,50 mercadona");
        String text = extra != null ? extra : summary;

        RemoteInput riExp = new RemoteInput.Builder(KEY_TEXT).setLabel("Gasto · ej: 12,50 mercadona").build();
        RemoteInput riInc = new RemoteInput.Builder(KEY_TEXT).setLabel("Ingreso · ej: 50 bizum Ana").build();
        NotificationCompat.Action aExp = new NotificationCompat.Action.Builder(R.drawable.ic_stat_money, "+ Gasto", replyIntent(c, "exp", 101))
                .addRemoteInput(riExp).setAllowGeneratedReplies(false).build();
        NotificationCompat.Action aInc = new NotificationCompat.Action.Builder(R.drawable.ic_stat_money, "+ Ingreso", replyIntent(c, "inc", 102))
                .addRemoteInput(riInc).setAllowGeneratedReplies(false).build();

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(c, 103, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_money)
                .setContentTitle("Mis Cuentas")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(openPi)
                .addAction(aExp)
                .addAction(aInc)
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

    static void updateWidgets(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, QuickAddWidget.class));
        if (ids != null && ids.length > 0) QuickAddWidget.updateAll(c, m, ids);
    }

    /** Guarda una anotación y refresca notificación y widget. Devuelve el texto de confirmación. */
    static String record(Context c, String type, String input) {
        Parsed p = parse(input);
        if (p.amount <= 0) return null;
        addPending(c, type, p.amount, p.note);
        String msg = ("inc".equals(type) ? "✓ Ingreso anotado: " : "✓ Gasto anotado: ") + money(p.amount)
                + (p.note.isEmpty() ? "" : " · " + p.note);
        updateWidgets(c);
        return msg;
    }
}
