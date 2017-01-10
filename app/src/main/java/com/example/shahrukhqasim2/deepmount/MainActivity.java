package com.example.shahrukhqasim2.deepmount;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import me.angrybyte.circularslider.CircularSlider;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;

    CircularSlider slider1,slider2;

    TextView textViewConnectionStatus;

    TextView textViewAngle1,textViewAngle2,textViewAngle;

    public static final String TAG = "MainActivity";

    void updateAnglesText(double d1, double d2) {
        Log.d(TAG,"1: "+d1);
        Log.d(TAG,"2: "+d2);
        if(d1!=-100) {
            textViewAngle1.setText(""+(Math.round(d1*90)+90)+(char)(176));
        }
        if(d2!=-100) {
            textViewAngle2.setText(""+(Math.round(d2*90)+90)+(char)(176));
        }
    }

    private void sendData(byte[] data) {
        if(writeCharacteristic ==null)
            return;

        // TODO: Implement this function
        writeCharacteristic.setValue(data);
        mBluetoothLeService.writeCharacteristic(writeCharacteristic);
    }


    private void dataReceived(byte[] data) {
        Log.d(TAG,"Data received: "+data.length);
        String dataB="";
        for(byte i:data) {
            dataB+=""+(int)i+"-";
        }
        Log.d(TAG,"Data ris: "+dataB);
        // TODO: Implement this function
        if(data.length!=4)
            return;
        Float f= ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        textViewAngle.setText("Angle: "+f);
    }

    public void sendData(View view) {
        if(writeCharacteristic ==null)
            return;
        try {
            byte[] array = new byte[1];
            array[0] = Byte.parseByte(((EditText) findViewById(R.id.editTextData)).getText().toString());
            writeCharacteristic.setValue(array);
            sendData(array);
            Log.d(TAG,"Sending data");
        }
        catch (Exception e) {
            Log.d(TAG,"Error: "+e.getMessage());
        }
    }

    public void subscribe() {
        if (mGattCharacteristics == null) {
            Log.e(TAG, "Error subscribe null");
            return;
        }

        try {
            // Find out the index opsition
            final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(3).get(1);
            writeCharacteristic = characteristic;
            final BluetoothGattCharacteristic characteristic2 = mGattCharacteristics.get(3).get(0);
            readCharacteristic = characteristic2;
            mBluetoothLeService.setCharacteristicNotification(readCharacteristic, true);

            Log.d(TAG,"Subscribed!");
        }
        catch (Exception e) {
            Log.d(TAG,"Result: "+e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewConnectionStatus = (TextView) findViewById(R.id.textViewConnectionStatus);
        slider1=(CircularSlider)findViewById(R.id.sliderCamera1);
        slider2=(CircularSlider)findViewById(R.id.sliderCamera2);
        textViewAngle1=(TextView)findViewById(R.id.textViewAngle1);
        textViewAngle2=(TextView)findViewById(R.id.textViewAngle2);
        textViewAngle=(TextView)findViewById(R.id.textViewAngle);

        slider1.setOnSliderMovedListener(new CircularSlider.OnSliderMovedListener() {
            @Override
            public void onSliderMoved(double pos) {
                updateAnglesText(pos,-100);
            }
        });

        slider2.setOnSliderMovedListener(new CircularSlider.OnSliderMovedListener() {
            @Override
            public void onSliderMoved(double pos) {
                updateAnglesText(-100,pos);
            }
        });

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

//        getSupportActionBar().setTitle(mDeviceName);
//        getSupportActionBar()m.setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void handleCharacteristics(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        if (gattServices == null) return;
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
            }
            mGattCharacteristics.add(charas);
        }
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewConnectionStatus.setText(resourceId);
            }
        });
    }


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: When data is received
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                handleCharacteristics(mBluetoothLeService.getSupportedGattServices());
                // Subscribe to characteristics to receive data
                subscribe();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                dataReceived(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));

            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
