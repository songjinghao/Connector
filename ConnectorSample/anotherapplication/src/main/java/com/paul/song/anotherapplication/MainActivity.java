package com.paul.song.anotherapplication;

import android.content.ComponentName;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.paul.song.connector.OnCallbackListener;
import com.paul.song.connector.OnServiceConnectionListener;
import com.paul.song.connector.ProcessManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private final String serverPackageName = "com.paul.song.connectorsample";
    private final String anotherServerPackageName = "com.paul.song.thirdapplication";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void bindToServer(View view) {

        ProcessManager.getInstance().connect(this, serverPackageName, new OnServiceConnectionListener() {
            @Override
            public void onServiceConnected(ComponentName componentName) {
                Log.d(TAG, "onServiceConnected " + componentName.flattenToShortString());
                try {
                    ProcessManager.getInstance().register(serverPackageName, new OnCallbackListener() {
                        @Override
                        public void onCallback(String command) throws RemoteException {
                            Log.d(TAG, "Client receive data from Server: " + command);
                            String err = null;
                            err.toString();
                            Log.d(TAG, "Client receive data from Server: " + command + " Test!!");
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(TAG, "onServiceDisconnected " + componentName.flattenToShortString());
            }
        });

        ProcessManager.getInstance().connect(this, anotherServerPackageName, new OnServiceConnectionListener() {
            @Override
            public void onServiceConnected(ComponentName componentName) {
                Log.d(TAG, "onServiceConnected " + componentName.flattenToShortString());
                try {
                    ProcessManager.getInstance().register(anotherServerPackageName, new OnCallbackListener() {
                        @Override
                        public void onCallback(String command) throws RemoteException {
                            Log.d(TAG, "Client receive data from Server: " + command);
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(TAG, "onServiceDisconnected " + componentName.flattenToShortString());
            }
        });
    }

    public void sendToServer(View view) {
        try {
//            ProcessManager.getInstance().sendToHost("I am client!");
            ProcessManager.getInstance().sendToHost(serverPackageName, "I am client!");
//            ProcessManager.getInstance().sendToHost(anotherServerPackageName, "I am client!");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void unbind(View view) {
        /*if (ProcessManager.getInstance().isConnected(serverPackageName)) {
            ProcessManager.getInstance().unconnect(this, serverPackageName);
        }

        if (ProcessManager.getInstance().isConnected(anotherServerPackageName)) {
            ProcessManager.getInstance().unconnect(this, anotherServerPackageName);
        }*/

        ProcessManager.getInstance().unconnect(this, serverPackageName);
        ProcessManager.getInstance().unconnect(this, anotherServerPackageName);

//        ProcessManager.getInstance().unconnect(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
