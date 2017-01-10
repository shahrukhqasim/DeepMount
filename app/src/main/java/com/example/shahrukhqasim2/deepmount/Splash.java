package com.example.shahrukhqasim2.deepmount;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class Splash extends AppCompatActivity {
    public static final String TAG="SplashActivity";



    TextView textViewStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        textViewStatus=(TextView)findViewById(R.id.textViewSplashStatus);

        registerBluetoothReceiver();

        textViewStatus.setText("Loading...");

        // Step 1: Turn on bluetooth
        turnOnBluetooth();
    }

    void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothEnabledListener, filter);
    }

    void turnOnBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        textViewStatus.setText("Turning on Bluetooth...");
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG,"Bluetooth was not enabled");
            bluetoothAdapter.enable();
        }
        else {
            // Step 2: Check permissions:
            enablePermissions();
        }
    }


    public static boolean checkPermission(final Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static final int PERMISSION_LOCATION_REQUEST_CODE=1001;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST_CODE:
                Log.d(TAG,"On permissions result");
                enablePermissions();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showPermissionDialog() {
        if (!checkPermission(this)) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_LOCATION_REQUEST_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBluetoothReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bluetoothEnabledListener);
    }

    void enablePermissions() {
        textViewStatus.setText("Checking permissions...");
        if(!checkPermission(Splash.this)) {
            showPermissionDialog();
        }
        else {
            // Step 3: Start the next activity. We are ready!
//            Toast.makeText(this, "Over and out!", Toast.LENGTH_SHORT).show();
            textViewStatus.setText("Loading...");
            Log.d(TAG,"Everything done. So splashing for 3 seconds");

            new CountDownTimer(3000, 3000) {

                public void onTick(long millisUntilFinished) {
                    Log.d(TAG,"Rem: "+millisUntilFinished);
                }

                public void onFinish() {
                    Log.d(TAG,"3 seconds over");
                    // TODO: Starting device scan activity here
                    Intent nextActivityLauncher=new Intent(Splash.this,DeviceScanActivity.class);
                    startActivity(nextActivityLauncher);
                    finish();
                    cancel();
//                    mTextField.setText("done!");
                }
            }.start();

        }
    }




    private final BroadcastReceiver bluetoothEnabledListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        // Step 2: Bluetooth is on. Now request for location permissions
                        Log.d(TAG,"Bluetooth is now enabled");
                        enablePermissions();
                        break;
                }
            }
        }
    };
}
