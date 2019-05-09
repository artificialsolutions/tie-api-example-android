package tiechatsample.artisol.com.tiesdksampleasrchat;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import com.artificialsolutions.tiesdk.TieApiService;
import com.artificialsolutions.tiesdk.model.TieResponse;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;



public class ChatActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private EditText userInputBox;
    private ListView messagesView;
    private MessageAdapter messageAdapter;

    final String defaultClientColor="#2f286e";
    final String defaultEngineColor="#ff4c5b";

    final String baseUrl = "fill_in_base url_before_use";
    final String solutionEndpoint = "fill_in_endpoint_url_before_use";

    //ACTIVITY METHODS
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        userInputBox = findViewById(R.id.userInputBox);
        userInputBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                //Send textbox input to Teneo Engine if the keyboard's "Send" button is tapped.
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendInputToTiE(userInputBox.getText().toString(),null);
                    addMessageToChatWindow(userInputBox.getText().toString(),defaultClientColor, true);
                    userInputBox.setText("");
                }
                return false;
            }
        });


        messagesView = findViewById(R.id.messages_view);
        messageAdapter=new MessageAdapter(this);
        messagesView.setAdapter(messageAdapter);

        //Initialize Teneo Service
        TieApiService.getSharedInstance().setup(this.getApplicationContext(), baseUrl, solutionEndpoint);

        //CHECK FOR ANDROID TEXT TO SPEECH RESOURCES
        // Check to see if we have TTS voice data
        Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(ttsIntent, ACT_CHECK_TTS_DATA);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    //** (TTS) TEXT TO SPEECH METHODS
    TextToSpeech googleTextToSpeech = null;
    private final int ACT_CHECK_TTS_DATA = 1000;

    private void saySomething(String text) {
        if(text!=null) {
            googleTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    //INITIALIZE TTS
    public void onInit(int status) {
        //Now that the outcome of TTS.onInit is known, send an empty user input to trigger a greeting from Teneo Engine.
        sendInputToTiE("",null); //null=no params
        if (status == TextToSpeech.SUCCESS) {
            if (googleTextToSpeech != null) {
                int result = googleTextToSpeech.setLanguage(Locale.UK);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_LONG).show();
        }
    }

    //FREE TTS Resources when the app is minimized
    @Override
    protected void onDestroy() {
        if (googleTextToSpeech != null) {
            googleTextToSpeech.stop();
            googleTextToSpeech.shutdown();
        }
        super.onDestroy();
    }



    //*** ASR METHODS
    //Callback from Google Voice
    final int RESULT_SPEECH=123;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                consumeASRresults(resultCode, data);
                break;
            }
            case ACT_CHECK_TTS_DATA: { //CHECK ANDROID TTS, SUGGEST INSTALLATION IF NECESSARY
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    // Data exists, so we instantiate the TTS engine
                    googleTextToSpeech = new TextToSpeech(this, this);
                } else {
                    // Data is missing, so we start the TTS installation process
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                }
            }
        }
    }

    //Launches ASR, called from micButton's xml code
    public void launchGoogleVoice(View view){  //Launches ASR
        if(googleTextToSpeech!=null){ //Stop Text to speech, if active...
            googleTextToSpeech.stop();
        }

        //Wipe the input text field, hide the keyboard, and prepare an ASR Intent.
        userInputBox.setText("");
        hideSoftKeyboard(userInputBox);
        Intent asrIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        asrIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); //Specify language model that listens fluently
        asrIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_GB"); //Use a different language, other than the phone's default language
        asrIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1); //Asking for fewer results can improve performance
        asrIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName()); //Prevents interference when other apps are using ASR simultaneously
        asrIntent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Listening..."); //A label for the Android ASR popup

        try {
            //Launch Android ASR
            startActivityForResult(asrIntent, RESULT_SPEECH);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),"Sorry, Your device doesn’t support Speech to Text",Toast.LENGTH_SHORT).show();
        }
    }

    ///TAKES A STRING FROM ASR AND SENDS IT TO THE UI AND TENEO ENGINE
    private void consumeASRresults(int resultCode, Intent data){ //From ASR
        if (resultCode == RESULT_OK && null != data) {
            ArrayList<String> result = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //ADD MESSAGE TO CHAT UI
            addMessageToChatWindow(result.get(0),defaultClientColor,true);
            //MESSAGE TO TENEO ENGINE
            sendInputToTiE(result.get(0),null); //null=no params
        }
    }



    //*** TENEO ENGINE METHODS
    private void sendInputToTiE(String text, HashMap<String, String> params) {
        HashMap<String, String> parameters = new HashMap<>();
        if(params!=null){
            parameters=params;
        }

        try {
            TieApiService.getSharedInstance().sendInput(text, parameters)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<TieResponse>() {
                        @Override
                        public void onSuccess(TieResponse result) {
                            userInputBox.setText("");
                            consumeEngineResponseResult(result);
                        }

                        @Override
                        public void onError(Throwable e) {
                            consumeEngineError(e);
                        }
                    });
        }
        catch(IllegalArgumentException e){
            addMessageToChatWindow("ERROR: "+e.getMessage(),defaultEngineColor,false);
        }
    }

    //SENDS ENGINE RESULTS TO UI AND Text To Speech
    private void consumeEngineResponseResult(TieResponse result){

        String tieReply = result.getOutput().getText();
        //Display the result in the chat window
        addMessageToChatWindow(tieReply,defaultEngineColor,false);
        //Speak the result out loud with TTS
        saySomething(tieReply);
    }

    public void consumeEngineError(Throwable e){ //for both sendInput and closeSession
        addMessageToChatWindow(e.getMessage(),defaultEngineColor,false);
    }

    //ADD MESSAGE TO CHAT UI
    public void addMessageToChatWindow(String messageText, String color, boolean isUser){
        final Message message = new Message(messageText, new MemberData("", color), isUser);
        runOnUiThread(new Runnable() {
            @Override
            public void run() { //Always modify the UI from the Main Thread
                messageAdapter.add(message);
                // scroll the ListView to the last added element
                messagesView.setSelection(messagesView.getCount() - 1);
            }
        });
    }


    //Hides Android Keyboard
    private void hideSoftKeyboard(EditText t) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(t.getWindowToken(), 0);
        }
        t.clearFocus();
    }

}