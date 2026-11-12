package com.mario.miscuentas;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(QuickAddPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
