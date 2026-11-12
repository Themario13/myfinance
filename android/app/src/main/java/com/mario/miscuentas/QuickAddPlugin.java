package com.mario.miscuentas;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "QuickAdd")
public class QuickAddPlugin extends Plugin {

    private boolean hasNotifPermission() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @PluginMethod
    public void setNotification(PluginCall call) {
        Context c = getContext();
        boolean on = Boolean.TRUE.equals(call.getBoolean("enabled", false));
        QuickStore.prefs(c).edit().putBoolean("notif", on).apply();
        if (on) {
            if (!hasNotifPermission() && getActivity() != null) {
                ActivityCompat.requestPermissions(getActivity(), new String[] { Manifest.permission.POST_NOTIFICATIONS }, 7001);
            }
            QuickStore.showNotification(c, null);
        } else {
            QuickStore.hideNotification(c);
        }
        JSObject r = new JSObject();
        r.put("enabled", on);
        r.put("permission", hasNotifPermission());
        call.resolve(r);
    }

    @PluginMethod
    public void updateSummary(PluginCall call) {
        Context c = getContext();
        String text = call.getString("text", "");
        String widget = call.getString("widget", text);
        QuickStore.prefs(c).edit().putString("summary", text).putString("widget", widget).apply();
        if (QuickStore.notifEnabled(c)) QuickStore.showNotification(c, null);
        QuickStore.updateWidgets(c);
        call.resolve();
    }

    @PluginMethod
    public void takePending(PluginCall call) {
        JSObject r = new JSObject();
        r.put("items", QuickStore.takePending(getContext()));
        call.resolve(r);
        QuickStore.updateWidgets(getContext());
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        JSObject r = new JSObject();
        r.put("enabled", QuickStore.notifEnabled(getContext()));
        r.put("permission", hasNotifPermission());
        call.resolve(r);
    }
}
