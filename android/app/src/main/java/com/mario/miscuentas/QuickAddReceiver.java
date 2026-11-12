package com.mario.miscuentas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;

/** Recibe lo que escribes en la notificación fija. */
public class QuickAddReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (i == null || !QuickStore.ACTION_ADD.equals(i.getAction())) return;
        String type = i.getStringExtra("type");
        if (type == null) type = "exp";
        Bundle r = RemoteInput.getResultsFromIntent(i);
        String msg = null;
        if (r != null) {
            CharSequence cs = r.getCharSequence(QuickStore.KEY_TEXT);
            String input = cs == null ? "" : cs.toString();
            msg = QuickStore.record(c, type, input);
            if (msg == null) msg = "No he entendido el importe. Prueba así: 12,50 mercadona";
            else msg = msg + "\nSe clasificará solo al abrir la app.";
        }
        QuickStore.showNotification(c, msg);
    }
}
