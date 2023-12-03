package com.example.schoolcalenderchecker;


import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.wearable.complications.ComplicationText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;

import kotlin.coroutines.Continuation;


public class complicationDataSourceService extends SuspendingComplicationDataSourceService {

    public static PlainComplicationText title;
    public static PlainComplicationText text;
    public static long laatsteLesEindtijd = System.currentTimeMillis() / 1000;

    public static final String shared_prefs = "sharedPrefs";
    public static final String freeHourSwitchPref = "freeHourSwitchPref";


    boolean freeHourSwitchState;

    //ComplicationData
    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public ComplicationData getPreviewData(@NonNull ComplicationType complicationType) {
        PlainComplicationText previewTitle = new PlainComplicationText(ComplicationText.plainText("x"));
        PlainComplicationText previewText = new PlainComplicationText(ComplicationText.plainText("x"));

        return new ShortTextComplicationData.Builder(previewTitle, previewText)
                .setTitle(previewText)
                .build();
    }

    @Override
    public void onComplicationActivated(int complicationInstanceId, @NonNull ComplicationType type) {
        super.onComplicationActivated(complicationInstanceId, type);

    }

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public Object onComplicationRequest(@NonNull ComplicationRequest complicationRequest, @NonNull Continuation<? super ComplicationData> continuation) {

        long currentTimeSeconds = System.currentTimeMillis() / 1000;


        //save the time that the request was made.
        saveData(currentTimeSeconds);

        if (title == null || text == null) {
            title = new PlainComplicationText(ComplicationText.plainText("x"));
            text = new PlainComplicationText(ComplicationText.plainText("x"));

        }

        MainActivity.instance.makeRequest(new RequestCallback() {
            @Override
            public void requestComplete(String response) {

                boolean done = false;

                JSONObject les = MainActivity.instance.processData(response, true, false, true);
                JSONObject laatsteLes = MainActivity.instance.processData(response, false, false, true);

                if (les == null) {
                    title = new PlainComplicationText(ComplicationText.plainText("x"));
                    text = new PlainComplicationText(ComplicationText.plainText("x"));

                } else {
                    try {
                        if (les.getString("response") == "empty") {
                            title = new PlainComplicationText(ComplicationText.plainText("Done!"));
                            text = new PlainComplicationText(ComplicationText.plainText("٩(❛ᴗ❛)۶"));
                            done = true;

                        } else if (les.getString("response") == "conflict") {
                            title = new PlainComplicationText(ComplicationText.plainText("Conflict"));
                            text = new PlainComplicationText(ComplicationText.plainText("-"));
                        }



                        } catch (Exception e) {
                        try {
                            if (MainActivity.unixToTime(currentTimeSeconds).getDayOfYear() == MainActivity.unixToTime(les.getInt("start")).getDayOfYear()) { //Check if the lesson is today
                                try {
                                    getData(); //Used to get the freeHourSwitchState

                                    //If toggled, check if you have a free hour.
                                    if (freeHourSwitchState){
                                        if (les.getLong("start") - currentTimeSeconds >= 60*45) { //If there is atleast 45 minutes left before the next lesson...
                                            String displayStartTime = MainActivity.niceTime(MainActivity.unixToTime(les.getLong("start"))).get(0) + "";

                                            title = new PlainComplicationText(ComplicationText.plainText(les.getJSONArray("subjects").get(0).toString()));
                                            text = new PlainComplicationText(ComplicationText.plainText(displayStartTime));
                                        }
                                        else {
                                            title = new PlainComplicationText(ComplicationText.plainText(les.getJSONArray("subjects").get(0).toString()));
                                            text = new PlainComplicationText(ComplicationText.plainText(les.getJSONArray("locations").get(0).toString()));
                                        }

                                    } else {
                                        title = new PlainComplicationText(ComplicationText.plainText(les.getJSONArray("subjects").get(0).toString()));
                                        text = new PlainComplicationText(ComplicationText.plainText(les.getJSONArray("locations").get(0).toString()));
                                    }

                                } catch (JSONException error) {
                                    throw new RuntimeException(error);
                                }
                            } else { //If the lesson is not today, display done.
                                title = new PlainComplicationText(ComplicationText.plainText("Done!"));
                                text = new PlainComplicationText(ComplicationText.plainText("٩(❛ᴗ❛)۶"));
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }

                if (laatsteLes == null || done) {
                    laatsteLesEindtijd = currentTimeSeconds;
                } else {
                    try {
                        laatsteLesEindtijd = laatsteLes.getLong("end");

                    } catch (JSONException e) {

                    }
                }
            }
        });


        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,  FLAG_IMMUTABLE);



        switch (complicationRequest.getComplicationType()) {
            case RANGED_VALUE:

                //This is all for the progress bar.
                LocalDateTime currentTime = MainActivity.unixToTime(currentTimeSeconds);
                long dayStart = (currentTimeSeconds - ((long) currentTime.getHour() * 60 * 60 + currentTime.getMinute() * 60L + currentTime.getSecond())) + 30600; //In the and we add 8.5*60*60 = 30600 to make the startTime 8:30

                long secondsToday = currentTimeSeconds - dayStart;
                long secondsTodayTillEndLastLesson = laatsteLesEindtijd - dayStart;

                float progress = secondsToday / (float) secondsTodayTillEndLastLesson;
                float progressCapped = Math.max(Math.min(progress, 1), 0);

                return new RangedValueComplicationData.Builder(progressCapped, 0f, 1f, androidx.wear.watchface.complications.data.ComplicationText.EMPTY)
                        .setTitle(text)
                        .setText(title)
                        .setTapAction(pendingIntent)
                        .build();

            case SHORT_TEXT:

                return new ShortTextComplicationData.Builder(title, text)
                        .setTitle(text)
                        .setTapAction(pendingIntent)
                        .build();

            case LONG_TEXT:

                return new LongTextComplicationData.Builder(title, text)
                        .setTapAction(pendingIntent)
                        .build();
        }
        return null;
    }

    public void saveData(Long time) {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();


        editor.putLong("LastComplicationUpdateUnix", time);
        editor.apply();
    }

    //Getting the nessasary data.
    public void getData() {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, MODE_PRIVATE);

        freeHourSwitchState = sharedPreferences.getBoolean(freeHourSwitchPref, true);
    }
}
