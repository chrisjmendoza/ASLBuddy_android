package xyz.softdev.aslbuddy;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class SkypeCall extends Activity {

    TextView interpreterFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skype_call);

        interpreterFound = (TextView) findViewById(R.id.label_interpreter_found);

        //TODO: Remove back button once system back button is working
        Button backButton = (Button) findViewById(R.id.button_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent i = new Intent(SkypeCall.this, HohMenu.class);
                SkypeCall.this.startActivity(i);
            }
        });

        setupVideoRequest();

        Button callButton = (Button) findViewById(R.id.callButton);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(interpreterFound.getText().equals("Finding Interpreter"))) {
                    String interpreter = "echo123";
                    SkypeResources.initiateSkypeCall(getApplicationContext(), interpreter);
                    // String interpreter = interpreterFound.getText().toString();
                    //initiate a call by grabbing the username from the TextView after it is updated
                    //this should only be available once the AsyncTask has completed and made the button visible
                    // SkypeResources.initiateSkypeCall(getApplicationContext(), interpreter);

//                    try {
//                        SkypeApi skypeApi = new SkypeApi(getApplicationContext());
//
//                        skypeApi.startConversation("echo123", Modality.AudioCall);
//                    } catch (SkypeSdkException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    Toast.makeText(getApplicationContext(), "Please select another Interpreter", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Call this method to set up the UI for pending video request
     */
    private void setupVideoRequest() {
        final Button nextInterpreterButton = (Button) findViewById(R.id.label_skip_user);

        // Can't skip until a skype interpreter is returned
        nextInterpreterButton.setClickable(false);

        //TODO: Make this button set current user ok_to_chat to false before switching to new user
        nextInterpreterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AsyncServerTask ast = new AsyncServerTask();
                ast.execute();
            }
        });
    }

    private class AsyncServerTask extends AsyncTask<Void, Void, String> {

        final String SERVER_BASE_URL = "https://softdev.xyz/aslbuddy/php/lib/skypeResources.php";

        @Override
        protected String doInBackground(Void... params) {

            HttpsURLConnection urlConnection = null;
            BufferedReader reader = null;
            URL url;

            try {
                // Set up the connection and accompanying parameters
                url = new URL(SERVER_BASE_URL);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.connect();

                InputStream stream = urlConnection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                    Log.d("Response: ", "> " + line); // The whole response stored here
                }

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                try {
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            TextView interpreterFound = (TextView) findViewById(R.id.label_request_pending);
            result = result.replace("\n", "").replace("\r", "");
            interpreterFound.setText(result);
        }
    }
}
