package com.guideMe.ui.fragments.blind.camera.objects;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.guideMe.R;
import com.guideMe.customView.OverlayView;
import com.guideMe.databinding.FragmentCameraObjectsBinding;
import com.guideMe.deepmodel.DetectionResult;
import com.guideMe.deepmodel.MobileNetObjDetector;
import com.guideMe.models.DoubleClickListener;
import com.guideMe.models.ImageUtils;
import com.guideMe.models.Speaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CameraObjectsFragment extends Fragment
        implements OnImageAvailableListener {
    private static int MODEL_IMAGE_INPUT_SIZE = 300;
    private static float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;
    private int previewWidth = 0;
    private int previewHeight = 0;
    private MobileNetObjDetector objectDetector;
    private Bitmap imageBitmapForModel = null;
    private Bitmap rgbBitmapForCameraImage = null;
    private boolean computing = false;
    private Matrix imageTransformMatrix;

    private OverlayView overlayView;

    private static final String LOGGING_TAG = "CameraFragment";
    private static final int PERMISSIONS_REQUEST = 1;

    private Handler handler;
    private HandlerThread handlerThread;
    FragmentCameraObjectsBinding binding;

    public CameraObjectsFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        binding = FragmentCameraObjectsBinding.inflate(inflater, container, false);
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.w("Permission",""+hasPermission());
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }
        Speaker.Init(requireContext(), () -> {
            Speaker.speak("Stated object detection mode.");
            Speaker.speakSilence(50L);
            Speaker.speakAfter("Click on the screen to start detecting!");
        });

        binding.getRoot().setOnClickListener(new DoubleClickListener() {
            @Override
            public void onClick() {
                Speaker.speak("Starting detecting objects");
                detect = true;
                start = 0;
            }

            @Override
            public void onDoubleClick() {
                readResult();
            }
        });
        return binding.getRoot();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
//        if (!requireActivity().isFinishing()) {
//            requireActivity().finish();
//        }

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException ex) {
            Log.e(LOGGING_TAG, "Exception: " + ex.getMessage());
        }

        super.onPause();
    }

    protected synchronized void runInBackground(final Runnable runnable) {
        if (handler != null) {
            handler.post(runnable);
        }
    }

//    @Override
//    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,
//                                           final int[] grantResults) {
//        switch (requestCode) {
//            case PERMISSIONS_REQUEST: {
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
//                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//                    setFragment();
//                } else {
//                    requestPermission();
//                }
//            }
//        }
//    }

    private boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
               ;
    }

    private void requestPermission() {
        if (requireActivity().shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                || requireActivity().shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(requireContext(),
                    "Camera AND storage permission are required for this app", Toast.LENGTH_LONG).show();
        }
        requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
    }

    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                AtomicBoolean b = new AtomicBoolean(true);
                permissions.forEach((key, value) -> {
                    if (!value) b.set(false);
                });
                if (b.get()) {
                    setFragment();
                }
            });

    protected void setFragment() {
        CameraConnectionFragment cameraConnectionFragment = new CameraConnectionFragment();
        cameraConnectionFragment.addConnectionListener(CameraObjectsFragment.this::onPreviewSizeChosen);
        cameraConnectionFragment.addImageAvailableListener(this);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.container, cameraConnectionFragment)
                .commit();
    }

    public void requestRender() {
        final OverlayView overlay = binding.getRoot().findViewById(R.id.overlay);
        if (overlay != null) {
            overlay.postInvalidate();
        }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {
        final OverlayView overlay = binding.getRoot().findViewById(R.id.overlay);
        if (overlay != null) {
            overlay.addCallback(callback);
        }
    }

    public void onPreviewSizeChosen(final Size previewSize, final int rotation) {
        final float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TEXT_SIZE_DIP, getResources().getDisplayMetrics());

        try {
            objectDetector = MobileNetObjDetector.create(requireActivity().getAssets());
            Log.i(LOGGING_TAG, "Model Initiated successfully.");
//            Toast.makeText(requireContext(), "MobileNetObjDetector created", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "MobileNetObjDetector could not be created", Toast.LENGTH_SHORT).show();
//            requireActivity().finish();
        }
        overlayView = binding.getRoot().findViewById(R.id.overlay);

        final int screenOrientation = requireActivity().getWindowManager().getDefaultDisplay().getRotation();
        //Sensor orientation: 90, Screen orientation: 0
        sensorOrientation = rotation + screenOrientation;
        Log.i(LOGGING_TAG, String.format("Camera rotation: %d, Screen orientation: %d, Sensor orientation: %d",
                rotation, screenOrientation, sensorOrientation));

        previewWidth = previewSize.getWidth();
        previewHeight = previewSize.getHeight();
        Log.i(LOGGING_TAG, "preview width: " + previewWidth);
        Log.i(LOGGING_TAG, "preview height: " + previewHeight);
        // create empty bitmap
        imageBitmapForModel = Bitmap.createBitmap(MODEL_IMAGE_INPUT_SIZE, MODEL_IMAGE_INPUT_SIZE, Bitmap.Config.ARGB_8888);
        rgbBitmapForCameraImage = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);

        imageTransformMatrix = ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                MODEL_IMAGE_INPUT_SIZE, MODEL_IMAGE_INPUT_SIZE, sensorOrientation, true);
        imageTransformMatrix.invert(new Matrix());
    }

    List<DetectionResult> fiveSecondDetection = new ArrayList<>();
    Map<String, Float> map = new HashMap<>();

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image imageFromCamera = null;

        try {
            imageFromCamera = reader.acquireLatestImage();
            if (imageFromCamera == null) {
                return;
            }
            if (computing) {
                imageFromCamera.close();
                return;
            }
            computing = true;
            preprocessImageForModel(imageFromCamera);
            imageFromCamera.close();
        } catch (final Exception ex) {
            if (imageFromCamera != null) {
                imageFromCamera.close();
            }
            Log.e(LOGGING_TAG, ex.getMessage());
        }

        runInBackground(() -> {
            Log.w("Background",""+detect+","+start+","+seconds);
            if (detect && start == 0) {
                start = System.currentTimeMillis();
                fiveSecondDetection = new ArrayList<>();
                map = new HashMap<>();
            }
            final List<DetectionResult> results = objectDetector.detectObjects(imageBitmapForModel);
            final List<DetectionResult> temp = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
//                Log.w("Confidence", "" + results.get(i).getConfidence());
                if (results.get(i).getConfidence() > 0.60) {
                    temp.add(results.get(i));
                    if (detect && start != 0) {
                        fiveSecondDetection.add(results.get(i));
                        map.putIfAbsent(results.get(i).getTitle(), 0f);
                        map.put(results.get(i).getTitle(), map.get(results.get(i).getTitle()) + results.get(i).getConfidence());
                    }
                }

            }
            overlayView.setResults(temp);

            if (detect && start != 0) {
                seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start);
                Log.w("SEC", "" + seconds);
            }

            if (seconds == 5) {
                start = 0;
                seconds = 0;
                detect = false;
                processDetection();
            }
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < temp.size(); i++) {
                if (i != 0 && i != temp.size() - 1) {
                    s.append(", ");
                } else if (i != 0 && i == temp.size() - 1) {
                    s.append(", and ");
                }
                s.append(temp.get(i).getTitle());
            }
