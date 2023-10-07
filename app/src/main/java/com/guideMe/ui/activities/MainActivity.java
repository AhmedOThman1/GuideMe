package com.guideMe.ui.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.Navigation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.am.siriview.DrawView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.guideMe.FCM.FCM_ApiClient;
import com.guideMe.FCM.PushNotification;
import com.guideMe.R;
import com.guideMe.models.STT;
import com.guideMe.models.Speaker;
import com.guideMe.pojo.LiveCallData;
import com.guideMe.pojo.VideoCallRequest;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public final static int code = 6000;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    public static String language = "en";
    public static final int timeInMillis = 20000;
    public static final char space = ' ';


    public static Long appID = 189576433L;
    public static String appSign = "06dc884755c1fd92ab6e824ab2a6caa3f6699e06cc1c8b0908c62a9af7451b2a";

    FirebaseDatabase database;
    DatabaseReference liveCallRef, beMyEyesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);

        // for offline mode
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.keepSynced(true);


        if (checkPermission(this, REQUESTED_PERMISSIONS[0])) {
//            STT.Init(this);

            if (!checkPermission(this, REQUESTED_PERMISSIONS[1]) || !checkPermission(this, REQUESTED_PERMISSIONS[2]))
                requestPermissions(REQUESTED_PERMISSIONS, code);

        } else requestPermissions(REQUESTED_PERMISSIONS, code);
