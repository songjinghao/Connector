package com.paul.song.thirdapplication;

import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.paul.song.connector.OnDataListener;
import com.paul.song.connector.ProcessManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ProcessManager.getInstance().setOnDataListener(new OnDataListener() {
            @Override
            public void onReceive(String data) {
                Log.d(TAG, "Server receive data from client: " + data);
            }
        });


    }

    public void dispatchAll(View view) {
        try {
            ProcessManager.getInstance().dispatch("Test dispacth all!!!");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void dispatchSingle(View view) {
        try {
            ProcessManager.getInstance().dispatch("com.paul.song.anotherapplication", "Test dispacth single!!!");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
