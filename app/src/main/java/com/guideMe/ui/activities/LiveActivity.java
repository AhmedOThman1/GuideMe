package com.guideMe.ui.activities;

import static com.guideMe.ui.activities.MainActivity.appID;
import static com.guideMe.ui.activities.MainActivity.appSign;
import static com.guideMe.ui.activities.MainActivity.sendNotificationWithToken;
import static com.guideMe.ui.fragments.common.LauncherFragment.type;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.guideMe.R;
import com.guideMe.databinding.ActivityLiveBinding;
import com.guideMe.models.DoubleClickListener;
import com.guideMe.models.Speaker;
import com.guideMe.pojo.Helper;
import com.guideMe.pojo.VideoCallRequest;
import com.zegocloud.uikit.components.audiovideocontainer.ZegoLayoutMode;
import com.zegocloud.uikit.components.audiovideocontainer.ZegoLayoutPictureInPictureConfig;
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig;
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment;
import com.zegocloud.uikit.prebuilt.call.config.ZegoMenuBarButtonName;
import com.zegocloud.uikit.prebuilt.call.config.ZegoMenuBarStyle;
import com.zegocloud.uikit.service.internal.UIKitCore;

import java.util.ArrayList;
import java.util.Arrays;

public class LiveActivity extends AppCompatActivity {

    FirebaseDatabase database;
    DatabaseReference callRef, helpersRef;
    VideoCallRequest callRequest;
    ActivityLiveBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        setContentView(R.layout.activity_live);

        binding = ActivityLiveBinding.inflate(getLayoutInflater());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        callRequest = new Gson().fromJson(getIntent().getStringExtra("item"), VideoCallRequest.class);
        database = FirebaseDatabase.getInstance();
        callRef = database.getReference("Be My Eyes").child(callRequest.id);
        helpersRef = database.getReference("Helpers");


        if (type == null) {
            binding.screen.setVisibility(View.VISIBLE);
            Speaker.Init(this, () -> {
                Speaker.speak("Front Camera has been opened and we were waiting for a volunteer to enter.");
                Speaker.speakSilence(50L);
                Speaker.speakAfter("To end the call click on the screen, and double click to flip the cam!");
                getHelpersTokens();
            });

        }

        binding.screen.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onClick() {
                Log.w("ScreenE", "onClick");
                finish();
            }

            @Override
            public void onDoubleClick() {
                Log.w("ScreenE", "onDoubleClick");
                UIKitCore.getInstance().useFrontFacingCamera(!UIKitCore.getInstance().isUseFrontCamera());
            }
        });

        addCallFragment(
                callRequest.id,
                getIntent().getStringExtra("userID"),
                getIntent().getStringExtra("userName"));
    }

    ArrayList<Helper> helpers = new ArrayList<>();

    private void getHelpersTokens() {
        // Read from the database
        helpersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                helpers = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Helper item = dataSnapshot.getValue(Helper.class);
                    helpers.add(item);
                }

                for (Helper helper : helpers) {
                    sendNotificationWithToken(
                            helper.token,
                            "Blind person need help!",
                            "Open the app to see the request and be his/her eyes."
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    ZegoUIKitPrebuiltCallConfig config;
    ZegoUIKitPrebuiltCallFragment fragment;

    public void addCallFragment(String callID, String userID, String userName) {
        config =
                type == null ?
                        blindSideVideoCall()
                        :
                        helperSideVoiceCall();
        fragment = ZegoUIKitPrebuiltCallFragment.newInstance(appID, appSign, userID, userName, callID, config);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commitNow();
    }

    public static ZegoUIKitPrebuiltCallConfig blindSideVideoCall() {
        ZegoUIKitPrebuiltCallConfig config = new ZegoUIKitPrebuiltCallConfig();
        config.turnOnCameraWhenJoining = true;
        config.turnOnMicrophoneWhenJoining = true;
        config.useSpeakerWhenJoining = true;
        config.layout.mode = ZegoLayoutMode.PICTURE_IN_PICTURE;
        config.layout.config = new ZegoLayoutPictureInPictureConfig();
        config.bottomMenuBarConfig.buttons = new ArrayList<>();
        config.topMenuBarConfig.buttons = new ArrayList<>();
        config.bottomMenuBarConfig.style = ZegoMenuBarStyle.LIGHT;
        config.topMenuBarConfig.isVisible = false;
        config.audioVideoViewConfig.useVideoViewAspectFill = true;
        return config;
    }

    public static ZegoUIKitPrebuiltCallConfig helperSideVoiceCall() {
        ZegoUIKitPrebuiltCallConfig config = new ZegoUIKitPrebuiltCallConfig();
        config.turnOnCameraWhenJoining = false;
        config.turnOnMicrophoneWhenJoining = true;
        config.useSpeakerWhenJoining = true;
        config.layout.mode = ZegoLayoutMode.PICTURE_IN_PICTURE;
        config.layout.config = new ZegoLayoutPictureInPictureConfig();
        config.bottomMenuBarConfig.buttons = new ArrayList<>(
                Arrays.asList(ZegoMenuBarButtonName.TOGGLE_MICROPHONE_BUTTON, ZegoMenuBarButtonName.HANG_UP_BUTTON));
        config.bottomMenuBarConfig.style = ZegoMenuBarStyle.DARK;
        config.topMenuBarConfig.buttons = new ArrayList<>();
        config.topMenuBarConfig.isVisible = false;
        config.audioVideoViewConfig.useVideoViewAspectFill = true;
        return config;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (type == null) {
            callRequest.end = true;
            callRef.child("end")
                    .setValue(true);
        }
        Speaker.speak("Call ended!");
    }
}