/*
 * Copyright 2013 Thomas Hoffmann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.administrator.mypedometer01;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import com.example.administrator.mypedometer01.util.Util;
import com.example.administrator.mypedometer01.widget.MyIntentService;

/**
 * Background service which keeps the step-sensor listener alive to always get
 * the number of steps since boot.
 * <p/>
 * This service won't be needed any more if there is a way to read the
 * step-value without waiting for a sensor event
 */
public class SensorListenerService extends Service implements SensorEventListener {

    public static int CURRENT_SETP = 0;
    public static float SENSITIVITY = 10; // SENSITIVITY灵敏度
    private float mLastValues[] = new float[3 * 2];
    private float mScale[] = new float[2];
    private float mYOffset;
    private static long end = 0;
    private static long start = 0;
    /**
     * 最后加速度方向
     */
    private float mLastDirections[] = new float[3 * 2];
    private float mLastExtremes[][] = {new float[3 * 2], new float[3 * 2]};
    private float mLastDiff[] = new float[3 * 2];
    private int mLastMatch = -1;

    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;//一分钟的微秒值
    private final static long MICROSECONDS_IN_ONE_SECOND = 1000000;//一秒钟的微秒值
    private final static long SAVE_OFFSET_TIME = AlarmManager.INTERVAL_HALF_HOUR;
    private final static int SAVE_OFFSET_STEPS = 300;

    private static int steps;
    private static int lastSaveSteps;
    private static long lastSaveTime;

    /*广播，打开服务MyIntentService*/
    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.startService(new Intent(context, MyIntentService.class));
        }
    };

    /**
     * 传入上下文的构造函数
     */
    public SensorListenerService() {
        super();
        Log.e("sdfsdf", "SensorListenerService+++构造方法");
        int h = 480;
        mYOffset = h * 0.5f;
        mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        // nobody knows what happens here: step value might magically decrease
        // when this method is called...
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        Log.e("sdfsdf", "SensorListenerService+++onSensorChanged");

        if (event.values[0] > Integer.MAX_VALUE) {
            return;
        } else {
            steps = (int) event.values[0];
            Log.e("sdfsdf", "sun--222222222222----" + steps);
            updateIfNecessary();
        }
//
//        Sensor sensor = event.sensor;
//        synchronized (this) {
//            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//                Log.e("sdfsdf", "sun--1111111111111----" + steps);
//                float vSum = 0;
//                for (int i = 0; i < 3; i++) {
//                    Log.e("sdfsdf", "sun--222222222222----" + steps);
//                    final float v = mYOffset + event.values[i] * mScale[1];
//                    vSum += v;
//                }
//                int k = 0;
//                float v = vSum / 3;
//
//                float direction = (v > mLastValues[k] ? 1
//                        : (v < mLastValues[k] ? -1 : 0));
//                if (direction == -mLastDirections[k]) {
//                    Log.e("sdfsdf", "sun--333333333----" + steps);
//                    // Direction changed
//                    int extType = (direction > 0 ? 0 : 1); // minumum or
//                    // maximum?
//                    mLastExtremes[extType][k] = mLastValues[k];
//                    float diff = Math.abs(mLastExtremes[extType][k]
//                            - mLastExtremes[1 - extType][k]);
//
//                    if (diff > SENSITIVITY) {
//                        Log.e("sdfsdf", "sun--4444444444444----" + steps);
//                        boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
//                        boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
//                        boolean isNotContra = (mLastMatch != 1 - extType);
//
//                        if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough
//                                && isNotContra) {
//                            Log.e("sdfsdf", "sun--55555555555----" + steps);
//                            end = System.currentTimeMillis();
//                            if (end - start > 500) {// 此时判断为走了一步
//                                Log.e("sdfsdf", "sun--6666666666666----" + steps);
//                                steps++;
//                                mLastMatch = extType;
//                                start = end;
//                                Log.e("sdfsdf", "sun------" + steps);
//                                updateIfNecessary();
//                            }
//                        } else {
//                            mLastMatch = -1;
//                            Log.e("sdfsdf", "sun--777777777777----" + steps);
//                        }
//                    }
//                    mLastDiff[k] = diff;
//                }
//                mLastDirections[k] = direction;
//                mLastValues[k] = v;
//            }
//        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        Log.e("sdfsdf", "SensorListenerService开启+onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, MyIntentService.class));
        Log.e("sdfsdf", "SensorListenerService开启+onCreate");
        registerReveicerForTwo();
        reRegisterSensor();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.e("sdfsdf", "SensorListenerService开启+onStartCommand");
        setAsFormService();
        if (steps == 0) {
            Database db = Database.getInstance(this);
            steps = db.getCurrentSteps();
            db.close();
            updateIfNecessary();
        }
        // restart service every 6秒 to save the current step count
        ((AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        AlarmManager.INTERVAL_HALF_HOUR,
                        PendingIntent.getService(getApplicationContext(),
                                2, new Intent(this, SensorListenerService.class),
                                PendingIntent.FLAG_UPDATE_CURRENT));

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.e("sdfsdf", "SensorListenerService+++onTaskRemoved");
        // Restart service in 500 ms    0.5秒重启
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, System.currentTimeMillis() + 5000, PendingIntent
                        .getService(this, 3, new Intent(this, SensorListenerService.class), 0));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("sdfsdf", "SensorListenerService关闭");
        stopForeground(true);

        try {
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent();
        intent.setAction("com.sendsorlistener.service");
        getApplicationContext().sendBroadcast(intent);
    }


    /*注册广播*/
    private void registerReveicerForTwo() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.myintent.service");
        registerReceiver(myReceiver, filter);
    }

    /*更新数据*/
    private void updateIfNecessary() {
        if (steps > lastSaveSteps + SAVE_OFFSET_STEPS || (steps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME)) {
            Database db = Database.getInstance(this);
            if (db.getSteps(Util.getToday()) == Integer.MIN_VALUE) {
                //得到今天的日期为0时表示现在是零点，开始新的一天，储存今天的步数
                db.insertNewDay(Util.getToday(), steps);
            }
            //继续储存步数
            db.saveCurrentSteps(steps);
            db.close();
            lastSaveSteps = steps;
            lastSaveTime = System.currentTimeMillis();
        }
    }

    /*注册监听器Sensor*/
    private void reRegisterSensor() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        try {
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (BuildConfig.DEBUG) {
            if (sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size() < 1)
                return; // emulator
        }

        // enable batching with delay of max 5 second      启用最大延迟5秒的批处理
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL, (int) (5 * MICROSECONDS_IN_ONE_SECOND));
    }

    /*   启用前台服务，主要是startForeground()*/
    private void setAsFormService() {
        /*普通的消息推送*/
        Intent intent = new Intent(this, MainActivity.class);//点击跳转到Main22Activity
        // 创建一个PendingIntent，和Intent类似，不同的是由于不是马上调用，需要在下拉状态条出发的Activity，所以采用的是PendingIntent,即点击Notification跳转启动到哪个Activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        // 获取Notification对象
        Notification.Builder builder = new Notification.Builder(this);
        // builder.setContentInfo("补充内容");
        builder.setContentText(R.string.app_name + "正在运行");
        builder.setContentTitle(steps + "步");
        // 设置显示的小图标
        builder.setSmallIcon(R.drawable.app_logo);
        builder.setAutoCancel(true);//点击后消失
        // 获取当前系统时间
        builder.setWhen(System.currentTimeMillis());
        // 设置显示的信息
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        startForeground(1, notification);
    }

}
