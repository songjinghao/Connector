package com.paul.song.connector;

import android.content.ComponentName;

public interface OnServiceConnectionListener {

    void onServiceConnected(ComponentName name);

    void onServiceDisconnected(ComponentName name);
}
