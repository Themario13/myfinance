package com.mario.miscuentas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Cada mañana repone y refresca la notificación fija (actualiza el "al día" y los días que quedan) y se reprograma.
 *  No hace ruido ni avisa: solo repinta el resumen con la fecha nueva. */
public class DailyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (QuickStore.notifEnabled(c)) QuickStore.showNotification(c, null);
        QuickStore.checkAlert(c);
        QuickStore.scheduleDaily(c);
    }
}
