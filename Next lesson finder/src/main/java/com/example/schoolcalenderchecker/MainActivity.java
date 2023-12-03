package com.example.schoolcalenderchecker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends Activity {

    public static com.example.schoolcalenderchecker.MainActivity instance;

    TextView nextLessonText;
    TextView nextLessonLocText;
    TextView nextLessonStartTime;

    Button settingsButton;
    Button authenticateButton;
    Button requestButton;

    TextView versionTitle;


    public static final String shared_prefs = "sharedPrefs";
    public static final String delaySeekBarStatusPref = "delaySeekBarStatusPref";
    public static final String updatesSeekBarStatusPref = "updatesSeekBarStatusPref"; //TODO
    public static final String llnNummerPref = "llnNummerPref";
    public static final String access_tokenPref = "access_tokenPref";


    String llnNummer;
    int timeDelay;
    String access_token;

    long lastRequestTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nextLessonText = findViewById(R.id.nextLessonText);
        nextLessonLocText = findViewById(R.id.nextLessonLocText);
        nextLessonStartTime = findViewById(R.id.nextLessonStartTime);

        settingsButton = findViewById(R.id.settingsButton);
        authenticateButton = findViewById(R.id.authenticateButton);
        requestButton = findViewById(R.id.requestButton);

        versionTitle = findViewById(R.id.versionTitleAuthenticate);

        //set the version text
        String versionCode = BuildConfig.VERSION_NAME;
        String versionString = "v" + versionCode;
        versionTitle.setText(versionString);


        //OnClickListener settings
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //switch to the settings view
                Intent intentSettingsView = new Intent(MainActivity.this, activity_settings.class);
                startActivity(intentSettingsView);
            }
        });

        //OnClickListener authenticate
        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //switch to the authenticate view
                Intent intentAuthenticateView = new Intent(MainActivity.this, activity_authenticate.class);
                startActivity(intentAuthenticateView);
            }
        });

        //OnClickListener make request
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (System.currentTimeMillis()-lastRequestTime >= 5000) {

                    lastRequestTime = System.currentTimeMillis();
                    makeRequest(new RequestCallback() {
                        @Override
                        public void requestComplete(String response) {
                            JSONObject les = processData(response, true);
                            LocalDateTime date;

                            if (les == null) {
                                nextLessonText.setText("No lesson was found.");
                                nextLessonLocText.setText("x");
                                nextLessonStartTime.setText("x");

                            } else {
                                try {
                                    if (les.getString("response") == "empty") {
                                        nextLessonText.setText("Done");
                                        nextLessonLocText.setText("You are done for today!");
                                        nextLessonStartTime.setText("");

                                    } else if (les.getString("response") == "conflict") {
                                        date = unixToTime(les.getInt("start"));
                                        List<String> niceTime = niceTime(date);
                                        nextLessonText.setText("Conflicting lessons");
                                        nextLessonLocText.setText("Please use the app");
                                        nextLessonStartTime.setText(niceTime.get(1) + ": " + niceTime.get(0));

                                    } else {
                                        date = unixToTime(les.getInt("start"));
                                        List<String> niceTime = niceTime(date);
                                        nextLessonText.setText(les.getJSONArray("subjects").get(0).toString());
                                        nextLessonLocText.setText(les.getJSONArray("locations").get(0).toString());
                                        nextLessonStartTime.setText(niceTime.get(1) + ": " + niceTime.get(0));
                                    }

                                } catch (JSONException e) {

                                }
                            }
                        }
                    }, true);
                }
            }
        });

        //Make a request when we start the app
        getData();

        if (access_token != null || access_token != "") {
            makeRequest(new RequestCallback() {
                @Override
                public void requestComplete(String response) {

                    JSONObject les = processData(response, true);
                    LocalDateTime date = null;

                    if (les == null) {
                        nextLessonText.setText("No lesson was found.");
                        nextLessonLocText.setText("x");
                        nextLessonStartTime.setText("x");
                    } else {

                        try {
                            if (les.getString("response") == "empty") {
                                nextLessonText.setText("Done");
                                nextLessonLocText.setText("You are done for today!");
                                nextLessonStartTime.setText("");

                            } else if (les.getString("response") == "conflict") {
                                date = unixToTime(les.getLong("start"));
                                List<String> niceTime = niceTime(date);
                                nextLessonText.setText("Conflicting lessons");
                                nextLessonLocText.setText("Please use the app");
                                nextLessonStartTime.setText(niceTime.get(1) + ": " + niceTime.get(0));
                            }

                        } catch (JSONException e) {
                            try {
                                date = unixToTime(les.getInt("start"));
                                List<String> niceTime = niceTime(date);
                                nextLessonText.setText(les.getJSONArray("subjects").get(0).toString());
                                nextLessonLocText.setText(les.getJSONArray("locations").get(0).toString());
                                nextLessonStartTime.setText(niceTime.get(1) + ": " + niceTime.get(0));

                            } catch (JSONException ex) {

                            }
                        }
                    }
                }
            });
        }
    }

    //Zermelo api stuff. (actually getting the next lesson)
    public void makeRequest(RequestCallback callback) {
        makeRequest(callback, false);
    }

    @SuppressLint("NewApi")
    public void makeRequest(RequestCallback callback, boolean doPopups) {

        //Get the necessary data from other activities.
        getData();

        //Get the current week and year, format it to the right format (for example: 202310, 2023 -> year and 10 -> week)
        LocalDateTime currentDate = LocalDateTime.now();

        int currentWeekInt = currentDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR);

        if (currentDate.getDayOfWeek().toString() == "SUNDAY") {
            currentWeekInt -=1; //we do this because the school-week starts on monday, not sunday.

        }

        String currentYear = Integer.toString(currentDate.getYear());
        String currentWeekString = Integer.toString(currentWeekInt);
        String week = currentYear + currentWeekString;

        String student = llnNummer;



        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "https://griftland.zportal.nl/api/v3/liveschedule?access_token=" + access_token + "&student=" + student + "&week=" + week;

        //check if there is an access_key.
        if (access_token == null || access_token == "") {
            if (doPopups) showToastMessage("No access_key was found\nPlease try again");

            return;
        }

        //Notify the user that the api request is being made.
        if (doPopups) showToastMessage("Calling zermelo API.\nThis might take a few seconds.");


        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (doPopups) showToastMessage("Api request successful.");
                        callback.requestComplete(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (doPopups) showToastMessage("The api request failed.\nCheck your lln nummer.");
            }

        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }

    public JSONObject processData(String data, boolean isFirst) {
        return processData(data, isFirst, false, false);
    }

    public JSONObject processData(String data, boolean isFirst, boolean doPopups, boolean isComplication) {
        JSONObject dataJSON;
        JSONArray lessen;
        try {
            dataJSON = new JSONObject(data);

        } catch (JSONException error) {
            if (doPopups) showToastMessage("Error while parsing data.\nPlease try again.");

            return null;

        }

        try {
            lessen = ((JSONObject) (dataJSON.getJSONObject("response").getJSONArray("data").get(0))).getJSONArray("appointments");
        } catch (JSONException error) {
            if (doPopups) showToastMessage("Error.\nPlease try again.");

            return null;
        }
        //Check of er lessen zijn in de week. (vakantie)
        if (lessen.length() == 0) {
            try {
                return new JSONObject().put("response", "empty");
            } catch (JSONException e) {

            }
        }

        try {
            ArrayList lessonTypes = new ArrayList<String>();
            lessonTypes.add("lesson");
            lessonTypes.add("exam");
            lessonTypes.add("activity");
            lessonTypes.add("talk");
            lessonTypes.add("other");
            lessonTypes.add("interlude");
            lessonTypes.add("meeting");
            lessonTypes.add("conflict");


            long time = (System.currentTimeMillis() / 1000) - (timeDelay * 60L);
            if (isComplication) {
                time += 5*60; //The request made has the data for the update in 5 minutes. So this code suck (shutup its good nuff)
            }

            JSONObject les = null;
            long bestDiff = Long.MAX_VALUE;

            JSONObject laatsteLes = null;
            long highscore = Long.MIN_VALUE;

            for (int i = 0; i < lessen.length(); i++) {
                JSONObject comparison = (JSONObject) lessen.get(i);
                long start = comparison.getLong("start");

                long diff = start - time;
                if (diff > 0 && !comparison.getBoolean("cancelled") && lessonTypes.contains(comparison.getString("appointmentType"))) { //Geen lessen in het verleden, de les is niet uitgevallen het is iets van een les, geen stempeluur.
                    if (diff < bestDiff) { //Les is dichter bij start tijd
                        les = comparison;
                        bestDiff = diff;
                    }

                    if (unixToTime(start).getDayOfYear() == unixToTime(System.currentTimeMillis() / 1000).getDayOfYear()) { //Check if the lesson is today.
                        if (start > highscore) {
                            highscore = start;
                            laatsteLes = comparison;
                        }
                    }
                }
            }

            //Check if it's the last day of school.
            if (les == null) {
                return new JSONObject().put("response", "empty");
            }

            //Check if the day has ended
            else if (unixToTime(les.getLong("start")).getDayOfYear() != unixToTime(System.currentTimeMillis() / 1000).getDayOfYear()) {
                return new JSONObject().put("response", "empty");
            }
            //Check if there are 2 lessons conflicting each other.
            else if (les.getString("appointmentType").toLowerCase().contains("conflict")) {
                return new JSONObject().put("response", "conflict").put("start", les.getLong("start"));
            }


            return isFirst ? les : laatsteLes;


        } catch (JSONException e) {
            if (doPopups) showToastMessage("Error while working with data.\nPlease try again.");
            return null;
        }
    }


    //make the time look nice.
    public static List<String> niceTime(LocalDateTime date) {

        HashMap<String, String> abbreviations = new HashMap<>();
        abbreviations.put(String.valueOf(DayOfWeek.MONDAY), "ma");
        abbreviations.put(String.valueOf(DayOfWeek.TUESDAY), "di");
        abbreviations.put(String.valueOf(DayOfWeek.WEDNESDAY), "wo");
        abbreviations.put(String.valueOf(DayOfWeek.THURSDAY), "do");
        abbreviations.put(String.valueOf(DayOfWeek.FRIDAY), "vr");


        int hour = date.getHour();
        int minute = date.getMinute();
        DayOfWeek day = date.getDayOfWeek();

        String niceHour = checkForMissingZero(hour);
        String niceMinute = checkForMissingZero(minute);
        String niceDay = abbreviations.get(day.toString());

        String resultTime = niceHour + ":" + niceMinute;

        List<String> resultList = new ArrayList<>();
        resultList.add(resultTime);
        resultList.add(niceDay);

        return resultList;
    }

    public static String checkForMissingZero(int x) {
        String result = "";
        if (x < 10) {
            String hourString = Integer.toString(x);
            result += "0" + hourString;
        } else {
            result += x;
        }
        return result;
    }


    //useful stuff
    public static LocalDateTime unixToTime(long unix) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unix), TimeZone.getDefault().toZoneId());
    }


    public void showToastMessage(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }


    //Getting the nessasary data.
    public void getData() {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, MODE_PRIVATE);

        timeDelay = sharedPreferences.getInt(delaySeekBarStatusPref, 10);
        llnNummer = sharedPreferences.getString(llnNummerPref, "");
        access_token = sharedPreferences.getString(access_tokenPref, "");

    }

}