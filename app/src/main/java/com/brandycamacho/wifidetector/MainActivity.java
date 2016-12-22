package com.brandycamacho.wifidetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.pwittchen.reactivewifi.ReactiveWifi;
import com.github.pwittchen.reactivewifi.WifiSignalLevel;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private Subscription wifiSubscription;
    private ReactiveWifi reactiveWifi;
    private Subscription signalLevelSubscription;
    private Subscription supplicantSubscription;
    private Subscription wifiInfoSubscription;
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1000;

    private static final String WIFI_SIGNAL_LEVEL_MESSAGE = "WiFi signal level: ";
    public static final boolean IS_PRE_M_ANDROID = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;


    TextView txt_info;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt_info = (TextView) findViewById(R.id.txt_info);

        txt_info.setText("What up?");
        txt_info.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    @Override protected void onResume() {
        super.onResume();

        reactiveWifi = new ReactiveWifi();

        signalLevelSubscription = reactiveWifi.observeWifiSignalLevel(getApplicationContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<WifiSignalLevel>() {
                    @Override public void call(final WifiSignalLevel level) {
                        Log.d("TESTER", level.toString());
                        final String description = level.description;
                        txt_info.setText(WIFI_SIGNAL_LEVEL_MESSAGE.concat(String.valueOf(level.level)));
                    }
                });

        if (!isCoarseLocationPermissionGranted()) {
            requestCoarseLocationPermission();
        } else if (isCoarseLocationPermissionGranted() || IS_PRE_M_ANDROID) {
//            startWifiAccessPointsSubscription();
        }

        startSupplicantSubscription();
        startWifiInfoSubscription();
    }

    private void startSupplicantSubscription() {
        supplicantSubscription = reactiveWifi.observeSupplicantState(getApplicationContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<SupplicantState>() {
                    @Override
                    public void call(SupplicantState supplicantState) {
                        Log.d("ReactiveWifi", "New supplicant state: " + supplicantState.toString());
                    }
                });
    }

    private void startWifiInfoSubscription() {
        wifiInfoSubscription = reactiveWifi.observeWifiAccessPointChanges(getApplicationContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<WifiInfo>() {
                    @Override
                    public void call(WifiInfo wifiInfo) {
                        Log.d("ReactiveWifi", "New BSSID: " + wifiInfo.getBSSID());
                    }
                });
    }

//    private void startWifiAccessPointsSubscription() {
//        wifiSubscription = reactiveWifi.observeWifiAccessPoints(getApplicationContext())
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Action1<List<ScanResult>>() {
//                    @Override public void call(final List<ScanResult> scanResults) {
//                        displayAccessPoints(scanResults);
//                    }
//                });
//    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                     int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final boolean isCoarseLocation = requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION;
        final boolean permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (isCoarseLocation && permissionGranted && wifiSubscription == null) {
//            startWifiAccessPointsSubscription();
        }
    }

    private void requestCoarseLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
        }
    }

    private boolean isCoarseLocationPermissionGranted() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

}
