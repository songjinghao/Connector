package com.paul.song.connector;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ConnectService extends Service {

    private static final String TAG = ConnectService.class.getSimpleName();

    private MyBinder binder = new MyBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind");
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    private static class MyBinder extends ProcessInterface.Stub {

        @Override
        public void send(String data) throws RemoteException {
            ProcessManager.getInstance().receiveFromClient(data);
        }

        @Override
        public void register(String clientPackageName, ICallback listener) throws RemoteException {
            ProcessManager.getInstance().put(clientPackageName, listener.asBinder());
            Log.d(TAG, "[" + clientPackageName + "] has register listener.");
        }

        @Override
        public void unregister(String clientPackageName) throws RemoteException {
            ProcessManager.getInstance().remove(clientPackageName);
        }
    }
}
