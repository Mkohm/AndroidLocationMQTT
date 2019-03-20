package com.example.mhew.hololensbluetoothgps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    TextView textViewTime;
    TextView textViewLatitude;
    TextView textViewLongitude;


    LocationManager mLocationManager;
    static final int PERMISSION_RESULT_CODE = 1;
    Location currentLocation;
    private MqttAndroidClient mqttAndroidClient;
    private String clientId;
    private String subscriptionTopic = "EIT/HololensMazemap";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup UI References
        textViewTime = (TextView) findViewById(R.id.textViewTime);
        textViewLatitude = (TextView) findViewById(R.id.textViewLatitude);
        textViewLongitude = (TextView) findViewById(R.id.textViewLongitude);


        clientId = "androidApp" + System.currentTimeMillis();

        String serverUri = "tcp://broker.hivemq.com:1883";
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                String a = "1";
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String a = "1";

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                String a = "1";

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);


        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions =
                            new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    String a = "1";
                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }

    }


    @Override
    protected void onResume() {
        super.onResume();

        // only process GPS when activity is running.
        int permissionCheck = ContextCompat.checkSelfPermission((Context) this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            startGPS();
        } else {

            // No explanation needed, we can request the permission.
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_RESULT_CODE);
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        stopGPS();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_RESULT_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    startGPS();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "ACCESS_FINE_LOCATION Denied. Quitting!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            UpdatePosition(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    public void startGPS() {
        //Execute location service call if user has explicitly granted ACCESS_FINE_LOCATION..
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
                    listener);
            currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            UpdatePosition(currentLocation);
            Log.i("GPS_Receiver", "startGPS: GPS Started..");
        } catch (SecurityException e) {
        }
    }

    public void stopGPS() {
        if (mLocationManager == null) return;
        mLocationManager.removeUpdates(listener);
    }

    public void UpdatePosition(Location location) {
        currentLocation = location;
        textViewTime.setText(new SimpleDateFormat("HH.mm.ss").format(new Date()));
        textViewLatitude.setText(Double.toString(currentLocation.getLatitude()));
        textViewLongitude.setText(Double.toString(currentLocation.getLongitude()));

        MqttMessage message = new MqttMessage();
        JSONObject object = new JSONObject();
        try {
            object.put("latitude", location.getLatitude());
            object.put("longitude", location.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        message.setPayload(object.toString().getBytes());


        if (mqttAndroidClient.isConnected()) {
            try {
                mqttAndroidClient.publish(subscriptionTopic, message);
            } catch (MqttException e) {
                e.printStackTrace();
            }

        }
    }

    public void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }


}
