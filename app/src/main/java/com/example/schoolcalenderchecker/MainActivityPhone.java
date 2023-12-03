package com.example.schoolcalenderchecker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class MainActivityPhone extends AppCompatActivity {


    TextView statusText;
    SeekBar delaySeekBar;
    TextView seekBarStatus;
    Switch statusToggle;
    TextView nextLesson;
    Button authenticateButton;
    EditText inputTextZermeloCode;
    Button getLessonButton;
    Button logoutButton;
    EditText inputTextLeerlingNummer;
    TextView workerStatus;


    AlertDialog.Builder builder;

    WorkRequest updateWorkRequest;

    //Stuff for preference
    int delaySeekBarState;
    boolean statusToggleState;
    String access_token;

    public static final String shared_prefs = "sharedPrefs";
    public static final String seekBarStatusPref = "seekBarStatusPref";
    public static final String statusTogglePref = "statusTogglePref";
    public static final String access_tokenPref = "access_tokenPref";
    public static final String inputTextLeerlingNummerPref = "inputTextLeerlingNummerPref";





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        statusText = findViewById(R.id.statusText);
        delaySeekBar = findViewById(R.id.seekBarDelay);
        seekBarStatus = findViewById(R.id.seekBarStatus);
        statusToggle = findViewById(R.id.statusToggle);
        nextLesson = findViewById(R.id.nextLesson);
        authenticateButton = findViewById(R.id.authenticateButton);
        inputTextZermeloCode = findViewById(R.id.inputTextZermeloCode);
        getLessonButton = findViewById(R.id.getLessonButton);
        logoutButton = findViewById(R.id.logoutButton);
        inputTextLeerlingNummer = findViewById(R.id.inputTextLeerlingNummer);
        workerStatus = findViewById(R.id.workerStatus);


        //OnChangeListener for the seekbar.
        delaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ //SeekbarChangeListener
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarStatus.setText(String.valueOf(progress));
                delaySeekBarState = progress;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveData();
            }
        });

        //OnChangeListener for the on/off switch
        statusToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { //CheckboxChangeListener
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                statusToggleState = isChecked;

                if (isChecked) {
                    //Execute code when the switch is checked.

                    setBackgroundUpdates();



                } else {
                    //Execute code when the switch is unchecked.
                    cancelBackgroundUpdates();
                }

                saveData();
            }
        });

        //Here we create the WorkRequest (initialized somewhere else)
        updateWorkRequest =
                new PeriodicWorkRequest.Builder(UpdateWorker.class,
                        15,
                        TimeUnit.MINUTES)
                        .addTag("TAG_UPDATE_DATA")
                        .build();


        //Import saved preferences (if available)
        try {
            loadData();
            updateViews();
        } catch (Error e){
            Log.d("SharedPreferences", "Error: Could not get shared preferences. More info: " + e);
        }

        //For ease of use, we make a request when starting the app
        makeRequest();
    }



    //useful stuff
    Date unixToTime(int unix) {
        Date date = new Date ();
        date.setTime((long)unix*1000);
        return date;
    }

    //Zermelo api stuff. (actually getting the next lesson)
    @SuppressLint("NewApi")
    public void makeRequest() {

        //Get the current week and year, format it to the right format (for example: 202310, 2023 -> year and 10 -> week)
        LocalDateTime currentDate = LocalDateTime.now();

        int currentWeekInt = currentDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

        if (currentDate.getDayOfWeek().toString() == "SATURDAY" || currentDate.getDayOfWeek().toString() == "SUNDAY") {
            currentWeekInt += 1;
        }

        String currentYear = Integer.toString(currentDate.getYear());
        String currentWeekString = Integer.toString(currentWeekInt);
        String week = currentYear + currentWeekString;


        String student = inputTextLeerlingNummer.getText().toString();


        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://griftland.zportal.nl/api/v3/liveschedule?access_token="+access_token+"&student="+student+"&week="+week;

        //check if there is an access_key.
        if (access_token == null || access_token == "") {
            statusText.setText("No access_key was found");
            return;
        }

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        statusText.setText("Api request successful.");

                        processData(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                statusText.setText("The api request failed.");
            }

        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);


    }

    public void onClickRequest(View view) {
        makeRequest();
    }

    public void processData(String data) {
        JSONObject dataJSON;
        JSONArray lessen;


        try {
            dataJSON = new JSONObject(data);
            statusText.setText("parsing successful");

        } catch (JSONException error) {
            statusText.setText("error while parsing data.");
            return;

        }

        try {
            lessen = ((JSONObject)(dataJSON.getJSONObject("response").getJSONArray("data").get(0))).getJSONArray("appointments");
        } catch (JSONException error) {
            statusText.setText("error while working with the data.");
            return;
        }

        try {
            long time = System.currentTimeMillis()/1000 - (delaySeekBarState*60);

            JSONObject les = null;
            long bestDiff = Long.MAX_VALUE;

            //Hierin kijken we naar de dichtbijzijnste les (deze word opgeslagen in les)
            ArrayList lessonTypes = new ArrayList<>();
                lessonTypes.add("lesson");
                lessonTypes.add("exam");
                lessonTypes.add("activity");
                lessonTypes.add("talk");
                lessonTypes.add("other");
                lessonTypes.add("interlude");
                lessonTypes.add("meeting");

            for(int i = 0; i < lessen.length(); i++) {
                JSONObject comparison = (JSONObject) lessen.get(i);
                long diff = (comparison.getLong("start") - time);
                if(diff > 0 && lessonTypes.contains(comparison.getString("appointmentType").toLowerCase()) && !comparison.getBoolean("cancelled")) { //Geen lessen in het verleden, het is een les, de les is niet uitgevallen
                    if(diff < bestDiff) { //Les is dichter bij start tijd
                        les = comparison;
                        bestDiff = diff;
                    }
                }
            }

            //comparison.getString("appointmentType").equalsIgnoreCase("lesson")|| comparison.getString("appointmentType").equalsIgnoreCase("exam")

            if (les == null){
                nextLesson.setText("No lesson was found.");
                return;
            }
            Date date = unixToTime(les.getInt("start"));
            nextLesson.setText(les.getJSONArray("subjects").get(0).toString() + ", in " + les.getJSONArray("locations").get(0).toString() + "\n" + date);

        } catch (JSONException e) {
            statusText.setText("error while working with the data.");
            return;
        }



    }


    //These are for saving data.
    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean(statusTogglePref, statusToggle.isChecked());
        editor.putInt(seekBarStatusPref, delaySeekBarState);
        editor.putString(access_tokenPref, access_token);
        editor.putString(inputTextLeerlingNummerPref, inputTextLeerlingNummer.getText().toString());

        editor.apply();
        Log.d("SharedPreferences", "Preferences have been saved.");
    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, MODE_PRIVATE);

        statusToggleState = sharedPreferences.getBoolean(statusTogglePref, false);
        delaySeekBarState = sharedPreferences.getInt(seekBarStatusPref, 0);
        access_token = sharedPreferences.getString(access_tokenPref, "");
        inputTextLeerlingNummer.setText(sharedPreferences.getString(inputTextLeerlingNummerPref, ""));

    }

    public void updateViews() {
        statusToggle.setChecked(statusToggleState);
        delaySeekBar.setProgress(delaySeekBarState, true);
    }


    //WorkManager (make the app run in the background).
    public void setBackgroundUpdates() {
        WorkManager
                .getInstance(this)
                .enqueue(updateWorkRequest);

        WorkManager workManager = WorkManager.getInstance(this);

        Log.d("WorkManager", "UpdateWorker initialized.");
    }

    public void cancelBackgroundUpdates() {
        WorkManager
                .getInstance(this)
                .cancelAllWorkByTag("TAG_UPDATE_DATA");
        Log.d("WorkManager", "UpdateWorker stopped.");
    }


}

