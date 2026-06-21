package com.nxthy.pr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponentName;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isKioskModeActive = false;
    private final String ADMIN_PASSWORD = "nath261920";
    private final String WIFI_SSID = "PHONE_01";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponentName = new ComponentName(this, AdminReceiver.class);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        findViewById(R.id.splash_text).setOnLongClickListener(v -> {
            showUnlockDialog();
            return true;
        });

        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            startKioskMode();
            setupNetworkMonitoring();
        } else {
            Toast.makeText(this, "Please provision as Device Owner for full security.", Toast.LENGTH_LONG).show();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void startKioskMode() {
        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                devicePolicyManager.setLockTaskPackages(adminComponentName, new String[]{getPackageName()});
                startLockTask();
            }
            isKioskModeActive = true;
            
            // Apply restrictions with version checks
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                devicePolicyManager.addUserRestriction(adminComponentName, UserManager.DISALLOW_SAFE_BOOT);
                devicePolicyManager.addUserRestriction(adminComponentName, UserManager.DISALLOW_FACTORY_RESET);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                devicePolicyManager.setStatusBarDisabled(adminComponentName, true);
                devicePolicyManager.setKeyguardDisabled(adminComponentName, true);
            }
        }
    }

    private void stopKioskMode() {
        if (isKioskModeActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask();
            }
            isKioskModeActive = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                devicePolicyManager.setStatusBarDisabled(adminComponentName, false);
                devicePolicyManager.setKeyguardDisabled(adminComponentName, false);
            }
            Toast.makeText(this, "Kiosk Mode Deactivated", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNetworkMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) { checkNetwork(network); }
                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities cap) { checkNetwork(network); }
            };
            NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    private void checkNetwork(Network network) {
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities != null) {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String ssid = wm.getConnectionInfo().getSSID().replace("\"", "");
            if (WIFI_SSID.equals(ssid)) {
                new Thread(() -> {
                    if (isInternetReachable()) {
                        runOnUiThread(this::stopKioskMode);
                    }
                }).start();
            }
        }
    }

    private boolean isInternetReachable() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://www.google.com" ).openConnection();
            conn.setConnectTimeout(1500);
            conn.connect();
            return conn.getResponseCode() == 200;
        } catch (Exception e) { return false; }
    }

    private void showUnlockDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Unlock");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("Unlock", (d, w) -> {
            if (ADMIN_PASSWORD.equals(input.getText().toString())) stopKioskMode();
        });
        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isKioskModeActive && (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_BACK)) return true;
        return super.onKeyDown(keyCode, event);
    }
}
