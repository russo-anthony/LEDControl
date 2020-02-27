/*
Anthony Russo & Derek Arnheiter
ELC 343 - MicroComputer Systems
Main Activity - BLE LED Control
 */

package control.led.ble.ledcontrol;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class MainActivity extends AppCompatActivity {

    // TAG is used for informational messages
    private final static String TAG = MainActivity.class.getSimpleName();

    // Variables to access objects from the layout such as buttons, seekbar, values
    private static TextView compareValue;
    private static TextView intensity_text;
    private static Button add_button;
    private static Button subtract_button;
    private static SeekBar seek_seekBar;
    private static Button power_button;
    private static Button search_button;
    private static Button connect_button;
    private static Button discover_button;
    private static boolean power_state;

    // Variables to manage BLE connection
    private static boolean mConnectState;
    private static boolean mBindState;
    private static boolean mServiceConnected;
    ledService mledService;

    //Intensity Variable
    private static int intensity;

    private static final int REQUEST_ENABLE_BLE = 1;

    //This is required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    /**
     * This is called when the main activity is first created
     *
     * @param savedInstanceState is any state saved from prior creations of this activity
     */
    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up a variables to access compareValue and text box
        intensity_text = (TextView) findViewById(R.id.textIntensity);
        compareValue = (TextView) findViewById(R.id.number);

        // Set up variables for accessing buttons and seek bar
        add_button = (Button) findViewById(R.id.add);
        subtract_button = (Button) findViewById(R.id.subtract);
        power_button = (Button) findViewById(R.id.power);
        seek_seekBar = (SeekBar) findViewById(R.id.seekBar);
        search_button = (Button) findViewById(R.id.search);
        connect_button = (Button) findViewById(R.id.connect);
        discover_button = (Button) findViewById(R.id.discover);

        // Initialize service and connection state variable
        mServiceConnected = false;
        mConnectState = false;
        mBindState = false;

        //Initialize Power State
        power_state = false;
        power_button.setEnabled(true);

        //Initialize Intensity
        intensity = 0;

        //Initialize Seek Bar
        seek_seekBar.setEnabled(false);
        seek_seekBar.setMax(255);

        seek_seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                compareValue.setText(Integer.toString(progress));
                updateCompare(progress);
            }
        });

        //This section required for Android 6.0 (Marshmallow)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access ");
                builder.setMessage("Please grant location access so this app can detect devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        } //End of section for Android 6.0 (Marshmallow)
    }

    //This method required for Android 6.0 (Marshmallow)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission for 6.0:", "Coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    } //End of section for Android 6.0 (Marshmallow)

    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver. This specified the messages the main activity looks for from the ledService
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ledService.ACTION_BLESCAN_CALLBACK);
        filter.addAction(ledService.ACTION_CONNECTED);
        filter.addAction(ledService.ACTION_DISCONNECTED);
        filter.addAction(ledService.ACTION_SERVICES_DISCOVERED);
        filter.addAction(ledService.ACTION_DATA_RECEIVED);
        registerReceiver(mBleUpdateReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBleUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close and unbind the service when the activity goes away
        mledService.close();
        unbindService(mServiceConnection);
        mledService = null;
        mServiceConnected = false;
    }

    /**
     * This method handles the start bluetooth button
     *
     * //@param view the view object
     */
    public void startBluetooth(View view) {

        // Find BLE service and adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLE);
        }

        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(this, ledService.class);
        mBindState = bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        Log.d(TAG, "Bluetooth is Enabled");
    }

    /**
     * This method handles the Search for Device button
     *
     * //@param view the view object
     */
    public void searchBluetooth(View view) {
        if(mServiceConnected) {
            mledService.scan();
        }
        /* After this we wait for the scan callback to detect that a device has been found */
        /* The callback broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Connect to Device button
     *
     * //@param view the view object
     */
    public void connectBluetooth(View view) {
         mledService.connect();
        /* After this we wait for the gatt callback to report the device is connected */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Discover Services and Characteristics button
     *
     * //@param view the view object
     */
    public void discoverServices(View view) {
        /* This will discover both services and characteristics */
        mledService.discoverServices();

        /* After this we wait for the gatt callback to report the services and characteristics */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Disconnect button
     *
     * @param view the view object
     */
    public void Disconnect(View view) {
        mledService.disconnect();

        /* After this we wait for the gatt callback to report the device is disconnected */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the add button
     *
     * @param view the view object
     */
    public void IncreaseIntensity(View view)
    {
        if(intensity >= 255)
        {
            compareValue.setText("Max");
            updateCompare(intensity);
        }
        else
        {
            intensity++;
            compareValue.setText(Integer.toString(intensity));
            updateCompare(intensity);
        }
    }

    /**
     * This method handles the subtract button
     *
     * @param view the view object
     */
    public void DecreaseIntensity(View view)
    {
        if(intensity <= 0)
        {
            compareValue.setText("Min");
            updateCompare(intensity);
        }
        else
        {
            intensity--;
            compareValue.setText(Integer.toString(intensity));
            updateCompare(intensity);
        }
    }

    public void updateCompare(int val)
    {
        intensity = val;
        mledService.updateVal(val);
        Log.d(TAG, "--Update--");
    }

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and initialize the service.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * This is called when the ledService is connected
         *
         * @param componentName the component name of the service that has been connected
         * @param service service being bound
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mledService = ((ledService.LocalBinder) service).getService();
            mServiceConnected = true;
            mledService.initialize();
        }

        /**
         * This is called when the ledService is disconnected.
         *
         * @param componentName the component name of the service that has been connected
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            mledService = null;
        }
    };

    /**
     * This method handles the power button
     *
     * @param view the view object
     */
    public void powerButton(View view) {

        if(!power_state)    //Turn Power On
        {
            //Start Bluetooth
            startBluetooth(view);
            search_button.setEnabled(true);

            //Reset Intensity
            intensity = 0;

            //Reset Text Value
            compareValue.setText("0");

            //Flip Power Boolean
            power_state = true;
        }
        else    //Turn Power Off
        {
            //Run Disconnect Procedures
            Disconnect(view);

            //Reset Intensity
            intensity = 0;

            //Reset Text Value
            compareValue.setText("0");

            //Flip Power Boolean
            power_state = false;
        }
    }

    /**
     * Listener for BLE event broadcasts
     */
    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case ledService.ACTION_BLESCAN_CALLBACK:
                    search_button.setEnabled(false);
                    connect_button.setEnabled(true);
                    break;

                case ledService.ACTION_CONNECTED:
                        mConnectState = true;
                        discover_button.setEnabled(true);
                        connect_button.setEnabled(false);
                        Log.d(TAG, "Connected to Device");
                    break;

                case ledService.ACTION_DISCONNECTED:
                    mConnectState = false;
                    Log.d(TAG, "Disconnected");
                    //Disable App Interface
                    add_button.setEnabled(false);
                    subtract_button.setEnabled(false);
                    seek_seekBar.setEnabled(false);
                    intensity_text.setEnabled(false);
                    compareValue.setEnabled(false);
                    mledService.close();
                    break;

                case ledService.ACTION_SERVICES_DISCOVERED:
                    Log.d(TAG, "Services Discovered");
                    //Enable App Interface
                    add_button.setEnabled(true);
                    subtract_button.setEnabled(true);
                    seek_seekBar.setEnabled(true);
                    intensity_text.setEnabled(true);
                    compareValue.setEnabled(true);
                    discover_button.setEnabled(false);
                    break;

                case ledService.ACTION_DATA_RECEIVED:
                default:
                    break;
            }
        }
    };
}