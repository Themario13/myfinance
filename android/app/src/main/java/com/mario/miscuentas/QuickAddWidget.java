package com.mario.miscuentas;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

/** Widget de la pantalla de inicio: tu resumen del mes + botón para anotar. */
public class QuickAddWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        updateAll(c, m, ids);
    }

    static PendingIntent addIntent(Context c) {
        Intent i = new Intent(c, QuickAddActivity.class);
        i.putExtra("type", "exp");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(c, 202, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void updateAll(Context c, AppWidgetManager m, int[] ids) {
        QuickStore.Summary s = QuickStore.summary(c);
        String sub = s.sub;
        int pend = QuickStore.pendingCount(c);
        if (pend > 0) sub = pend + " anotación" + (pend > 1 ? "es" : "") + " sin clasificar · " + sub;
        PendingIntent openPi = QuickStore.openApp(c, 201);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_quick);
            v.setTextViewText(R.id.w_main, s.main);
            v.setTextColor(R.id.w_main, s.bad ? 0xFFFF6B6B : 0xFFEEF1F4);
            v.setTextViewText(R.id.w_sub, sub);
            v.setProgressBar(R.id.w_bar_ok, 100, s.progress, false);
            v.setProgressBar(R.id.w_bar_warn, 100, s.progress, false);
            v.setProgressBar(R.id.w_bar_bad, 100, s.progress, false);
            v.setViewVisibility(R.id.w_bar_ok, !s.bad && !s.warn ? View.VISIBLE : View.GONE);
            v.setViewVisibility(R.id.w_bar_warn, s.warn ? View.VISIBLE : View.GONE);
            v.setViewVisibility(R.id.w_bar_bad, s.bad ? View.VISIBLE : View.GONE);
            v.setOnClickPendingIntent(R.id.w_root, openPi);
            v.setOnClickPendingIntent(R.id.w_add, QuickStore.quickIntent(c, "exp", 202));
            v.setOnClickPendingIntent(R.id.w_inc, QuickStore.quickIntent(c, "inc", 203));
            m.updateAppWidget(id, v);
        }
    }
}
