package com.example.schoolcalenderchecker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;


public class activity_authenticate extends Activity {

    TextView versionTitleAuthenticate;
    TextView authenticateStatusText;

    EditText inputNumberLlnNummer;
    EditText inputTextZermeloCode;

    Button loginButton;
    Button logoutButton;
    String access_token;


    public String llnNummer;

    //saving stuff
    public static final String shared_prefs = "sharedPrefs";
    public static final String access_tokenPref = "access_tokenPref";
    public static final String llnNummerPref = "llnNummerPref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticate);

        versionTitleAuthenticate = findViewById(R.id.versionTitleAuthenticate);
        authenticateStatusText = findViewById(R.id.authenticateStatusText);
        inputTextZermeloCode = findViewById(R.id.inputTextZermeloCode);

        loginButton = findViewById(R.id.loginButton);
        logoutButton = findViewById(R.id.logoutButton);


        inputNumberLlnNummer = findViewById(R.id.inputNumberLlnNummer);

        //set the version text
        String versionCode = BuildConfig.VERSION_NAME;
        String versionString = "v" + versionCode;
        versionTitleAuthenticate.setText(versionString);



        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                authenticateZermelo();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });

        inputNumberLlnNummer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                llnNummer = String.valueOf(inputNumberLlnNummer.getText());
                saveData();
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
        updateStatus();
    }


    //zermelo authentication stuff
    public void authenticateZermelo() {
        String zermeloCode = (inputTextZermeloCode.getText()).toString().replaceAll("\\s", "");

        //Check if the given code is the correct length\
        if (zermeloCode.length() == 0) {
            showToastMessage("Please provide your zermelo code");
            return;
        } else if (zermeloCode.length() != 12) {
            showToastMessage("Invalid code.\nPlease try again.");
            return;
        }


        //Make an api request to get the access_token
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://griftland.zportal.nl/api/v3/oauth/token?grant_type=authorization_code&code=" + zermeloCode;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        processAuthenticateData(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showToastMessage("API failed. \nPlease try again.\n(don't use the same code twice)");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }

    public void processAuthenticateData(String data) {
        JSONObject dataJSON;


        try {
            dataJSON = new JSONObject(data);

        } catch (JSONException error) {
            showToastMessage("Parsing error.\nPlease try again.");
            return;

        }

        try {
            //Call the logout function before requesting a new access_token
            logout();
            access_token = dataJSON.getString("access_token");
            showToastMessage("Success!");
            inputTextZermeloCode.setText(null);
            updateStatus();
            saveData();

        } catch (JSONException error) {
            showToastMessage("Unknown error.\nPlease try again.");
        }
    }

    public void logout() {
        if (access_token == null || access_token == "") {
            return;
        }

        //Make an api request to remove the token from the database.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://griftland.zportal.nl/api/v3/oauth/logout?access_token=" + access_token;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        showToastMessage("Logged out!");
                        access_token = "";
                        saveData();

                        updateStatus();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showToastMessage("Error while logging out.\nPlease try again");
                logoutButton.setText("Error");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }


    //update the status text.
    public void updateStatus() {
        if (access_token == null || access_token == "") {
            authenticateStatusText.setText("Not logged in.");
            inputTextZermeloCode.setInputType(InputType.TYPE_CLASS_NUMBER);
            loginButton.setEnabled(true);

        } else {
            authenticateStatusText.setText("logged in!");
            inputTextZermeloCode.setInputType(InputType.TYPE_NULL);
            loginButton.setEnabled(false);
        }
    }


    //Stuff for saving
    public void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(access_tokenPref, access_token);
        editor.putString(llnNummerPref, inputNumberLlnNummer.getText().toString());

        editor.apply();
    }

    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(shared_prefs, Context.MODE_PRIVATE);

        access_token = sharedPreferences.getString(access_tokenPref, "");
        llnNummer = sharedPreferences.getString(llnNummerPref, "");
    }

    public void updateViews() {
        inputNumberLlnNummer.setText(llnNummer);
    }


    //other useful stuff
    public void showToastMessage(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}