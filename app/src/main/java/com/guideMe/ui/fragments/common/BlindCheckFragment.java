package com.guideMe.ui.fragments.common;

import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.guideMe.R;
import com.guideMe.databinding.FragmentBlindCheckBinding;
import com.guideMe.models.DoubleClickListener;
import com.guideMe.models.STT;
import com.guideMe.models.Speaker;
import com.guideMe.ui.activities.MainActivity;

public class BlindCheckFragment extends Fragment {
    FragmentBlindCheckBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentBlindCheckBinding.inflate(inflater, container, false);
        Speaker.Init(requireContext(), () -> {
            Log.w("Speaker", "init");
            Speaker.speak("Welcome to the Guide Me app! To detect objects, simply tap the screen. For text detecting, double-tap the screen. And for voice commands, perform a long press on the screen.");
//            Speaker.speak("Welcome to " + getString(R.string.app_name) + " app.");
//            Speaker.speakSilence(200L);
//            Speaker.speakAfter("If you want to detect objects tap the screen");
//            Speaker.speakSilence(100L);
//            Speaker.speakAfter("To read a text double tap the screen");
//            Speaker.speakSilence(100L);
//            Speaker.speakAfter("To voice command long press on the screen");
        });


        binding.getRoot().setOnClickListener(new DoubleClickListener() {
            @Override
            public void onClick() {
                //Object detection
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.cameraObjectsFragment);
            }

            @Override
            public void onDoubleClick() {
                //OCR detection
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.cameraOCRFragment);
            }
        });

        binding.getRoot().setOnLongClickListener(view -> {
            //Start voice command
            STT.siriWave = binding.siriWave;
            STT.Init(requireContext());
            MainActivity.startLongClick(requireContext(),binding.siriWave);
            return true;
        });

        binding.swipeBtn.setOnStateChangeListener(active -> {
            Log.w("ACTIVE", "" + active);
            //TO DO login
            if (active) {
                Speaker.speak("");
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.action_blindCheckFragment_to_loginFragment);
            }
        });

        return binding.getRoot();
    }


}

