// ProcessInterface.aidl
package com.paul.song.connector;

// Declare any non-default types here with import statements
import com.paul.song.connector.ICallback;

interface ProcessInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void send(String data);

    void register(String clientPackageName, in ICallback listener);

//    void unregister(in ICallback listener);

    void unregister(String clientPackageName);
}
