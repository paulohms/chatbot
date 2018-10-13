package com.chatbot.chatbot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.PartialResultsListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class MainActivity extends AppCompatActivity implements AIListener {

    private EditText editText;
    private ListView messagesView;
    private MessageAdapter messageAdapter;
    private boolean currentUser = true;
    private AIService aiService;
    private AIConfiguration config;

    private static final int RECORD_AUDIO_PERMISSION = 1;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        messageAdapter = new MessageAdapter(this);
        messagesView = (ListView) findViewById(R.id.messages_view);
        messagesView.setAdapter(messageAdapter);

        //Setando as configurações
         config = new AIConfiguration("2c25bcb1adfa49e29f640083698f15e4",
                AIConfiguration.SupportedLanguages.PortugueseBrazil,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        final AIDataService aiDataService = new AIDataService(config);

        final AIRequest aiRequest = new AIRequest();
        aiRequest.setQuery("query-welcome-initial");

        new AsyncTask<AIRequest, Void, AIResponse>() {
            @Override
            protected AIResponse doInBackground(AIRequest... requests) {
                final AIRequest request = requests[0];
                try {
                    final AIResponse response = aiDataService.request(aiRequest);
                    return response;
                } catch (AIServiceException e) {
                    setError("Ops! Algo deu errado, verifique se você tem conexão com a internet e tente novamente.");
                }
                return null;
            }
            @Override
            protected void onPostExecute(AIResponse aiResponse) {
                if (aiResponse != null) {
                    messageAdapter.add(new Message(aiResponse.getResult().getFulfillment().getSpeech(), !currentUser));
                    messagesView.setSelection(messagesView.getCount() - 1);
                }
            }
        }.execute(aiRequest);

    }

    public void ouvir(View view){

        if("".equals(editText.getText().toString())) {
            return;
        } else {
            final AIDataService aiDataService = new AIDataService(config);

            final AIRequest aiRequest = new AIRequest();
            aiRequest.setQuery(editText.getText().toString());

            messageAdapter.add(new Message(editText.getText().toString(), currentUser));
            messagesView.setSelection(messagesView.getCount() - 1);

            new AsyncTask<AIRequest, Void, AIResponse>() {
                @Override
                protected AIResponse doInBackground(AIRequest... requests) {
                    final AIRequest request = requests[0];
                    try {
                        final AIResponse response = aiDataService.request(aiRequest);
                        return response;
                    } catch (AIServiceException e) {
                        setError("Ops! Algo deu errado, verifique se você tem conexão com a internet e tente novamente.");
                    }
                    return null;
                }
                @Override
                protected void onPostExecute(AIResponse aiResponse) {
                    if (aiResponse != null) {
                        messageAdapter.add(new Message(aiResponse.getResult().getFulfillment().getSpeech(), !currentUser));
                        messagesView.setSelection(messagesView.getCount() - 1);
                    }
                }
            }.execute(aiRequest);

        }

        editText.getText().clear();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                aiService.startListening();

            } else {

                messageAdapter.add(new Message("Permissão não obtida", currentUser));
                messagesView.setSelection(messagesView.getCount() - 1);

            }
        }

    }

    @Override
    public void onResult(AIResponse result) {
        setResult(result.getResult());
    }

    @Override
    public void onError(AIError error) {
        messageAdapter.add(new Message(error.toString(), currentUser));
        messagesView.setSelection(messagesView.getCount() - 1);
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {
        Log.d(TAG,"Lendo audio!");
    }

    @Override
    public void onListeningCanceled() {
        Log.d(TAG,"Leitura de audio cancelada!");
    }

    @Override
    public void onListeningFinished() {
        Log.d(TAG,"-------------->>>>>>>> Lendo audio finalizada!");
    }

    private void setResult(Result result){
        messageAdapter.add(new Message(result.getResolvedQuery(), currentUser));
        messagesView.setSelection(messagesView.getCount() - 1);
        messageAdapter.add(new Message(result.getFulfillment().getSpeech(), !currentUser));
        messagesView.setSelection(messagesView.getCount() - 1);
    }

    private void setError(String error){
        messageAdapter.add(new Message(error, !currentUser));
        messagesView.setSelection(messagesView.getCount() - 1);
    }

}