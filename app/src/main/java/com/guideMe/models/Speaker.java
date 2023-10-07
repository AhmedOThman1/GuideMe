package com.guideMe.models;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import com.guideMe.ui.activities.MainActivity;

import java.util.Locale;

public class Speaker {
    public static TextToSpeech t1;

    public static void Init(Context context,Interface init) {
        if (t1 == null)
            t1 = new TextToSpeech(context, status -> {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.forLanguageTag(MainActivity.language));
                    init.onInitEnded();
                }
            });
        else
            init.onInitEnded();
    }

    public static TextToSpeech getINSTANCE() {
        return t1;
    }

    public static boolean isSpeakingNow() {
        return t1.isSpeaking();
    }

    public static void speak(String textToSpeak) {
        t1.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public static void speakAfter(String textToSpeak) {
        t1.speak(textToSpeak, TextToSpeech.QUEUE_ADD, null, null);
    }

    public static void speakSilence(Long durationInMs) {
        t1.playSilentUtterance(durationInMs, TextToSpeech.QUEUE_ADD, null);
    }

    public interface Interface {
        void onInitEnded();
    }
}
