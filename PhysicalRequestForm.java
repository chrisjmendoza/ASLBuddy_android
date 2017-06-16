package xyz.softdev.aslbuddy;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class PhysicalRequestForm extends AppCompatActivity {

    public static final String MyPREFERENCES = "MyPrefs";
    public final static String REQUEST_TYPE = "xyz.softdev.aslbuddy.REQUEST_TYPE";
    static final int TIME_DIALOG_ID = 1;
    static final int DATE_DIALOG_ID = 0;
    private static TextView mDateDisplay;
    private static TextView mTimeDisplay;
    private static int mYear;
    private static int mMonth;
    private static int mDay;
    private static int mhour;
    private static int mminute;
    SharedPreferences sharedpreferences;
    private View mPhysicalRequestView;
    private View mProgressView;
    EditText nameForm;
    private EditText addressForm;
    private EditText cityForm;
    private EditText stateForm;
    private EditText zipForm;
    private EditText phoneForm;

    private String mRequestType;
    private int userId;
    private String userEmail;

    static JSONObject json;

    private static boolean mIsError = false;

    private static void setError(boolean error) { mIsError = error;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_physical_request_form);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        mRequestType = intent.getStringExtra(REQUEST_TYPE);
        userId = getIntent().getIntExtra("userId", userId);
        userEmail = getIntent().getStringExtra("userEmail");

        nameForm = (EditText) findViewById(R.id.request_form_username);
        addressForm = (EditText) findViewById(R.id.request_form_address);
        cityForm = (EditText) findViewById(R.id.request_form_address_city);
        stateForm = (EditText) findViewById(R.id.request_form_address_state);
        zipForm = (EditText) findViewById(R.id.request_form_zipcode);
        phoneForm = (EditText) findViewById(R.id.request_form_phoneNumber);

        mDateDisplay = (TextView) findViewById(R.id.request_form_date);
        mDateDisplay.setTextSize(getResources().getDimension(R.dimen.textsize));
        mTimeDisplay = (TextView) findViewById(R.id.request_form_time);
        mTimeDisplay.setTextSize(getResources().getDimension(R.dimen.textsize));

        Button submit = (Button) findViewById(R.id.request_form_submit_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    onSubmit();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });

        mPhysicalRequestView = findViewById(R.id.physReq);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void onSubmit() throws ParseException {

        // Reset errors
        nameForm.setError(null);
        addressForm.setError(null);
        cityForm.setError(null);
        stateForm.setError(null);
        zipForm.setError(null);
        phoneForm.setError(null);

        String name = nameForm.getText().toString();
        String address = addressForm.getText().toString();
        String city = cityForm.getText().toString();
        String state = stateForm.getText().toString();
        String zip = zipForm.getText().toString();
        String phone = phoneForm.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check to see if all fields have been filled
        if (TextUtils.isEmpty(name)) {
            nameForm.setError("This field is required.");
            focusView = nameForm;
            cancel = true;
        }

        if (TextUtils.isEmpty(address)) {
            addressForm.setError("This field is required.");
            focusView = addressForm;
            cancel = true;
        }

        if (TextUtils.isEmpty(city)) {
            cityForm.setError("This field is required.");
            focusView = cityForm;
            cancel = true;
        }

        if (TextUtils.isEmpty(state)) {
            stateForm.setError("This field is required.");
            focusView = stateForm;
            cancel = true;
        }

        if (TextUtils.isEmpty(zip)) {
            zipForm.setError("This field is required.");
            focusView = zipForm;
            cancel = true;
        } else {
            if(!checkZip(zip)) {
                zipForm.setError("Please enter a valid zip code");
                focusView = zipForm;
                cancel = true;
            }
        }

        if (TextUtils.isEmpty(phone)) {
            phoneForm.setError("This field is required.");
            focusView = phoneForm;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt submission and focus the first
            // form field with an error
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the physical request information upload
            //showProgress(true);

        }

        String date = mDateDisplay.getText().toString();
        String time = mTimeDisplay.getText().toString();
        String timeDate = date + " " + time + ":00";
//        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String dateTime = fmt.format(timeDate);

        try {
            json = new JSONObject();
            json.put("userId", userId);
            json.put("username", name);
            json.put("address", address);
            json.put("city", city);
            json.put("state", state);
            json.put("zip", zip);
            json.put("phone", phone);
            json.put("dateTime", timeDate);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        AsyncServerTask ast = new AsyncServerTask();
        ast.execute();

    }

    private class AsyncServerTask extends AsyncTask<Void, Void, String> {

        final String SERVER_BASE_URL = "https://softdev.xyz/aslbuddy/php/lib/phys_req.php";

        @Override
        protected String doInBackground(Void... params) {

            BufferedReader reader;

            try {
                // Set up connection and accompanying parameters
                URL url = new URL(SERVER_BASE_URL);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.connect();

                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(json.toString());
                wr.flush();
                wr.close();

                InputStream stream = urlConnection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                    Log.d("Response: ", "> " + line); // Here you'll get whole response
                }
                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {
            result = result.replace("\n", "").replace("\r", "");
            if(result.equals("Successful")) {
                Toast.makeText(getApplicationContext(), "Request Sent", Toast.LENGTH_LONG).show();
                Intent i = new Intent(PhysicalRequestForm.this, HohMenu.class);
                i.putExtra("userId", userId);
                PhysicalRequestForm.this.startActivity(i);
            } else {
                Toast.makeText(getApplicationContext(), "Error. Please check your input. ", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Zip code Regex validator. Allows for 5 digit zip or 9 digit zip code
     * @param zip The zip code to validate
     * @return Zip code passes regex check
     */
    private boolean checkZip(String zip) {
        String regex = "^[0-9]{5}(?:-[0-9]{4})?$";
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(zip);
        return matcher.matches();
    }

    public void showTimePickerdialog(View v) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void showDatePickerdialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

//    /**
//     * Shows the progress UI and hides the physical request form
//     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
//    private void showProgress(final boolean show) {
//        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
//        // for very easy animations. If available, use these APIs to fade-in
//        // the progress spinner.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
//            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
//
//            mPhysicalRequestView.setVisibility(show ? View.GONE : View.VISIBLE);
//            mPhysicalRequestView.animate().setDuration(shortAnimTime).alpha(
//                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    mPhysicalRequestView.setVisibility(show ? View.GONE : View.VISIBLE);
//                }
//            });
//
//            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
//            mProgressView.animate().setDuration(shortAnimTime).alpha(
//                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
//                }
//            });
//        } else {
//            // The ViewPropertyAnimator APIs are not available, so simply show
//            // and hide the relevant UI components.
//            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
//            mPhysicalRequestView.setVisibility(show ? View.GONE : View.VISIBLE);
//        }
//    }

    public static class TimePickerFragment extends DialogFragment implements
            TimePickerDialog.OnTimeSetListener {

        private static String pad(int c) {
            if (c >= 10)
                return String.valueOf(c);
            else
                return "0" + String.valueOf(c);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            mhour = c.get(Calendar.HOUR_OF_DAY);
            mminute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, mhour, mminute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the chosen time here
            mTimeDisplay.setText(new StringBuilder()
                    .append(pad(hourOfDay)).append(":")
                    .append(pad(minute)));
        }
    }

    public static class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, mYear, mMonth, mDay);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            // Do something with the date chosen here
            String pattern = "yyyy-MM-dd";

            String date = year + "-" + zeroCheck(month) + month + "-" + zeroCheck(day) + day;

            mDateDisplay.setText(date);
//            mDateDisplay.setText(new StringBuilder()
//                    // Month is 0 based so add 1
//                    .append(day).append("/")
//                    .append(month + 1).append("/")
//                    .append(year).append(" "));
        }

        public String zeroCheck(int num) {
            return num < 10 ? "0" : "";
        }

    }



}
