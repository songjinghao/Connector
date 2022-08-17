package com.paul.song.connector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessManager {

    private static final String TAG = ProcessManager.class.getSimpleName();

    private static final ProcessManager sInstance = new ProcessManager();

    public static ProcessManager getInstance() {
        return sInstance;
    }

    private ProcessManager() {
        listeners = new ConcurrentHashMap<>();

        mListeners = new ConcurrentHashMap<>();
        mProcessInterfaces = new ConcurrentHashMap<>();
        isConnected = new ConcurrentHashMap<>();
        mTempServiceConnectListeners = new ConcurrentHashMap<>();
        mServiceConnectListeners = new ConcurrentHashMap<>();
        mTempProcessConnections = new ConcurrentHashMap<>();
        mProcessConnections = new ConcurrentHashMap<>();
    }

    //---------------------主进程--- 单例1-------------------

    /**
     * 服务端存储客户端注册的listener
     * key: clientPackageName
     */
    private ConcurrentHashMap<String, IBinder> listeners;
    /** 服务端数据回调 */
    private OnDataListener onDataListener;

    void put(String clientPackageName, IBinder listener) {
        if (null == clientPackageName || clientPackageName.isEmpty()) return;

        if (!listeners.containsKey(clientPackageName)) {
            listeners.put(clientPackageName, listener);
        }
    }

    void remove(String clientPackageName) {
        if (null == clientPackageName || clientPackageName.isEmpty()) return;

        listeners.remove(clientPackageName);
    }

    public void setOnDataListener(OnDataListener listener) {
        onDataListener = listener;
    }

    void receiveFromClient(String data) {
        if (onDataListener != null) {
            onDataListener.onReceive(data);
        }
    }

    /**
     * 指定包名定向分发
     * @param clientPackageName 客户端包名
     * @param command
     * @throws RemoteException
     */
    public void dispatch(String clientPackageName, String command) throws RemoteException {
        if (null == clientPackageName || clientPackageName.isEmpty()) return;

        if (listeners.containsKey(clientPackageName)) {
            ICallback callback = ICallback.Stub.asInterface(listeners.get(clientPackageName));
            if (callback != null) {
                try {
                    callback.onCallback(command);
                } catch (RuntimeException e) {
                    Log.e(TAG, e.toString());
                    e.printStackTrace();
                }
            }
        } else {
            Log.d(TAG, "[" + clientPackageName + "] had not register listener!");
        }
    }

    /**
     * 全部分发
     * @param command
     * @throws RemoteException
     */
    public void dispatch(String command) throws RemoteException {
        Collection<IBinder> binders = listeners.values();
        for (IBinder binder : binders) {
            ICallback callback = ICallback.Stub.asInterface(binder);
            if (callback != null) {
                try {
                    // 远程代码产生的异常会通过 _reply.readException();
                    callback.onCallback(command);
                } catch (RuntimeException e) {
                    Log.e(TAG, e.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    //---------------------另外一个对象--- 单例2-------------------

    /** 客户端上下文 */
    private Context mAppContext;
    /** 客户端包名 */
    private String mClientPackageName;
    /**
     * 服务端Binder在客户端的代理集合
     * key: serverPackageName
     */
    private ConcurrentHashMap<String, ProcessInterface> mProcessInterfaces;

    /**
     * 客户端绑定服务端的回调监听器
     * key: serverPackageName
     */
    private ConcurrentHashMap<String, OnCallbackListener> mListeners;

    /**
     * 客户端绑定服务端的回调监听器-临时存储
     * key: serverPackageName
     */
    private ConcurrentHashMap<String, OnServiceConnectionListener> mTempServiceConnectListeners;
    /**
     * 客户端绑定服务端的回调监听器
     * key: serverPackageName
     */
    private ConcurrentHashMap<String, OnServiceConnectionListener> mServiceConnectListeners;

    /**
     * 客户端绑定服务端的连接器
     * key: serverPackageName
     */
    private ConcurrentHashMap<String, ProcessConnection> mProcessConnections;
    /**
     * 客户端绑定服务端的连接器-临时存储
     * key: serverPackageName
     */
    private ConcurrentHashMap<String, ProcessConnection> mTempProcessConnections;

    /**
     * 服务是否已连接
     * key: serverPackageName
     */
    private ConcurrentHashMap<String, Boolean> isConnected;

    public boolean isConnected(String serverPackageName) {
        if (null == serverPackageName || serverPackageName.isEmpty()) return false;

        if (isConnected.containsKey(serverPackageName)) {
            return isConnected.get(serverPackageName);
        } else {
            return false;
        }
    }

    public void connect(Context context) {
        bind(context, null, null);
    }

    public void connect(Context context, OnServiceConnectionListener listener) {
        bind(context, null, listener);
    }

    public void connect(Context context, String serverPackageName) {
        bind(context, serverPackageName, null);
    }

    public void connect(Context context, String serverPackageName, OnServiceConnectionListener listener) {
        bind(context, serverPackageName, listener);
    }

    /**
     * 全部解绑定
     * @param context
     */
    public void unconnect(Context context) {
        Set<Map.Entry<String, ProcessConnection>> entries = mProcessConnections.entrySet();
        for (Map.Entry<String, ProcessConnection> entry : entries) {
            String serverPackageName = entry.getKey();
            ProcessConnection processConnection = entry.getValue();
            try {
                if (processConnection != null) {
                    context.unbindService(processConnection);
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            } finally {
                reset(serverPackageName);
            }
        }
    }

    /**
     * 指定包名定向解绑定
     * @param context
     * @param serverPackageName
     */
    public void unconnect(Context context, String serverPackageName) {
        if (null == serverPackageName || serverPackageName.isEmpty()) return;

        try {
            ProcessConnection processConnection = mProcessConnections.get(serverPackageName);
            if (processConnection != null) {
                context.unbindService(processConnection);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        } finally {
            reset(serverPackageName);
        }
    }

    /**
     * 解绑定后删除对应数据
     * @param serverPackageName
     */
    private void reset(String serverPackageName) {
        try {
            // 服务端解注册，避免解绑定后服务端还可以向客户端发送数据
            unregister(serverPackageName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mProcessInterfaces.remove(serverPackageName);
        mTempServiceConnectListeners.remove(serverPackageName); // OK?
        mServiceConnectListeners.remove(serverPackageName);
        isConnected.remove(serverPackageName);
        mTempProcessConnections.remove(serverPackageName);
        mProcessConnections.remove(serverPackageName);
    }

    /**
     * 向所有绑定的服务端发送数据
     * @param data
     * @throws RemoteException
     */
    public void sendToHost(String data) throws RemoteException {
        Collection<ProcessInterface> interfaces = mProcessInterfaces.values();
        for (ProcessInterface processInterface : interfaces) {
            if (processInterface != null) {
                processInterface.send(data);
            }
        }
    }

    /**
     * 向指定已绑定的服务端发送数据
     * @param serverPackageName
     * @param data
     * @throws RemoteException
     */
    public void sendToHost(String serverPackageName, String data) throws RemoteException {
        if (null == serverPackageName || serverPackageName.isEmpty()) return;

        if (mProcessInterfaces.containsKey(serverPackageName)) {
            ProcessInterface processInterface = mProcessInterfaces.get(serverPackageName);
            if (processInterface != null) {
                processInterface.send(data);
            }
        }
    }

    /**
     * 向指定已绑定的服务端注册回调
     * @param serverPackageName
     * @param callbackListener
     * @throws RemoteException
     */
    public void register(String serverPackageName, OnCallbackListener callbackListener) throws RemoteException {
        if (null == serverPackageName || serverPackageName.isEmpty()) return;

        if (mProcessInterfaces.containsKey(serverPackageName)) {
            ProcessInterface processInterface = mProcessInterfaces.get(serverPackageName);
            OnCallbackListener listener = mListeners.get(serverPackageName);
            // 未注册过listener才有效
            if (processInterface != null && listener == null) {
                processInterface.register(mClientPackageName, callbackListener);
                mListeners.put(serverPackageName, callbackListener);
            }
        }
    }

    /**
     * 取消注册已绑定的服务端回调
     * @param serverPackageName
     * @throws RemoteException
     */
    public void unregister(String serverPackageName) throws RemoteException {
        if (null == serverPackageName || serverPackageName.isEmpty()) return;

        if (mProcessInterfaces.containsKey(serverPackageName)) {
            ProcessInterface processInterface = mProcessInterfaces.get(serverPackageName);
            if (processInterface != null) {
                processInterface.unregister(mClientPackageName);
                mListeners.remove(serverPackageName);
            }
        }
    }

    private void bind(Context context, String serverPackageName, OnServiceConnectionListener listener) {
        if (context == null) {
            Log.d(TAG, "Context shoud not be null!");
            return;
        }
        Log.d(TAG, "bind ConnectService");
        // 保存Context和包名信息，重新绑定服务使用
        mAppContext = context.getApplicationContext();
        mClientPackageName = mAppContext.getPackageName();

        Intent intent;
        if (TextUtils.isEmpty(serverPackageName)) {
            intent = new Intent(context, ConnectService.class);
            if (listener != null) {
                mTempServiceConnectListeners.put(mClientPackageName, listener);
            }
        } else {
            // 多APP通信
            intent = new Intent();
            intent.setClassName(serverPackageName, ConnectService.class.getName());
            if (listener != null) {
                mTempServiceConnectListeners.put(serverPackageName, listener);
            }
        }
        ProcessConnection processConnection = new ProcessConnection();
        mTempProcessConnections.put(serverPackageName, processConnection);
        context.bindService(intent, processConnection, Context.BIND_AUTO_CREATE);
    }

    private class ProcessConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            String serverPackageName = name.getPackageName();
            ProcessInterface processInterface = ProcessInterface.Stub.asInterface(service);
            mProcessInterfaces.put(serverPackageName, processInterface);
            isConnected.put(serverPackageName, true);

            // 绑定成功，存储回调监听器
            if (mTempServiceConnectListeners.containsKey(serverPackageName)) {
                OnServiceConnectionListener connectionListener = mTempServiceConnectListeners.get(serverPackageName);
                if (connectionListener != null) {
                    mServiceConnectListeners.put(serverPackageName, connectionListener);
                    connectionListener.onServiceConnected(name);
                }
                // 删除对应临时存储
                mTempServiceConnectListeners.remove(serverPackageName);
            }

            // 绑定成功，存储连接器
            if (mTempProcessConnections.containsKey(serverPackageName)) {
                ProcessConnection processConnection = mTempProcessConnections.get(serverPackageName);
                if (processConnection != null) {
                    mProcessConnections.put(serverPackageName, processConnection);
                }
                // 删除对应临时存储
                mTempProcessConnections.remove(serverPackageName);
            }

            try {
                IBinder.DeathRecipient deathRecipient = new CustomDeathRecipient(serverPackageName);
                service.linkToDeath(deathRecipient, 0);
                OnCallbackListener callbackListener = mListeners.get(serverPackageName);
                if (callbackListener != null) {
                    register(serverPackageName, callbackListener); // Need?
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            String serverPackageName = name.getPackageName();
            isConnected.remove(serverPackageName);

            OnServiceConnectionListener connectionListener = mServiceConnectListeners.get(serverPackageName);
            if (connectionListener != null) {
                connectionListener.onServiceDisconnected(name);
            }
            // 断开连接 删除对应存储回调监听器
            mServiceConnectListeners.remove(serverPackageName);
            // 断开连接 删除对应存储连接器
            mProcessConnections.remove(serverPackageName);
        }
    }

    private class CustomDeathRecipient implements IBinder.DeathRecipient {

        private String serverPackageName;

        CustomDeathRecipient(String serverPackageName) {
            this.serverPackageName = serverPackageName;
        }

        @Override
        public void binderDied() {
            Log.d(TAG, "binderDied from [" + serverPackageName + "]");
            if (null == serverPackageName || serverPackageName.isEmpty()) return;

            if (mProcessInterfaces.containsKey(serverPackageName)) {
                ProcessInterface processInterface = mProcessInterfaces.get(serverPackageName);
                if (processInterface != null) {
                    processInterface.asBinder().unlinkToDeath(this, 0);
                    mProcessInterfaces.remove(serverPackageName);
                    OnServiceConnectionListener connectionListener = mServiceConnectListeners.get(serverPackageName);
                    // 重新绑定
                    connect(mAppContext, serverPackageName, connectionListener);
                }
            }
        }
    }

}
