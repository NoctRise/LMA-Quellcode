package com.example.student.lokalmobil.praktikum3;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.student.lokalmobil.Manifest;
import com.example.student.lokalmobil.R;
import com.example.student.lokalmobil.realm.ServerLog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;


public class Praktikum3Activity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener {

    private final String TAG = getClass().getSimpleName();
    private final String ZEIT = "Zeit in Sekunden";
    private final String ENERGIE = "Energie-Effizenz in km/h";
    private final String DISTANZ = "Distanz in Meter";
    private final String STILLSTAND = "Stillstandswert";
    Button senden;
    Spinner dropDown;
    EditText config;
    EditText stillText;
    MeinEchoClient meinEchoClient;
    LocationManager locationManager;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    String locationString = "";
    TimerTask doAsynchronousTask;
    boolean isRunning;
    long last_time;
    int counter = 0;
    private boolean energieBool;
    private boolean distanzBool;
    private boolean stillBool;
    private Location lastGPSfix;
    private SensorManager sensorManager;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    private Realm realmInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_praktikum3);

        Realm.init(this);
        realmInstance = Realm.getDefaultInstance();


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        last_time = System.nanoTime();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Keine Rechte du Linke!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        senden = (Button) findViewById(R.id.buttonSenden3);
        config = (EditText) findViewById(R.id.textConfig);
        stillText = (EditText) findViewById(R.id.editStill);

        dropDown = (Spinner) findViewById(R.id.dropDown);
        String[] items = new String[]{ZEIT, DISTANZ, ENERGIE, STILLSTAND};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, items);
        dropDown.setAdapter(adapter);

        meinEchoClient = new MeinEchoClient();
        meinEchoClient.execute();
        senden.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (!meinEchoClient.isCancelled()) {
                    if (dropDown.getSelectedItem().toString().equals(ZEIT)) {
                        periodischesSpeichern();
                    } else if (dropDown.getSelectedItem().toString().equals(DISTANZ)) {
                        if (senden.getText().toString().equals("Start")) {
                            lastGPSfix = mLastLocation;
                            meinEchoClient.setMessageToSend(locationString);
                            saveLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude());
                            meinEchoClient.setSend(true);
                            distanzBool = true;
                            senden.setText("Stop");
                        } else {
                            distanzBool = false;
                            senden.setText("Start");
                        }
                    } else if (dropDown.getSelectedItem().toString().equals(ENERGIE)) {
                        if (senden.getText().toString().equals("Start")) {
                            lastGPSfix = mLastLocation;
                            meinEchoClient.setMessageToSend(locationString);
                            saveLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude());
                            meinEchoClient.setSend(true);
                            energieBool = true;
                            senden.setText("Stop");
                        } else {
                            energieBool = false;
                            senden.setText("Start");
                        }
                    } else if (dropDown.getSelectedItem().toString().equals(STILLSTAND)) {
                        if (senden.getText().toString().equals("Start")) {
                            lastGPSfix = mLastLocation;
                            meinEchoClient.setMessageToSend(locationString);
                            saveLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude());
                            meinEchoClient.setSend(true);
                            stillBool = true;
                            senden.setText("Stop");
                        } else {
                            stillBool = false;
                            senden.setText("Start");
                        }
                    }
