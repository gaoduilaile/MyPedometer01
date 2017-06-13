package com.example.administrator.mypedometer01;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.example.administrator.mypedometer01.util.Logger;
import com.example.administrator.mypedometer01.util.Util;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    final static int DEFAULT_GOAL = 10000;
    /*步伐大小 0.82英尺或者25cm*/
    final static float DEFAULT_STEP_SIZE = Locale.getDefault() == Locale.US ? 0.82f : 25f;
    /*步伐单位  英尺或者厘米*/
    final static String DEFAULT_STEP_UNIT = Locale.getDefault() == Locale.US ? "ft" : "cm";

    private TextView stepsView, totalView, averageView;
    private int todayOffset, total_start, goal, since_boot, total_days;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
    private boolean showSteps = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_overview);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(MainActivity.this, SensorListener.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startService(i);
            }
        });


        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        initView();

    }


    private void initView() {
        /*今天的步数*/
        stepsView = (TextView) findViewById(R.id.steps);
        /*总步数*/
        totalView = (TextView) findViewById(R.id.total);
        /*平均每日步数*/
        averageView = (TextView) findViewById(R.id.average);

    }

    @Override
    public void onResume() {
        super.onResume();
        Database db = Database.getInstance(this);
        if (BuildConfig.DEBUG) db.logState();
        // read todays offset
        todayOffset = db.getSteps(Util.getToday());

        SharedPreferences prefs = this.getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        since_boot = db.getCurrentSteps(); // do not use the value from the sharedPreferences
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        // register a sensorlistener to live update the UI if a step is taken
        if (!prefs.contains("pauseCount")) {
            SensorManager sm = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (sensor == null) {
                new AlertDialog.Builder(this).setTitle(R.string.no_sensor)
                        .setMessage(R.string.no_sensor_explain)
//                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
//                            @Override
//                            public void onDismiss(final DialogInterface dialogInterface) {
//                                finish();
//                            }
//                        })
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).create().show();
            } else {
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
            }
        }

        since_boot -= pauseDifference;
        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();

        db.close();

        stepsDistanceChanged();
    }

    /**
     * Call this method if the Fragment should update the "steps"/"km" text in
     * the pie graph as well as the pie and the bars graphs.
     */
    private void stepsDistanceChanged() {
        if (showSteps) {
            ((TextView) findViewById(R.id.unit)).setText(getString(R.string.steps));
        } else {
            String unit = this.getSharedPreferences("pedometer", Context.MODE_PRIVATE).getString("stepsize_unit", DEFAULT_STEP_UNIT);
            if (unit.equals("cm")) {
                unit = "km";
            } else {
                unit = "mi";
            }
            ((TextView) findViewById(R.id.unit)).setText(unit);
        }

        updatePie();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            SensorManager sm =
                    (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Database db = Database.getInstance(this);
        db.saveCurrentSteps(since_boot);
        db.close();
    }


    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        // won't happen
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (BuildConfig.DEBUG)
            Logger.log("UI - sensorChanged | todayOffset: " + todayOffset + "since_boot：" + event.values[0]);
        if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
            return;
        }
        if (todayOffset == Integer.MIN_VALUE) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = -(int) event.values[0];
            Database db = Database.getInstance(this);
            db.insertNewDay(Util.getToday(), (int) event.values[0]);
            db.close();
        }
        since_boot = (int) event.values[0];
        updatePie();
    }

    /**
     * Updates the pie graph to show todays steps/distance as well as the
     * yesterday and total values. Should be called when switching from step
     * count to distance.
     */
    private void updatePie() {
        if (BuildConfig.DEBUG) Logger.log("UI - update steps: " + since_boot);
        // todayOffset might still be Integer.MIN_VALUE on first start
        int steps_today = Math.max(todayOffset + since_boot, 0);

        if (showSteps) {
            stepsView.setText(formatter.format(steps_today));
            totalView.setText(formatter.format(total_start + steps_today));
            averageView.setText(formatter.format((total_start + steps_today) / total_days));
        } else {
            // update only every 10 steps when displaying distance
            SharedPreferences prefs = getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            float stepsize = prefs.getFloat("stepsize_value", DEFAULT_STEP_SIZE);
            float distance_today = steps_today * stepsize;
            float distance_total = (total_start + steps_today) * stepsize;
            if (prefs.getString("stepsize_unit", DEFAULT_STEP_UNIT)
                    .equals("cm")) {
                distance_today /= 100000;
                distance_total /= 100000;
            } else {
                distance_today /= 5280;
                distance_total /= 5280;
            }
            stepsView.setText(formatter.format(distance_today));
            totalView.setText(formatter.format(distance_total));
            averageView.setText(formatter.format(distance_total / total_days));
        }
    }
}
