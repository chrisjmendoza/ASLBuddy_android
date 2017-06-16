package xyz.softdev.aslbuddy;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatTool extends AppCompatActivity implements TextToSpeech.OnInitListener {

    //Logged in userId
    private int userId;

    // layout reference
    private LinearLayout layout;

    // button references
    private FloatingActionButton textSpeechButton;
    private Button typingButton;

    // EditText reference for composing message
    private EditText textMessage;

    // TextView references for all sent messages
    private List<TextView> sentMessages;
    private ArrayList<String> sentStringMessages;

    // Other conversation logic variables stored here
    private String currentMessage;
    private boolean isTryingToGetResponse;

    // Text to Speech and Speech to Text
    private int MY_TTS_DATA_CHECK_CODE = 42;
    private int REQ_CODE_SPEECH_INPUT = 43;
    private TextToSpeech ASLBuddyTTS;

    // Chat Labels
    private static final String OTHER_LABEL = "Other";
    private static final String USER_LABEL = "Me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_tool);
        createScrollView();

        // instantiate text-to-speech
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_TTS_DATA_CHECK_CODE);

        //handle the buttons
        textSpeechButton = (FloatingActionButton) findViewById(R.id.textSpeechButton);
        textSpeechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });

        typingButton = (Button) findViewById(R.id.typing_button);
        typingButton.setOnClickListener(onTypeClick());

        // instantiate the sent messages list
        sentMessages = new ArrayList<>();
        if(sentMessages == null) {
            sentStringMessages = new ArrayList<>();
        }
//            // if messages loaded from saved instance state
//            boolean isOther = false;
//            for (int i = 0; i < sentStringMessages.size(); i++) {
//                final TextView curMessage = createNewTextView(sentStringMessages.get(i), isOther);
//                layout.addView(curMessage);
//                curMessage.startAnimation(AnimationUtils.loadAnimation(ChatTool.this,
//                        android.R.anim.slide_in_left));
//                isOther = !isOther;
//            }
//        }
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
    }

    /**
     * This onClickListener method is triggered when the Type/Send button is clicked
     * @return OnClickListener
     */
    private View.OnClickListener onTypeClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // depending on the button text, we have different behavior
                String buttonText = typingButton.getText().toString();
                if (buttonText.equals("Type")) {
                    layout.addView(createNewEditText());
                    textSpeechButton.setVisibility(View.INVISIBLE);
                    typingButton.setText("Send");

                } else if (buttonText.equals("Send")) {
                    // get rid of the keyboard
                    InputMethodManager imm = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textMessage.getWindowToken(), 0);

                    String message = textMessage.getText().toString();

                    final TextView userMessage = createNewTextView(message, false);
                    layout.addView(userMessage);
                    userMessage.startAnimation(AnimationUtils.loadAnimation(ChatTool.this,
                            android.R.anim.slide_in_left));

                    textSpeechButton.setVisibility(View.VISIBLE);
                    typingButton.setText("Type");

                    // remove the edit view when the user sends a message
                    layout.removeView(textMessage);

                    // We can notify the user that ASL Buddy is waiting for a response
                    currentMessage = message;
                    isTryingToGetResponse = true;

                    // print for debugging the current message
                    System.out.println("Current message: " + currentMessage);

                    // Speak the current message via the speakers
                    if (ASLBuddyTTS != null) {
                        ASLBuddyTTS.speak(currentMessage, TextToSpeech.QUEUE_ADD, null);
                    }
                }
            }
        };
    }

    /**
     * Creates new EditText when you click "Type" to compose a message
     * Sets focus to EditText and brings up keyboard
     * @return EditText
     */
    private EditText createNewEditText() {
        final LinearLayout.LayoutParams params;

        // create new EditText
        textMessage = new EditText(this);
        params = getTextViewParams(false, true);
        textMessage.setLayoutParams(params);
        textMessage.setHint("Type text here!");
        textMessage.requestFocus();
        textMessage.setHintTextColor(ContextCompat.getColor(this, R.color.hearing_tool_text));
        textMessage.setTextColor(ContextCompat.getColor(this, R.color.hearing_tool_text));

        // bring up keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        return textMessage;
    }

    /**
     * Creates new TextView when you click "Send" to send a message
     * @param message to place in TextView
     * @param isOtherPerson determines left or right alignment
     * @return message TextView
     */
    private TextView createNewTextView(String message, boolean isOtherPerson) {
        final LinearLayout.LayoutParams params;

        // place current message
        TextView sentMessage = new TextView(this);
        params = getTextViewParams(isOtherPerson, false);
        sentMessage.setLayoutParams(params);
        sentMessage.setTextColor(ContextCompat.getColor(this, R.color.hearing_tool_text));
        sentMessage.setBackgroundColor(ContextCompat.getColor(this, R.color.hearing_tool_messageBackground));
        sentMessage.setPadding(10, 10, 10, 10);

        // set the correct message label
        if (isOtherPerson) {
            sentMessage.setText(OTHER_LABEL + ":    " + message);
        } else {
            sentMessage.setText(USER_LABEL + ":    " + message);
        }
        sentStringMessages.add(message);
        sentMessages.add(sentMessage);

        return sentMessage;
    }

    /**
     * Helper function for createNewTextView
     * @param isOtherPerson determines left or right alignment
     * @return LayoutParams to attach to a TextView
     */
    private LinearLayout.LayoutParams getTextViewParams(boolean isOtherPerson, boolean isEditText) {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // add xml layout rules to this new EditText
        if (isOtherPerson || isEditText) {
            params.gravity = Gravity.LEFT;
        } else {
            params.gravity = Gravity.RIGHT;
        }

        if (isEditText) {
            params.width = layout.getWidth() - typingButton.getWidth() - 10;
            params.gravity = Gravity.BOTTOM;
        }
        return params;
    }


    /**
     * Show google speech input dialog, handler for the record button
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk!");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Speech to Text is not supported on your device!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the activity results for Speech to Text and Text to Speech
     * */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MY_TTS_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                //the device has the necessary data - create the TTS
                ASLBuddyTTS = new TextToSpeech(this, this);
            }
            else {
                //no data - install it now
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        } else
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {

            ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String message = text.get(0);

            final TextView userMessage = createNewTextView(message, true);
            layout.addView(userMessage);
            userMessage.startAnimation(AnimationUtils.loadAnimation(ChatTool.this,
                    android.R.anim.slide_in_left));

            // update global variable to keeep track of whose talking
            currentMessage = message;
            isTryingToGetResponse = true;

            // print for debugging the current message
            System.out.println("Current message: " + currentMessage);
        }
    }

    /**
     * Programmatically creates a scrollview for the messages to populate,
     * and adds it to the existing RelativeLayout from "content_voice_chat.xml"
     */
    private void createScrollView() {
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.chat_tool_layout);
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT));
        layout = new LinearLayout(this);
        layout.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        sv.addView(layout);
        rl.addView(sv);
    }

    /**
     * Install Text to Speech data when the application initializes.
     * Checks to see if Text to speech is available on this device.
     * */
    public void onInit(int initStatus) {
        if (initStatus == TextToSpeech.SUCCESS &&
                ASLBuddyTTS.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE) {
            ASLBuddyTTS.setLanguage(Locale.US);

        } else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(this, "Your Device does not support Text to Speech",
                    Toast.LENGTH_LONG).show();
        }
    }
}
