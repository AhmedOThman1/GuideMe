package com.guideMe.models;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import com.am.siriview.BuildConfig;
import com.am.siriview.DrawView;
import com.am.siriview.UpdaterThread;
import com.guideMe.ui.activities.MainActivity;

import java.util.ArrayList;
import java.util.Random;

public class STT {
    public static MutableLiveData<ArrayList<String>> results = new MutableLiveData<>();
    public static SpeechRecognizer mSpeechRecognizer;
    public static Intent mSpeechRecognizerIntent;
    public Context context;
    public static DrawView siriWave;

    public static float tr = 0.0f;
    public static UpdaterThread up;
    private static final int REFRESH_INTERVAL_MS = 30;
    private static final int REQUEST_PERMISSION_RECORD_AUDIO = 1;

    public static void Init(Context context) {
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, MainActivity.language);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                context.getPackageName());

        SpeechRecognitionListener listener = new SpeechRecognitionListener(context);
        mSpeechRecognizer.setRecognitionListener(listener);

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            // Your code to set up and start the recognizer
            Log.w("Error","isRecognitionAvailable");
            ArrayList<String> supportedLanguages = mSpeechRecognizerIntent.getStringArrayListExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                Log.w("Error Language",""+supportedLanguages);
        } else {
            // Handle the case where speech recognition is not available
            Log.w("Error","isRecognitionNOTAvailable");
        }
        ///
        if (siriWave != null) {
            Log.w("HERE", "1" + siriWave);
            up = new UpdaterThread(REFRESH_INTERVAL_MS, siriWave, ((Activity) context));
            up.start();
            Log.w("HERE", "2" + siriWave);
        }
    }

    public static SpeechRecognizer getInstance() {
        return mSpeechRecognizer;
    }

    public static void startListening() {
        Log.w("HERE", "3" + siriWave);
        if (Speaker.isSpeakingNow()) {
            Speaker.speak("");
        }
        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        Log.w("HERE", "4" + siriWave);
    }


    public static class SpeechRecognitionListener implements RecognitionListener {
        Context context;
        SpeechRecognitionListener (Context context){
            this.context = context;
        }
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d("TAG", "onReadyForSpeech" + siriWave); //$NON-NLS-1$
            siriWave.setVisibility(View.VISIBLE);
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d("TAG", "onBeginingOfSpeech");
        }

        @Override
        public void onRmsChanged(float v) {
            Log.w("HERE", "5" + siriWave);
            v = Math.abs(v);
            double a = Math.pow(20.0d, ((double) v) / 14.14d);
            tr = ((float) a) * 100.0f;
            Log.d("RMS", v + " = " + a);
            up.setPRog(tr * 1.8f);
        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {
            Log.d("TAG", "onEndOfSpeech");
            tr = 0.0f;
        }

        @Override
        public void onError(int i) {
            tr = 0.0f;
            Log.d("Error Recording: ", BuildConfig.FLAVOR + i);
            Toast.makeText(context, ""+handleSpeechRecognitionError(i), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle bundle) {
            Log.d("TAG", "onResults"); //$NON-NLS-1$
            ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // matches are the return values of speech recognition engine
            // Use these values for whatever you wish to do
            Log.w("onResults", "" + matches);

            results.setValue(matches);
            tr = 0.0f;


        }

        @Override
        public void onPartialResults(Bundle bundle) {

        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }

    }

    public static String handleSpeechRecognitionError(int errorCode) {
        String errorTitle;

        switch (errorCode) {
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorTitle = "Network Timeout";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorTitle = "Network Error";
                break;
            case SpeechRecognizer.ERROR_AUDIO:
                errorTitle = "Audio Error";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                errorTitle = "Server Error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                errorTitle = "Client Error";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorTitle = "Speech Timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorTitle = "No Match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorTitle = "Recognizer Busy";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorTitle = "Insufficient Permissions";
                break;
            case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
                errorTitle = "TOO MANY REQUESTS";
                break;
            case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
                errorTitle = "ERROR SERVER DISCONNECTED";
                break;
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                errorTitle = "ERROR LANGUAGE NOT SUPPORTED";
                break;
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                errorTitle = "ERROR LANGUAGE UNAVAILABLE";
                break;
            case SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT:
                errorTitle = "ERROR CANNOT CHECK SUPPORT";
                break;
            default:
                errorTitle = "Unknown Error";
                break;
        }

        return errorTitle;
    }
}
