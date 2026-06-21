package com.nxthy.pr;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.os.UserManager;
import android.provider.Settings;
import android.net.wifi.WifiManager;
import android.widget.Toast;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponentName;
    private ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
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

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Hide navigation bar and status bar
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Add long-press to unlock for safety
        findViewById(R.id.splash_text).setOnLongClickListener(v -> {
            showUnlockDialog();
            return true;
        });

        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            startKioskMode();
            setupNetworkMonitoring();
        } else {
            Toast.makeText(this, "This app is not provisioned as Device Owner. Please provision it.", Toast.LENGTH_LONG).show();
            // Optionally, guide the user to provision the app
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This app needs to be a Device Owner to function as a kiosk.");
            startActivityForResult(intent, 1);
        }
    }

    private void startKioskMode() {
        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            devicePolicyManager.setLockTaskPackages(adminComponentName, new String[]{getPackageName()});
            startLockTask();
            isKioskModeActive = true;

            // Apply restrictions
            devicePolicyManager.addUserRestriction(adminComponentName, UserManager.DISALLOW_SAFE_BOOT);
            devicePolicyManager.addUserRestriction(adminComponentName, UserManager.DISALLOW_FACTORY_RESET);
            devicePolicyManager.addUserRestriction(adminComponentName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);
            devicePolicyManager.addUserRestriction(adminComponentName, UserManager.DISALLOW_ADJUST_VOLUME);
            devicePolicyManager.setStatusBarDisabled(adminComponentName, true);
            devicePolicyManager.setKeyguardDisabled(adminComponentName, true);

            // Disable power button menu
            devicePolicyManager.setGlobalSetting(adminComponentName, Settings.Global.GLOBAL_ACTIONS_PANEL_ENABLED, "0");

            Toast.makeText(this, "Kiosk Mode Activated", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopKioskMode() {
        if (isKioskModeActive) {
            stopLockTask();
            isKioskModeActive = false;

            // Remove restrictions
            devicePolicyManager.clearUserRestriction(adminComponentName, UserManager.DISALLOW_SAFE_BOOT);
            devicePolicyManager.clearUserRestriction(adminComponentName, UserManager.DISALLOW_FACTORY_RESET);
            devicePolicyManager.clearUserRestriction(adminComponentName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);
            devicePolicyManager.clearUserRestriction(adminComponentName, UserManager.DISALLOW_ADJUST_VOLUME);
            devicePolicyManager.setStatusBarDisabled(adminComponentName, false);
            devicePolicyManager.setKeyguardDisabled(adminComponentName, false);

            // Enable power button menu
            devicePolicyManager.setGlobalSetting(adminComponentName, Settings.Global.GLOBAL_ACTIONS_PANEL_ENABLED, "1");

            Toast.makeText(this, "Kiosk Mode Deactivated", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNetworkMonitoring() {
        networkCallback = new NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                checkNetworkCapabilities(network);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                checkNetworkCapabilities(network);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                // If the specific Wi-Fi is lost, re-lock if it was unlocked by Wi-Fi
                // (This part needs more sophisticated state management if multiple unlock methods are used)
                if (!isKioskModeActive) {
                    startKioskMode();
                }
            }
        };

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
    }

    private void checkNetworkCapabilities(Network network) {
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities != null) {
            String ssid = getWifiSSID(network);
            if (WIFI_SSID.equals(ssid)) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    // Network is validated and has internet access
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Internet access detected on PHONE_01! Unlocking.", Toast.LENGTH_SHORT).show();
                        stopKioskMode();
                    });
                } else {
                    // Connected to WIFI_SSID but no internet (captive portal)
                    // Optionally, you can perform an HTTP ping here for more robust check
                    new Thread(() -> {
                        if (isInternetReachable()) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "HTTP ping successful on PHONE_01! Unlocking.", Toast.LENGTH_SHORT).show();
                                stopKioskMode();
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Connected to PHONE_01, but no internet. Kiosk mode active.", Toast.LENGTH_SHORT).show();
                                startKioskMode(); // Ensure kiosk mode is active if internet is lost
                            });
                        }
                    }).start();
                }
            }
        }
    }

    private String getWifiSSID(Network network) {
        // This method is simplified. In a real app, you'd use WifiManager to get the SSID
        // and associate it with the Network object. This is complex and requires more permissions.
        // For this example, we'll assume the network is PHONE_01 if it's a Wi-Fi network.
        // A more robust solution would involve iterating through WifiInfo and comparing BSSID/SSID.
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                return wifiInfo.getSSID().replace("\"", ""); // Remove quotes from SSID
            }
        }
        return null;
    }

    private boolean isInternetReachable() {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://www.google.com").openConnection();
            urlConnection.setConnectTimeout(1500);
            urlConnection.setReadTimeout(1500);
            urlConnection.connect();
            return urlConnection.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            // Close any system dialogs that might appear
            Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialogs);
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from exiting kiosk mode
        if (isKioskModeActive) {
            Toast.makeText(this, "Back button disabled in Kiosk Mode.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isKioskModeActive) {
            // Block power button, volume buttons, etc.
            if (keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showUnlockDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Unlock");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Unlock", (dialog, which) -> {
            String password = input.getText().toString();
            if (ADMIN_PASSWORD.equals(password)) {
                stopKioskMode();
                Toast.makeText(MainActivity.this, "Device Unlocked!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        // Ensure kiosk mode is stopped if app is destroyed (e.g., for development)
        stopKioskMode();
    }
}
