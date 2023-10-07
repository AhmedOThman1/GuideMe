package com.guideMe.ui.fragments.blind.camera.ocr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.guideMe.databinding.FragmentCameraOcrBinding;
import com.guideMe.models.DoubleClickListener;
import com.guideMe.models.Speaker;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraOCRFragment extends Fragment {
    FragmentCameraOcrBinding binding;
    CameraSource cameraSource;

    boolean detect = false;

    public CameraOCRFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        binding = FragmentCameraOcrBinding.inflate(inflater, container, false);
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        Speaker.Init(requireContext(), () -> {
            Speaker.speak("Stated text detection mode.");
            Speaker.speakSilence(50L);
            Speaker.speakAfter("Click on the screen to start detecting!");
        });

        binding.getRoot().setOnClickListener(new DoubleClickListener() {
            @Override
            public void onClick() {
                Speaker.speak("Starting detecting objects");
                detect = true;
            }

            @Override
            public void onDoubleClick() {
                readResult();
            }
        });


        TextRecognizer textRecognizer = new TextRecognizer.Builder(requireContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {
            cameraSource = new CameraSource.Builder(requireContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            binding.surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {

                    try {
                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

//                            ActivityCompat.requestPermissions(requireActivity(),
//                                    new String[]{Manifest.permission.CAMERA},
//                                    RequestCameraPermissionID);
                            requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});

                            return;
                        }
                        cameraSource.start(binding.surfaceView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0) {
                        binding.textView.post(() -> {
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int i = 0; i < items.size(); i++) {
                                TextBlock item = items.valueAt(i);
                                stringBuilder.append(item.getValue());
                                stringBuilder.append("\n");
                            }
                            if (detect) {
                                result = stringBuilder.toString();
                                readResult();
                                detect = false;
                            }
                            binding.textView.setText(stringBuilder.toString());
                        });
                    }
                }
            });
        }

        return binding.getRoot();
    }

    String result;

    void readResult() {
        Speaker.speak("The result is ");
        Speaker.speakSilence(200L);
        Speaker.speakAfter("" + result);
        Speaker.speakSilence(1000L);
        Speaker.speakAfter("To detect another text tap the screen.");
        Speaker.speakSilence(200L);
        Speaker.speakAfter("To listen the result again double tap on the screen");
    }


    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                AtomicBoolean b = new AtomicBoolean(true);
                permissions.forEach((key, value) -> {
                    if (!value) b.set(false);
                });
                if (b.get()) {
                    try {
                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        cameraSource.start(binding.surfaceView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

}
