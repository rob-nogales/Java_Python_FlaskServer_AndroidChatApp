package com.example.saurabh.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText username = (EditText) findViewById(R.id.txt_username);
        final EditText password = (EditText) findViewById(R.id.txt_password);
        final Button btnLogin = (Button) findViewById(R.id.btn_login);
        final CheckBox rememberMe = (CheckBox) findViewById(R.id.checkbox_remember_me);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String username_str = username.getText().toString();
                String password_str = password.getText().toString();

                Log.i("login", "Logging in as " + username_str);

                if(username_str.isEmpty() || password_str.isEmpty()) {
                    return;
                }

                (new LoginAsyncTask(username_str, password_str, rememberMe.isChecked())).execute();
            }
        });
    }

    class LoginAsyncTask extends AsyncTask<String, String, JSONObject> {

        private int user_id;
        private final String username;
        private final String password;
        private final boolean rememberMe;

        public LoginAsyncTask(String username, String password, boolean rememberMe) {
            this.username = username;
            this.password = password;
            this.rememberMe = rememberMe;
        }

        public void showServerError() {
            Toast.makeText(
                    getApplicationContext(),
                    "Unable to login; please try again later",
                    Toast.LENGTH_LONG
            ).show();
        }

        public void showInvalidAuthError() {
            Toast.makeText(
                    getApplicationContext(),
                    "Invalid username or password",
                    Toast.LENGTH_LONG
            ).show();
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            JSONObject input_json = new JSONObject();
            try {
                input_json.put("username", username);
                input_json.put("password", password);
            } catch(JSONException e) {
                e.printStackTrace();
                return null;
            }

            JSONParser jsonParser = new JSONParser();

            JSONObject output_json = jsonParser.getJSONFromUrl(
                    WelcomeActivity.url + "/login",
                    input_json
            );

            Log.i("login", "output_json");

            return output_json;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            boolean authenticated;
            if(json == null) {
                showServerError();
                return;
            }

            try {
                authenticated = json.getBoolean("authenticated");
            } catch (JSONException e) {
                e.printStackTrace();
                showServerError();
                return;
            }

            if(!authenticated) {
                LoginActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    showInvalidAuthError();
                    }
                });
                return;
            }

            String session;

            try {
                session = json.getString("session");
                user_id = json.getInt("user_id");
            } catch (JSONException e) {
                e.printStackTrace();
                showServerError();
                return;
            }

            if(rememberMe) {
                SharedPreferences sharedPreferences =
                        getSharedPreferences("user", Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("user_id", user_id)
                        .putString("username", username)
                        .putString("session", session).apply();
            }

            Intent menuIntent = new Intent(LoginActivity.this, MenuActivity.class);
            menuIntent.putExtra("username", username);
            menuIntent.putExtra("user_id", user_id);
            menuIntent.putExtra("session", session);
            startActivity(menuIntent);
        }
    }
}
