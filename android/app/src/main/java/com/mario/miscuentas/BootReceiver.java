package com.mario.miscuentas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Vuelve a poner la notificación fija al reiniciar el móvil. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (i == null || !Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) return;
        if (QuickStore.notifEnabled(c)) QuickStore.showNotification(c, null);
        QuickStore.scheduleWeekly(c);
    }
}
