package com.example.vmac.WatBot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import android.os.Handler;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.assistant.v2.model.DialogNodeOutputOptionsElement;
import com.ibm.watson.assistant.v2.model.RuntimeResponseGeneric;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.assistant.v2.Assistant;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.SessionResponse;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.text_to_speech.v1.model.SynthesizeOptions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private RecyclerView recyclerView;
  private ChatAdapter mAdapter;
  private ArrayList messageArrayList;
  private EditText inputMessage;
  private Button btnSend;
  private Button btnRecord;
  StreamPlayer streamPlayer = new StreamPlayer();
  private boolean initialRequest;
  private boolean permissionToRecordAccepted = false;
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
  private static String TAG = "MainActivity";
  private static final int RECORD_REQUEST_CODE = 101;
  private boolean listening = false;
  private MicrophoneInputStream capture;
  private Context mContext;
  private MicrophoneHelper microphoneHelper;

  private Assistant watsonAssistant;
  private Response<SessionResponse> watsonAssistantSession;
  private SpeechToText speechService;
  private TextToSpeech textToSpeech;

  private String DueDate;

  public  SharedPreferences sharedpreferences;

  private boolean toggle = false;

    // mic animation
  private Button button;
  private ImageView imgAnimation1;
  private ImageView imgAnimation2;

    // mic animation Handler
    private Handler handlerAnimation = new Handler();
    private Boolean statusAnimation = false;


    // Initiating API Service Objects
    private void createServices() {
    watsonAssistant = new Assistant("2019-02-28", new IamAuthenticator(mContext.getString(R.string.assistant_apikey)));
    watsonAssistant.setServiceUrl(mContext.getString(R.string.assistant_url));

    textToSpeech = new TextToSpeech(new IamAuthenticator((mContext.getString(R.string.TTS_apikey))));
    textToSpeech.setServiceUrl(mContext.getString(R.string.TTS_url));

    speechService = new SpeechToText(new IamAuthenticator(mContext.getString(R.string.STT_apikey)));
    speechService.setServiceUrl(mContext.getString(R.string.STT_url));
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

      imgAnimation1 = findViewById(R.id.imgAnimation1);
      imgAnimation2 = findViewById(R.id.imgAnimation2);

      sharedpreferences = getSharedPreferences("userData", Context.MODE_PRIVATE);
      String UserEmail = sharedpreferences.getString("Email", null);
      String DueD = sharedpreferences.getString("Date", null);
      if (UserEmail != null && DueD != null )
      {
      }else{
          Intent i = new Intent(this,ActivityHome.class);
          startActivity(i);
      }

      mContext = getApplicationContext();

    inputMessage = findViewById(R.id.message);
    btnSend = findViewById(R.id.btn_send);
    btnRecord = findViewById(R.id.btn_record);
    String customFont = "Montserrat-Regular.ttf";
    Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
    inputMessage.setTypeface(typeface);
    recyclerView = findViewById(R.id.recycler_view);

    messageArrayList = new ArrayList<>();
    mAdapter = new ChatAdapter(messageArrayList);
    microphoneHelper = new MicrophoneHelper(this);

    LinearLayoutManager layoutManager;
    layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(mAdapter);

    this.inputMessage.setText("");
    this.initialRequest = true;

    int permission = ContextCompat.checkSelfPermission(this,
      Manifest.permission.RECORD_AUDIO);

    if (permission != PackageManager.PERMISSION_GRANTED) {
      Log.i(TAG, "Permission to record denied");
      makeRequest();
    } else {
      Log.i(TAG, "Permission to record was already granted");
    }


        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                Message audioMessage = (Message) messageArrayList.get(position);
                if (audioMessage != null && !audioMessage.getMessage().isEmpty()) {

                        new SayTask().execute(audioMessage.getMessage());

                }
            }
            @Override
            public void onLongClick(View view, int position) {
                recordMessage();

            }
        }));

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInternetConnection()) {
                    sendMessage();
                }
            }
        });


      btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rcrdBTNAnim();
            }
        });

        createServices();
        sendMessage();
    };

  public void rcrdBTNAnim(){
      if (statusAnimation) {
          stopPulse();
          btnRecord.setBackgroundResource(R.mipmap.mic);
          listening = true;
          recordMessage();
      }
      else {
          startPulse();
          btnRecord.setBackgroundResource(R.mipmap.mic_2);
          recordMessage();
      }
      statusAnimation =! statusAnimation;
  }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0,     spanString.length(), 0); //fix the color to white
            item.setTitle(spanString);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { switch(item.getItemId()) {
        case R.id.refresh:
            streamPlayer.interrupt();
            this.recreate();
            break;
        case R.id.logout:
            streamPlayer.interrupt();
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.clear().commit();
            String UserEmail = sharedpreferences.getString("Email", null);
            String DueD = sharedpreferences.getString("Date", null);
            if (UserEmail != null && DueD != null )
            {
            }else{
                MainActivity.this.finish();
                Intent i = new Intent(this,ActivityHome.class);
                startActivity(i);
            }
            break;

    }
        return(super.onOptionsItemSelected(item));
    }


    // Speech-to-Text Record Audio permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case RECORD_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
                return;
            }

            case MicrophoneHelper.REQUEST_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                MicrophoneHelper.REQUEST_PERMISSION);
    }


    public static boolean containsWords(String inputString) {
        boolean isItTrue = false;

        String[] VariablesListArray = new String[]{
                "duedate", "due date",
                "my duedate", "my due date",
                "delivery","delivery date",
                "my delivery","my delivery date",
                "tell me duedate", "tell me due date",
                "when is duedate", "when is due date",
                "when is delivery date", "when is delivery date",
                "when is delivery", "when is delivery",
                "remember me duedate", "remember me duedate"
        };

        List<String> list = Arrays.asList(VariablesListArray);

        if(list.contains(inputString)){
            isItTrue = true;
        }
        return  isItTrue;
    }

    Boolean dueDate = false;

    // Sending a message to Watson Assistant Service
    private void sendMessage() {
        if(statusAnimation){
            rcrdBTNAnim();
        }

        final String inputmessage = this.inputMessage.getText().toString().trim();
        if (!this.initialRequest) {
            Message inputMessage = new Message();
            if(this.containsWords(inputmessage)){

                inputMessage.setMessage("remember my duedate");
                inputMessage.setId("1");
                messageArrayList.add(inputMessage);
                this.dueDate = true;

            }else{
                if(inputmessage.length() > 0){
                    inputMessage.setMessage(inputmessage);
                    inputMessage.setId("1");
                    messageArrayList.add(inputMessage);
                    this.dueDate = false;
                }else{
                    Toast.makeText(getApplicationContext(), "No Input", Toast.LENGTH_SHORT).show();

                }
            }

        } else {
            Message inputMessage = new Message();

            if(this.containsWords(inputmessage)){
                this.dueDate = true;
                this.initialRequest = false;

            }else{
                inputMessage.setMessage(inputmessage);
                inputMessage.setId("100");
                this.dueDate = false;
                this.initialRequest = false;
            }
            Toast.makeText(getApplicationContext(), "Tap on the message for Voice", Toast.LENGTH_LONG).show();
        }

        this.inputMessage.setText("");
        mAdapter.notifyDataSetChanged();

        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    if (watsonAssistantSession == null) {
                        ServiceCall<SessionResponse> call = watsonAssistant.createSession(new CreateSessionOptions.Builder().assistantId(mContext.getString(R.string.assistant_id)).build());
                        watsonAssistantSession = call.execute();
                    }

                    if(initialRequest == false && toggle == false  ){
                        toggle = true;

                        MessageInput input = new MessageInput.Builder()
                                .text(inputmessage)
                                .build();
                        MessageOptions options = new MessageOptions.Builder()
                                .assistantId(mContext.getString(R.string.assistant_id))
                                .input(input)
                                .sessionId(watsonAssistantSession.getResult().getSessionId())
                                .build();
                        Response<MessageResponse> response = watsonAssistant.message(options).execute();
                        Log.i(TAG, "run: " + response.getResult());
                        if (response != null &&
                                response.getResult().getOutput() != null &&
                                !response.getResult().getOutput().getGeneric().isEmpty()) {

                            List<RuntimeResponseGeneric> responses = response.getResult().getOutput().getGeneric();

                            for (RuntimeResponseGeneric r : responses) {
                                Message outMessage;
                                switch (r.responseType()) {
                                    case "text":
                                        if(!dueDate){
                                            outMessage = new Message();
                                            outMessage.setMessage(r.text());
                                            outMessage.setId("2");
                                            messageArrayList.add(outMessage);
                                            new SayTask().execute(outMessage.getMessage());

                                        }else{

                                            DueDate = sharedpreferences.getString("Date", null);
                                            outMessage = new Message();
                                            if(!dueDate){
                                                outMessage.setMessage(r.text());
                                                outMessage.setId("2");
                                                messageArrayList.add(outMessage);
                                                new SayTask().execute(outMessage.getMessage());
                                            }else{
                                                outMessage.setMessage("Your due date is " + DueDate);
                                                outMessage.setId("2");
                                                messageArrayList.add(outMessage);
                                                new SayTask().execute(outMessage.getMessage());
                                            }

                                        }

                                        break;

                                    case "option":
                                        outMessage =new Message();
                                        String title = r.title();
                                        String OptionsOutput = "";
                                        for (int i = 0; i < r.options().size(); i++) {
                                            DialogNodeOutputOptionsElement option = r.options().get(i);
                                            OptionsOutput = OptionsOutput + option.getLabel() +"\n";

                                        }
                                        outMessage.setMessage(title + "\n" + OptionsOutput);
                                        outMessage.setId("2");

                                        messageArrayList.add(outMessage);

                                        // speak the message
                                        new SayTask().execute(outMessage.getMessage());
                                        break;

                                    case "image":
                                        outMessage = new Message(r);
                                        messageArrayList.add(outMessage);

                                        // speak the description
                                        new SayTask().execute(outMessage.getTitle() + outMessage.getDescription());
                                        break;
                                    default:
                                        Log.e("Error", "Unhandled message type");
                                }
                            }

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    mAdapter.notifyDataSetChanged();
                                    if (mAdapter.getItemCount() > 1) {
                                        recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);

                                    }

                                }
                            });
                        }

                    }else if(initialRequest == false && inputmessage.length() == 0 && toggle == true){
                        Toast.makeText(getApplicationContext(), "No Inputs!", Toast.LENGTH_SHORT).show();

                    }else if(initialRequest == false && inputmessage.length() > 0 && toggle == true){
                        MessageInput input = new MessageInput.Builder()
                                .text(inputmessage)
                                .build();
                        MessageOptions options = new MessageOptions.Builder()
                                .assistantId(mContext.getString(R.string.assistant_id))
                                .input(input)
                                .sessionId(watsonAssistantSession.getResult().getSessionId())
                                .build();
                        Response<MessageResponse> response = watsonAssistant.message(options).execute();
                        Log.i(TAG, "run: " + response.getResult());
                        if (response != null &&
                                response.getResult().getOutput() != null &&
                                !response.getResult().getOutput().getGeneric().isEmpty()) {

                            List<RuntimeResponseGeneric> responses = response.getResult().getOutput().getGeneric();

                            for (RuntimeResponseGeneric r : responses) {
                                Message outMessage;
                                switch (r.responseType()) {
                                    case "text":
                                        if(!dueDate){
                                            outMessage = new Message();
                                            outMessage.setMessage(r.text());
                                            outMessage.setId("2");
                                            messageArrayList.add(outMessage);
                                            new SayTask().execute(outMessage.getMessage());

                                        }else{

                                            DueDate = sharedpreferences.getString("Date", null);
                                            outMessage = new Message();
                                            if(!dueDate){
                                                outMessage.setMessage(r.text());
                                                outMessage.setId("2");
                                                messageArrayList.add(outMessage);
                                                new SayTask().execute(outMessage.getMessage());
                                            }else{
                                                outMessage.setMessage("Your due date is " + DueDate);
                                                outMessage.setId("2");
                                                messageArrayList.add(outMessage);
                                                new SayTask().execute(outMessage.getMessage());
                                            }

                                        }

                                        break;

                                    case "option":
                                        outMessage =new Message();
                                        String title = r.title();
                                        String OptionsOutput = "";
                                        for (int i = 0; i < r.options().size(); i++) {
                                            DialogNodeOutputOptionsElement option = r.options().get(i);
                                            OptionsOutput = OptionsOutput + option.getLabel() +"\n";

                                        }
                                        outMessage.setMessage(title + "\n" + OptionsOutput);
                                        outMessage.setId("2");

                                        messageArrayList.add(outMessage);

                                        // speak the message
                                        new SayTask().execute(outMessage.getMessage());
                                        break;

                                    case "image":
                                        outMessage = new Message(r);
                                        messageArrayList.add(outMessage);

                                        // speak the description
                                        new SayTask().execute(outMessage.getTitle() + outMessage.getDescription());
                                        break;
                                    default:
                                        Log.e("Error", "Unhandled message type");
                                }
                            }

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    mAdapter.notifyDataSetChanged();
                                    if (mAdapter.getItemCount() > 1) {
                                        recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);

                                    }

                                }
                            });
                        }
                    }



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    //Record a message via Watson Speech to Text
    private void recordMessage() {

        if (listening != true) {
            listening = true;
            streamPlayer.interrupt();
            capture = microphoneHelper.getInputStream(true);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(!Thread.interrupted()){
                            speechService.recognizeUsingWebSocket(getRecognizeOptions(capture), new MicrophoneRecognizeDelegate());

                        }
                    } catch (Exception e) {
                        showError(e);
                    }

                }
            }).start();
            Toast.makeText(MainActivity.this, "I am Listening now!", Toast.LENGTH_SHORT).show();

        } else {
            try {
                microphoneHelper.closeInputStream();
                listening = false;
                Toast.makeText(MainActivity.this, "Listening Stopped!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    /**
     * Check Internet Connection
     *
     * @return
     */
    private boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected) {
            return true;
        } else {
            Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
            return false;
        }

    }

    //Private Methods - Speech to Text
    private RecognizeOptions getRecognizeOptions(InputStream audio) {
        return new RecognizeOptions.Builder()
                .audio(audio)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .build();
    }

    private void showMicText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                inputMessage.setText(text);
            }
        });
    }

    private void enableMicButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnRecord.setEnabled(true);
            }
        });
    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }


    private class SayTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

                for (int i = 0; i < params.length; i++) {
                    if(listening){
                        streamPlayer.interrupt();
                        break;
                    }else{
                        streamPlayer.playStream(textToSpeech.synthesize(new SynthesizeOptions.Builder()
                                .text(params[i])
                                .voice(SynthesizeOptions.Voice.EN_US_LISAVOICE)
                                .accept(HttpMediaType.AUDIO_WAV)
                                .build()).execute().getResult());
                    }
                }

            return "Did synthesize";
        }


    }

    //Watson Speech to Text Methods.
    private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback {
        @Override
        public void onTranscription(SpeechRecognitionResults speechResults) {
            if (speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                if(text.length()>0){
                    showMicText(text);
                }
            }
        }

        @Override
        public void onError(Exception e) {
            showError(e);
            enableMicButton();
        }

        @Override
        public void onDisconnected() {
            enableMicButton();
        }

    }


    private void  startPulse() {
        r.run();
    };

    private void  stopPulse() {
        handlerAnimation.removeCallbacks(r);
    };


    final Runnable r = new Runnable() {
        public void run() {
            imgAnimation1.animate().scaleX(2f).scaleY(2f).alpha(0f).setDuration(1000)
                    .withEndAction(new Runnable(){
                        public void run(){
                            imgAnimation1.setScaleX( 1f);
                            imgAnimation1.setScaleY(1f);
                            imgAnimation1.setAlpha(1f);
                        }
                    });

            imgAnimation2.animate().scaleX(2f).scaleY(2f).alpha(0f).setDuration(700)
                    .withEndAction(new Runnable(){
                        public void run(){
                            imgAnimation2.setScaleX( 1f);
                            imgAnimation2.setScaleY(1f);
                            imgAnimation2.setAlpha(1f);
                        }
                    });
            handlerAnimation.postDelayed(r, 1500);
        }
    };
}