//            if (!temp.isEmpty()) {
//                Log.w("Detected", "" + s.toString());
//                if (!oldDetect.equals(s.toString())) {
//                    Speaker.speak("" + s.toString());
//                    oldDetect = s.toString();
//                }
//            }

            requestRender();
            computing = false;
        });
    }

    String result;

    private void processDetection() {
        fiveSecondDetection.sort(Comparator.comparing(DetectionResult::getConfidence, Comparator.reverseOrder()));
        for (int i = 0; i < fiveSecondDetection.size(); i++) {
            Log.w("Detect " + i, fiveSecondDetection.get(i) + "");
        }
        List<DetectionResult> tempFiveSecondDetection = new ArrayList<>();

        for (Map.Entry<String, Float> entry : map.entrySet()) {
            tempFiveSecondDetection.add(new DetectionResult(0, entry.getKey(), entry.getValue()));
        }

        tempFiveSecondDetection.sort(Comparator.comparing(DetectionResult::getConfidence, Comparator.reverseOrder()));


        StringBuilder s = new StringBuilder();
        int size = Math.min(tempFiveSecondDetection.size(), 5);
        for (int i = 0; i < size; i++) {
            Log.w("#Detect " + i, tempFiveSecondDetection.get(i) + "");
            if (i != 0 && i != size - 1) {
                s.append(", ");
            } else if (i != 0 && i == size - 1) {
                s.append(", and ");
            }
            s.append(tempFiveSecondDetection.get(i).getTitle());
        }

        result = s.toString();

        readResult();
    }

    void readResult() {
        Speaker.speak("The result is ");
        Speaker.speakSilence(200L);
        Speaker.speakAfter("" + result);
        Speaker.speakSilence(1000L);
        Speaker.speakAfter("To detect another objects tap the screen.");
        Speaker.speakSilence(200L);
        Speaker.speakAfter("To listen the result again double tap on the screen");
    }

    boolean detect = false;
    long seconds, start;
    String oldDetect = "";

    private void preprocessImageForModel(final Image imageFromCamera) {
        rgbBitmapForCameraImage.setPixels(ImageUtils.convertYUVToARGB(imageFromCamera, previewWidth, previewHeight),
                0, previewWidth, 0, 0, previewWidth, previewHeight);

        new Canvas(imageBitmapForModel).drawBitmap(rgbBitmapForCameraImage, imageTransformMatrix, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (objectDetector != null) {
            objectDetector.close();
        }
    }
}