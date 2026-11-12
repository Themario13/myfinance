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
        Double can = call.getDouble("canSpend", 0.0);
        Double spendable = call.getDouble("spendable", 0.0);
        Double goal = call.getDouble("goal", 0.0);
        QuickStore.prefs(c).edit()
                .putString("summary", call.getString("text", ""))
                .putBoolean("hasPlan", Boolean.TRUE.equals(call.getBoolean("hasPlan", false)))
                .putString("ym", call.getString("ym", ""))
                .putString("canSpend", String.valueOf(can == null ? 0 : can))
                .putString("spendable", String.valueOf(spendable == null ? 0 : spendable))
                .putString("goal", String.valueOf(goal == null ? 0 : goal))
                .putString("goalName", call.getString("goalName", "Ahorro"))
                .putString("weekStart", call.getString("weekStart", ""))
                .putString("weekSpent", String.valueOf(call.getDouble("weekSpent", 0.0)))
                .putString("weekCats", call.getString("weekCats", ""))
                .putString("cats", call.getString("cats", ""))
                .apply();
        QuickStore.scheduleWeekly(c);
        if (QuickStore.notifEnabled(c)) QuickStore.showNotification(c, null);
        QuickStore.updateWidgets(c);
        QuickStore.checkAlert(c);
        call.resolve();
    }

    @PluginMethod
    public void setWeekly(PluginCall call) {
        boolean on = Boolean.TRUE.equals(call.getBoolean("enabled", true));
        QuickStore.prefs(getContext()).edit().putBoolean("weekly", on).apply();
        call.resolve();
    }

    @PluginMethod
    public void peekPending(PluginCall call) {
        JSObject r = new JSObject();
        r.put("items", QuickStore.peekPending(getContext()));
        call.resolve(r);
    }

    @PluginMethod
    public void ackPending(PluginCall call) {
        Double upTo = call.getDouble("upTo", 0.0);
        QuickStore.ackPending(getContext(), upTo == null ? 0 : upTo.longValue());
        QuickStore.updateWidgets(getContext());
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