//                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void changeLocationString() {
        locationString = dropDown.getSelectedItem().toString() + " " + mLastLocation.getLongitude() + " " + mLastLocation.getLatitude() + " " + mLastLocation.getAltitude();
    }

    // startet einen Timertask, um jede Sekunde den Standort an den Server zu senden
    // bricht den Task mit doAsynchronousTask.cancel() ab, sobald auf stop gedrückt wurde
    public void periodischesSpeichern() {
        if (config.getText().toString().equals("")) {
            Toast.makeText(this, "Keine Zeitangabe", Toast.LENGTH_SHORT).show();
        } else {
            if (!isRunning) {
                senden.setText("Stop");
                isRunning = true;
                long period = Long.parseLong(config.getText().toString()) * 1000;

                final Handler handler = new Handler();
                Timer timer = new Timer();
                doAsynchronousTask = new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            public void run() {
                                meinEchoClient.setMessageToSend(locationString);
                                Log.e("Periodisch", locationString);
                                meinEchoClient.setSend(true);
                                Log.d(TAG, "Gesendet: " + locationString);
                                saveLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude());
                            }
                        });
                    }
                };
                timer.schedule(doAsynchronousTask, 0, period);
            } else {
                isRunning = false;
                doAsynchronousTask.cancel();
                senden.setText("Start");
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        meinEchoClient.setStop(true);
        realmInstance.close();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    // Wird bei jeder Veränderung des Standortes aufgerufen
    @Override
    public void onLocationChanged(Location location) {
        senden.setClickable(true);
        mLastLocation = location;
        changeLocationString();
        Log.e("OnloCationChanged", locationString);
        if (distanzBool) {
            handleDistanceFix(false);
        } else if (energieBool) {
            handleEnergieFix();
        }
    }

    // Überprüft ob die Distanz zwischen der alten Location und dem letzten GPSfix, returned true, wenn
// die überprüfte Distanz > der eingegebenen min. Distanz ist
    private boolean handleDistanceFix(boolean stillStand) {
        double distanceBetween = mLastLocation.distanceTo(lastGPSfix);
        double minDistance;
        if (stillStand) {
            minDistance = Double.parseDouble(stillText.getText().toString());
        } else {
            minDistance = Double.parseDouble(config.getText().toString());
        }
        Log.d(TAG, "distanceBetween: " + distanceBetween + " minDistance: " + minDistance);
        if (distanceBetween >= minDistance) {
            meinEchoClient.setMessageToSend(locationString);
            saveLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude());
            meinEchoClient.setSend(true);
            Log.e("HandleDistanceFix", locationString);
            lastGPSfix = mLastLocation;
            return true;
        }
        return false;
    }

    private void handleEnergieFix() {
        double timeToCheck = Double.parseDouble(stillText.getText().toString()) / (Double.parseDouble(config.getText().toString()) / 3.6) * 1000;

        long time = System.nanoTime();
        int delta_time = (int) ((time - last_time) / 1000000);
        last_time = time;
        counter += delta_time;

        if (counter >= timeToCheck) {
            // True, damit er den unteren Edittext nimmt ( Distanzschwelle )
            // setze Counter auf 0, wenn Distanzschwelle erreicht wurde
            if (handleDistanceFix(true)) {
                counter = 0;
            }
        }
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Keine Rechte!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            senden.setText("Start");
        } else {
            senden.setText("Noch keine Position gefunden!");
            senden.setClickable(false);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    //Berechnet derzeitige Beschleunigung mit der letzten gespeicherten, um ein Bewegungswert zu berechnet,
    // wenn Bewegung > Stillstandswert, überprüfe ob Distanzschwelle erreicht wurde
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (stillBool) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float[] values = event.values;
                float x = values[0];
                float y = values[1];
                float z = values[2];
                mAccelLast = mAccelCurrent;
                mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
                float delta = mAccelCurrent - mAccelLast;
                mAccel = mAccel * 0.9f + delta;
                // Make this higher or lower according to how much
                // motion you want to detect
                if (mAccel > Double.parseDouble(config.getText().toString())) {
                    handleDistanceFix(true);
                    //handleEnergieFix();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void saveLocation(double longitude, double latitude) {
        Date currentTime = Calendar.getInstance().getTime();
        realmInstance.beginTransaction();
        ServerLog serverLogMessage = realmInstance.createObject(ServerLog.class);

        serverLogMessage.id = serverLogMessage.getNextKey(realmInstance);
        serverLogMessage.strategie = dropDown.getSelectedItem().toString();
        serverLogMessage.latitude = latitude;
        serverLogMessage.longitude = longitude;
        serverLogMessage.timestamp = currentTime.getTime();
        realmInstance.commitTransaction();
        Toast.makeText(this, "Position gespeichert", Toast.LENGTH_SHORT).show();
    }

}