//
        STT.results.observe(this, arrayList -> {
            if (arrayList.isEmpty()) return;
            String s = arrayList.get(0);
            handleText(s);
        });

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        database = FirebaseDatabase.getInstance();
        beMyEyesRef = database.getReference("Be My Eyes");
        liveCallRef = database.getReference("LiveCallData");
        getLiveCallData();
    }

    LiveCallData liveCallData;

    private void getLiveCallData() {
        // Read from the database
        liveCallRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                liveCallData = snapshot.getValue(LiveCallData.class);

                if (liveCallData != null) {
                    appID = liveCallData.appID;
                    appSign = liveCallData.appSign;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    public static void startLongClick(Context context, DrawView view) {
        if (STT.getInstance() == null) {
            Toast.makeText(context, "Failed to listen to you!", Toast.LENGTH_SHORT).show();
            if (checkPermission(context, REQUESTED_PERMISSIONS[0]))
                STT.Init(context);
            else {
                ((Activity) context).requestPermissions(REQUESTED_PERMISSIONS, code);
                return;
            }
        }
        Log.w("Test", "" + view);
//        if (view != null)
//            STT.siriWave = view;
        STT.startListening();
        Log.w("LONG", "HERE");
    }


    private void handleText(String textToHandle) {
        if (textToHandle.contains("open the camera") || textToHandle.contains("camera")) {
            Speaker.speak(getString(R.string.sentence1));
            while (Speaker.isSpeakingNow()) {
            }
            startLongClick(this, null);
        } else if (textToHandle.contains("object") || textToHandle.contains("object detection")) {
            if (!checkPermission(this, REQUESTED_PERMISSIONS[1]) || !checkPermission(this, REQUESTED_PERMISSIONS[2]))
                requestPermissions(REQUESTED_PERMISSIONS, code);
            else
                Navigation.findNavController(this, R.id.nav_host_fragment)
                        .navigate(R.id.cameraObjectsFragment);
        } else if (textToHandle.contains("text") || textToHandle.contains("ocr") || textToHandle.contains("text detection") || textToHandle.contains("read this")) {
            if (!checkPermission(this, REQUESTED_PERMISSIONS[1]) || !checkPermission(this, REQUESTED_PERMISSIONS[2]))
                requestPermissions(REQUESTED_PERMISSIONS, code);
            else
                Navigation.findNavController(this, R.id.nav_host_fragment)
                        .navigate(R.id.cameraOCRFragment);
        } else if (textToHandle.contains("need a volunteer") || textToHandle.contains("help from person") || textToHandle.contains("video call") || textToHandle.contains("be my eye") || textToHandle.contains("volunteer")) {
            VideoCallRequest item = new VideoCallRequest();
            item.id = beMyEyesRef.push().getKey();
            item.date = System.currentTimeMillis();
            item.img = "https://api.dicebear.com/7.x/thumbs/png?seed=" +
                    item.id;
            beMyEyesRef.child(item.id).setValue(item);

            Intent intent = new Intent(this, LiveActivity.class);
            intent.putExtra("item", new Gson().toJson(item));
            intent.putExtra("userID", generateUserID());
            intent.putExtra("userName", "Blind Person");
            startActivity(intent);
        } else if (textToHandle.contains("what is this app") || textToHandle.contains("about") || textToHandle.contains("about the app") || textToHandle.contains("about us")) {
            Speaker.speak("Welcome to Guide Me, an app dedicated to assisting the visually impaired. Use voice commands to access services like object recognition, text detection, and more. Connect with volunteers for real-time assistance—all in one place to empower and connect.");
        }
        ///////////////
        else if (textToHandle.contains("hi") || textToHandle.contains("hello") || textToHandle.contains("hey") || textToHandle.contains("how are you")) {
            int r = (int) ((Math.random() * (300 - 1)) + 1);
            if (r < 100)
                Speaker.speak(getString(R.string.sentence4));
            else if (r < 200)
                Speaker.speak(getString(R.string.sentence5));
            else
                Speaker.speak(getString(R.string.sentence6));

            while (Speaker.isSpeakingNow()) {
            }
            startLongClick(this, null);
        } else if (textToHandle.contains("i need a help") || textToHandle.contains("i need help") || textToHandle.contains("help me") || textToHandle.contains("help")) {
            Speaker.speak(getString(R.string.sentence8));
            while (Speaker.isSpeakingNow()) {
            }
            startLongClick(this, null);
        } else if (textToHandle.contains("what time is it") || textToHandle.contains("What is the current time?")) {
            Speaker.speak(getString(R.string.sentence9));
            Calendar calendar = Calendar.getInstance();
            Speaker.speakAfter(calendar.get(Calendar.HOUR) + " o'clock");
            if (calendar.get(Calendar.MINUTE) != 0)
                Speaker.speakAfter(" and " + calendar.get(Calendar.MINUTE) + " minutes.");
        } else if (textToHandle.contains("date") || textToHandle.contains("today") || textToHandle.contains("what today") || textToHandle.contains("what day is it")) {
            Speaker.speak(getString(R.string.sentence10) + getDays(System.currentTimeMillis()) + ", " + new SimpleDateFormat("d MMMM", Locale.forLanguageTag(language)).format(System.currentTimeMillis()));
        }

        Toast.makeText(this, textToHandle, Toast.LENGTH_SHORT).show();
    }

    private String getDays(long currentTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis);
        return new SimpleDateFormat("EEEE", Locale.forLanguageTag(language)).format(currentTimeMillis);
    }

    public static boolean checkPermission(Context activity, String permission) {
        if (ContextCompat.checkSelfPermission(activity, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        return true;
    }

    public static void sendNotificationWithToken(String token, String title, String message) {
        FCM_ApiClient.getINSTANCE()
                .pushNotification(new PushNotification(new PushNotification.NotificationData(
                        title, message
                ), token)).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            Log.w("Success", "" + response.body() + "\n\n" + response.message());
                        } else {
                            Log.w("Not Success", "" + response.message() + " , " + response.errorBody().toString() + " , " + response.body());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Log.w("Error", "" + t.getMessage());
                    }
                });
    }

    public static void restartApp(Activity activity) {
        Intent restartIntent = activity.getBaseContext()
                .getPackageManager()
                .getLaunchIntentForPackage(activity.getBaseContext().getPackageName());
        assert restartIntent != null;
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(restartIntent);
        activity.finish();
    }

    public static void openKeyboard(EditText textInputLayout, Activity activity) {
        textInputLayout.requestFocus();     // editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);     // Context.INPUT_METHOD_SERVICE
        assert imm != null;
        imm.showSoftInput(textInputLayout, InputMethodManager.SHOW_IMPLICIT); //    first param -> editText
    }


    public static void close_keyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);     // Context.INPUT_METHOD_SERVICE
            assert imm != null;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }


    public static String formatPrice(Double price) {
        if (price == 0) return "";
        if (price.intValue() == price)
            return "$" + price.intValue();
        return ("$" + new DecimalFormat("#.##").format(price));
    }

    private String generateUserID() {
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        while (builder.length() < 5) {
            int nextInt = random.nextInt(10);
            if (builder.length() == 0 && nextInt == 0) {
                continue;
            }
            builder.append(nextInt);
        }
        return builder.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        STT.results = new MutableLiveData<>();

    }
}