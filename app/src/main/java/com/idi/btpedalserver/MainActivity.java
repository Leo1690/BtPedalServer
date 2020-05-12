package com.idi.btpedalserver;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;
    UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
    Button meassureButton;
    String sendData;
    boolean mBusy = false;
    float scale = 8;
    public void startMeassure(){
        sensorMan.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopMeassure(){
        sensorMan.unregisterListener(this);
    }

    public void sendBtMsg(String msg2send){
        if (mmSocket.isConnected()) {
            try {
                String msg = msg2send;
                OutputStream mmOutputStream = mmSocket.getOutputStream();
                mmOutputStream.write(msg.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                stopMeassure();
                closeSocket();
                mBusy=false;
                meassureButton.setText("Start");
            }
        }
    }
    SensorManager sensorMan;
    Sensor accelerometer;

    public boolean openSocket(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("leo-GL552VW")) {
                    mmDevice = device;
                    break;
                }
            }
        }
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()) {
                mmSocket.connect();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void closeSocket(){
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorMan = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sendData="";
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        meassureButton = (Button) findViewById(R.id.meassureButton);
        final EditText scaleEditText = (EditText) findViewById(R.id.scaleEditText);
        meassureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBusy){
                    stopMeassure();
                    closeSocket();
                    mBusy=false;
                    meassureButton.setText("Start");
                }else{
                    boolean connected = openSocket();
                    if (connected){
                        meassureButton.setText("Stop");
                        scale = Float.parseFloat(scaleEditText.getText().toString());
                        mBusy = true;
                        startMeassure();
                    }
                }
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            float[] mAcc=event.values.clone();
            int fx = Math.abs(Math.round(mAcc[0]*scale));
            int fy = Math.abs(Math.round(mAcc[1]*scale));
            int fz = Math.abs(Math.round(mAcc[2]*scale));
            int maxA = Math.max(Math.max(fx,fy),fz);
            int arA = (maxA+fx+fy+fz)>>1;
            char resA = (char)((arA>255)?255:arA);
            sendData=resA+"";
            sendBtMsg(sendData);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
