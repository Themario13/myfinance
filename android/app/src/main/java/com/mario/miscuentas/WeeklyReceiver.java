package com.mario.miscuentas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Salta el domingo por la noche con el resumen de la semana y se reprograma para el siguiente. */
public class WeeklyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        QuickStore.showWeekly(c);
        QuickStore.scheduleWeekly(c);
    }
}
