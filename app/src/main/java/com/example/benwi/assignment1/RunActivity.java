package com.example.benwi.assignment1;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class RunActivity extends AppCompatActivity {
    private Location startLocation, endLocation;
    private Handler customHandler = new Handler();

    private TextView timerValue;
    private TextView distanceValue;
    private TextView calorieValue;

    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private long startTime = 0L;

    private float distanceMiles, distanceSwapBuff, updatedDistance;
    private float calories;

    public static ArrayList<String> runHistory = new ArrayList<>();
    public static ArrayAdapter<String> adapter;
    public static ListView runList;

    public static String runSelection;
    public static long runTime;
    public static float runDistance;
    public static float runCalories;
    public static int runPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle("Run Tracker");

        initializeRuns();

        timerValue = (TextView) findViewById(R.id.timerValue);
        distanceValue = (TextView) findViewById(R.id.distanceValue);
        calorieValue = (TextView) findViewById(R.id.calorieValue);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.appInfo:
                startActivity(new Intent(RunActivity.this, InfoActivity.class));
        }
        return false;
    }

    private void initializeRuns(){
        runHistory.clear();
        try {
            runHistory = (ArrayList<String>) ObjectSerializer.deserialize(MainActivity.sharedPreferences.getString(MainActivity.profile.name + "History",
                    ObjectSerializer.serialize(new ArrayList<String>())));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (runList == null){
            runList = (ListView) findViewById(R.id.runList);
        }
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, runHistory);
        setListAdapter(adapter);

        runList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id){
                runPosition = position;
                runSelection = (String) adapter.getItemAtPosition(position);
                runTime = MainActivity.sharedPreferences.getLong(MainActivity.profile.name + runSelection + "time", 0);
                runDistance = MainActivity.sharedPreferences.getFloat(MainActivity.profile.name + runSelection + "distance", 0);
                runCalories = MainActivity.sharedPreferences.getFloat(MainActivity.profile.name + runSelection + "calories", 0);

                Log.i("Selection", runSelection);
                Log.i("Time", Long.toString(runTime));
                Log.i("Distance", Float.toString(runDistance));
                Log.i("Calories", Float.toString(runCalories));

                startActivity(new Intent(RunActivity.this, HistoryActivity.class));
            }
        });
    }

    private ListView getListView(){
        if (runList == null){
            runList = (ListView) findViewById(R.id.usersList);
        }
        return runList;
    }

    private void setListAdapter(ListAdapter adapter){
        getListView().setAdapter(adapter);
    }

    public void startRun(View view){
        startTime = SystemClock.uptimeMillis();
        startLocation = MainActivity.currentLocation;
        endLocation = MainActivity.currentLocation;
        timeSwapBuff += timeInMilliseconds;
        customHandler.postDelayed(updateTimerThread, 0);
        customHandler.postDelayed(updateDistanceThread, 0);
        customHandler.postDelayed(updateCaloriesThread, 0);

    }

    public void endRun(View view){
        timeSwapBuff += timeInMilliseconds;
        distanceSwapBuff += distanceMiles;
        endLocation = MainActivity.currentLocation;
        customHandler.removeCallbacks(updateTimerThread);
        customHandler.removeCallbacks(updateDistanceThread);
        customHandler.removeCallbacks(updateCaloriesThread);
    }

    public void saveRun(View view){
        int currentRun = runHistory.size() + 1;
        String runName = "Run " + currentRun;
        runHistory.add(runName);
        try {
            MainActivity.sharedPreferences.edit().putString(MainActivity.profile.name + "History", ObjectSerializer.serialize(runHistory)).apply();
            MainActivity.sharedPreferences.edit().putLong(MainActivity.profile.name + runName + "time", updatedTime).apply();
            MainActivity.sharedPreferences.edit().putFloat(MainActivity.profile.name + runName + "distance", distanceMiles).apply();
            MainActivity.sharedPreferences.edit().putFloat(MainActivity.profile.name + runName + "calories", calories).apply();
        } catch (IOException e){
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();
    }

    private Runnable updateTimerThread = new Runnable(){
        public void run(){
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            if (mins < 10) {
                timerValue.setText("0" + mins + ":" + String.format("%02d", secs) + ":"
                        + String.format("%02d", milliseconds));
            }
            else {
                timerValue.setText("" + mins + ":" + String.format("%02d", secs) + ":"
                        + String.format("%02d", milliseconds));
            }
            customHandler.postDelayed(this, 0);
        }
    };

    private Runnable updateDistanceThread = new Runnable(){
        public void run(){
            endLocation = MainActivity.currentLocation;
            distanceMiles = startLocation.distanceTo(endLocation) * (float) 0.000621371;
            updatedDistance = distanceMiles + distanceSwapBuff;
            //distanceMiles += distanceSwapBuff;

            distanceValue.setText("" + String.format("%.2f", updatedDistance) + " miles");
            customHandler.postDelayed(this, 0);
        }
    };

    private Runnable updateCaloriesThread = new Runnable() {
        @Override
        public void run() {
            calories = (float) 0.72 * MainActivity.profile.weight * updatedDistance;

            if (calories > 1) {
                calorieValue.setText("" + String.format("%.2f", calories));
            }
            customHandler.postDelayed(this, 0);
        }
    };
}
