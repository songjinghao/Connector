# Connector
A convenient and compact Server and Client communication toolkit, implemented through AIDL.


# Getting started

# GRADLE

``` java
    implementation 'com.paul.song.library:connector:(insert latest version)'
```

# Server

Declare `ConnectService` in `AndroidManifest.xml` in `src\main` of your Server project as following.

``` java
    <service android:name="com.paul.song.connector.ConnectService"
        android:enabled="true"
        android:exported="true" />
```

Set `OnDataListener` to receive data from Clients.

``` java
    ProcessManager.getInstance().setOnDataListener(new OnDataListener() {
        @Override
        public void onReceive(String data) {
            Log.d(TAG, "Server receive from Client : " + data);
        }
    });
```

Dispatch data to all clients.

``` java
    try {
        ProcessManager.getInstance().dispatch("Test All");
    } catch (RemoteException e) {
        e.printStackTrace();
    }
```

Dispatch data to the specified Client by package name.

``` java
    try {
        ProcessManager.getInstance().dispatch(clientPackageName, "Test one");
    } catch (RemoteException e) {
        e.printStackTrace();
    }
```


# Client

Connect to `Server` by specifying the package name, then you can check the Connection Status by `ProcessManager.getInstance().isConnected()`.

``` java
    ProcessManager.getInstance().connect(this, serverPackageName);
```

You can also use `OnServiceConnectionListener` to receive the Connection Status.

``` java
    ProcessManager.getInstance().connect(this, serverPackageName, new OnServiceConnectionListener() {
        @Override
        public void onServiceConnected(ComponentName componentName) {
            Log.d(TAG, "onServiceConnected " + componentName.flattenToShortString());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected " + componentName.flattenToShortString());
        }
      });
```

Send data to `Server`.

``` java
    try {
        ProcessManager.getInstance().sendToHost("I am client!");
    } catch (RemoteException e) {
        e.printStackTrace();
    }
```

Send data to `Server` by specifying the package name.

``` java
    try {
        ProcessManager.getInstance().sendToHost(serverPackageName, "I am client!");
    } catch (RemoteException e) {
        e.printStackTrace();
    }
```

Register `OnCallbackListener` to receive data from `Server` by specifying the package name.

``` java
    try {
        ProcessManager.getInstance().register(serverPackageName, new OnCallbackListener() {
            @Override
            public void onCallback(String s) {
                Log.d(TAG, "Client receive from Server : " + s);
            }
        });
    } catch (RemoteException e) {
        e.printStackTrace();
    }
```

Remember to unregister `OnCallbackListener` and unconnect to Server at the end of your Application.
