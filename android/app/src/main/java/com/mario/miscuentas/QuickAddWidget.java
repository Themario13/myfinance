package com.mario.miscuentas;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Widget de la pantalla de inicio: resumen + botones para anotar. */
public class QuickAddWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        updateAll(c, m, ids);
    }

    static PendingIntent addIntent(Context c, String type, int req) {
        Intent i = new Intent(c, QuickAddActivity.class);
        i.putExtra("type", type);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void updateAll(Context c, AppWidgetManager m, int[] ids) {
        String summary = QuickStore.prefs(c).getString("widget", "Abre la app para ver tu resumen");
        int pend = QuickStore.pendingCount(c);
        if (pend > 0) summary = summary + "\n" + pend + " anotación" + (pend > 1 ? "es" : "") + " pendiente" + (pend > 1 ? "s" : "") + " de clasificar";
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPi = PendingIntent.getActivity(c, 201, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_quick);
            v.setTextViewText(R.id.widget_summary, summary);
            v.setOnClickPendingIntent(R.id.widget_summary, openPi);
            v.setOnClickPendingIntent(R.id.widget_title, openPi);
            v.setOnClickPendingIntent(R.id.widget_exp, addIntent(c, "exp", 202));
            v.setOnClickPendingIntent(R.id.widget_inc, addIntent(c, "inc", 203));
            m.updateAppWidget(id, v);
        }
    }
}
