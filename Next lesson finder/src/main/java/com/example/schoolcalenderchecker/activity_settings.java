package com.example.schoolcalenderchecker;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;


public class activity_settings extends Activity {


    TextView lastComplicationUpdateValue;

    SeekBar delaySeekBar;
    TextView delaySeekBarStatusText;

    Switch freeHourSwitch;

    EditText inputZermeloPortal;

    Button deleteAuthButton;
    TextView versionTitleSettings;


    public static int delaySeekBarStatus;
    public static boolean freeHourSwitchState;
    public static String access_token;

    public static String zportal_name;

    //saving stuff
    public static final String shared_prefs = "sharedPrefs";
    public static final String delaySeekBarStatusPref = "delaySeekBarStatusPref";
    public static final String freeHourSwitchPref = "freeHourSwitchPref";
    public static final String access_tokenPref = "access_tokenPref";

    public static final String zportal_namePref = "zportal_namePref";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        lastComplicationUpdateValue = findViewById(R.id.lastComplicationUpdateValue);
        delaySeekBar = findViewById(R.id.delaySeekBar);
        delaySeekBarStatusText = findViewById(R.id.delaySeekBarStatusText);

        freeHourSwitch = findViewById(R.id.freeHourSwitch);

        inputZermeloPortal = findViewById(R.id.inputZermeloPortal);

        deleteAuthButton = findViewById(R.id.deleteAuthButton);
        versionTitleSettings = findViewById(R.id.versionTitleSettings);


        //set the version text
        String versionCode = BuildConfig.VERSION_NAME;
        String versionString = "v" + versionCode;
        versionTitleSettings.setText(versionString);

        //set the last update value
        if (getLastComplicationUpdateTime() == 0) {
            lastComplicationUpdateValue.setText("No complication found");
        } else {
            List<String> lastUpdateTime = niceTime(unixToTime(getLastComplicationUpdateTime()));
            lastComplicationUpdateValue.setText(lastUpdateTime.get(0) + ", " + lastUpdateTime.get(1));
        }


        //OnChange listeners for both the seekbars
        delaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int listenerDelayValue, boolean b) {
                delaySeekBarStatus = listenerDelayValue;
                delaySeekBarStatusText.setText(listenerDelayValue + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveData();
            }
        });


        freeHourSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                freeHourSwitchState = b;
                saveData();
            }
        });


        deleteAuthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteAuthData();
                showToastMessage("Deleted authentication token, please sign in again.");
            }
        });

        inputZermeloPortal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                zportal_name = String.valueOf(inputZermeloPortal.getText());
                saveData();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        try {
            loadData();
            updateViews();
        } catch (Error e) {

            //toast message
            String text = "Something went wrong while getting preferences.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(this, text, duration);
            toast.show();
        }

    }


    public void deleteAuthData() {
        access_token = "";
        saveData();
    }
    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(delaySeekBarStatusPref, delaySeekBarStatus);
        editor.putBoolean(freeHourSwitchPref, freeHourSwitchState);
        editor.putString(access_tokenPref, access_token);
        editor.putString(zportal_namePref, zportal_name);


        editor.apply();
    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, MODE_PRIVATE);
        delaySeekBarStatus = sharedPreferences.getInt(delaySeekBarStatusPref, 10);
        freeHourSwitchState = sharedPreferences.getBoolean(freeHourSwitchPref, true);
        zportal_name = sharedPreferences.getString(zportal_namePref, "griftland");

    }

    public void updateViews() {
        delaySeekBar.setProgress(delaySeekBarStatus);
        freeHourSwitch.setChecked(freeHourSwitchState);
        inputZermeloPortal.setText(zportal_name);
    }

    public long getLastComplicationUpdateTime() {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, MODE_PRIVATE);

        return sharedPreferences.getLong("LastComplicationUpdateUnix", 0);
    }


    //make the time look nice.
    public List<String> niceTime(LocalDateTime date) {


        HashMap<String, String> abbreviations = new HashMap<>();
        abbreviations.put("MONDAY", "ma");
        abbreviations.put("TUESDAY", "di");
        abbreviations.put("WEDNESDAY", "wo");
        abbreviations.put("THURSDAY", "do");
        abbreviations.put("FRIDAY", "vr");
        abbreviations.put("SATURDAY", "za");
        abbreviations.put("SUNDAY", "zo");


        int hour = date.getHour();
        int minute = date.getMinute();
        DayOfWeek day = date.getDayOfWeek();

        String niceHour = checkForMissingZero(hour);
        String niceMinute = checkForMissingZero(minute);
        String niceDay = abbreviations.get(day.name());

        String resultTime = niceHour + ":" + niceMinute;

        List<String> resultList = new ArrayList<>();
        resultList.add(resultTime);
        resultList.add(niceDay);

        return resultList;
    }

    public String checkForMissingZero(int x) {
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
}