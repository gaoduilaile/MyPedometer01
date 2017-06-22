package com.example.administrator.mypedometer01.widget;

import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.administrator.mypedometer01.SensorListenerService;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyIntentService extends Service {
    private BroadcastReceiver myReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            context.startService(new Intent(context, SensorListenerService.class));
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("sdfsdf", "SensorListenerService开启+onCreate");
        registerReveicerForOne();
    }

    private void registerReveicerForOne() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.sendsorlistener.service");
        registerReceiver(myReceiver, filter);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.e("sdfsdf", "SensorListenerService开启+onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("sdfsdf", "SensorListenerService关闭");
        Intent intent = new Intent();
        intent.setAction("com.myintent.service");
        getApplicationContext().sendBroadcast(intent);
    }


}
