package com.guideMe.ui.fragments.manager.events.add;

import static com.guideMe.ui.activities.MainActivity.openKeyboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.guideMe.R;
import com.guideMe.databinding.FragmentManagerAddEventBinding;
import com.guideMe.pojo.Event;
import com.guideMe.ui.fragments.manager.main.ManagerMainFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManagerAddEventFragment extends Fragment {

    FragmentManagerAddEventBinding binding;
    Uri ImageUri;

    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference eventsRef;
    StorageReference eventsStorageRef;

    Event editItem;
    boolean editMode = false;

    Calendar calendar;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentManagerAddEventBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        eventsRef = database.getReference("Events");
        eventsStorageRef = FirebaseStorage.getInstance().getReference("Events");

        binding.image.setOnClickListener(v -> checkPermissionAndOpenGal());
        binding.uploadImage.setOnClickListener(v -> checkPermissionAndOpenGal());

        calendar = Calendar.getInstance();

        Bundle arg = getArguments();
        if (arg != null) {
            String json = arg.getString("item", "");
            if (!json.trim().isEmpty()) {
                //get edit item
                editItem = new Gson().fromJson(json, Event.class);
                if (editItem != null) {

                    Glide.with(requireContext())
                            .load(editItem.image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.image);
                    photo = editItem.image;
                    ImageUri = Uri.parse(editItem.image);
                    binding.uploadImage.setVisibility(View.GONE);

                    binding.eventTitle.getEditText().setText(editItem.title);
                    binding.eventLocation.getEditText().setText(editItem.location);
                    binding.description.getEditText().setText(String.valueOf(editItem.description));
                    calendar.setTimeInMillis(editItem.date);
                    binding.eventDate.getEditText().setText(new SimpleDateFormat("'\uD83D\uDCC5' EEEE, dd MMM \uD83D\uDD54 h:mm a", Locale.getDefault()).format(calendar.getTimeInMillis()));
                    editMode = true;
                    binding.addEvent.setText("Edit Event");
                    ManagerMainFragment.toolbar.setTitle("Edit Event");
                }
            }
        } else {
            binding.addEvent.setText(getString(R.string.add_event));
            ManagerMainFragment.toolbar.setTitle(getString(R.string.add_event));
        }

        binding.addEvent.setOnClickListener(v -> {
            if (binding.eventTitle.getEditText().getText().toString().trim().isEmpty()) {
                binding.eventTitle.setError(getResources().getString(R.string.empty));
                binding.eventTitle.requestFocus();
                openKeyboard(binding.eventTitle.getEditText(), requireActivity());
            } else if (binding.eventLocation.getEditText().getText().toString().trim().isEmpty()) {
                binding.eventTitle.setError(null);
                binding.eventLocation.setError(getResources().getString(R.string.empty));
                binding.eventLocation.requestFocus();
                openKeyboard(binding.eventLocation.getEditText(), requireActivity());
            } else if (binding.eventDate.getEditText().getText().toString().trim().isEmpty()) {
                binding.eventTitle.setError(null);
                binding.eventLocation.setError(null);
                binding.eventDate.setError(getResources().getString(R.string.empty));
                openPickDate();
            } else if (binding.description.getEditText().getText().toString().trim().isEmpty()) {
                binding.eventTitle.setError(null);
                binding.eventLocation.setError(null);
                binding.description.setError(getResources().getString(R.string.empty));
                binding.description.requestFocus();
                openKeyboard(binding.description.getEditText(), requireActivity());
            } else if (ImageUri == null) {
                binding.eventTitle.setError(null);
                binding.eventLocation.setError(null);
                binding.description.setError(null);
                binding.eventDate.setError(null);
                Toast.makeText(
                        requireContext(),
                        "Upload image first",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                binding.eventTitle.setError(null);
                binding.eventLocation.setError(null);
                binding.description.setError(null);
                binding.eventDate.setError(null);
                binding.loading.setVisibility(View.VISIBLE);
                binding.addEvent.setEnabled(false);

                id = (editMode ? editItem.id : eventsRef.push().getKey());

                if (editMode && editItem.image.equalsIgnoreCase(ImageUri.toString()))
                    uploadData();
                else
                    uploadImg();
            }

        });

        binding.pickEventDate.setOnClickListener(v -> {
            openPickDate();
        });
        return binding.getRoot();
    }


    String id, photo;

    private void uploadImg() {
        eventsStorageRef.child(id).putFile(ImageUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                eventsStorageRef.child(id).getDownloadUrl().addOnSuccessListener(uri -> {
                    photo = uri.toString();
                    uploadData();
                }).addOnFailureListener(e -> {
                    Toast.makeText(
                            requireContext(),
                            "Failed to upload ! Try again." + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    binding.loading.setVisibility(View.GONE);
                    binding.addEvent.setEnabled(true);
                });
            }
        }).continueWith(task -> {
            binding.loading.setVisibility(View.GONE);
            binding.addEvent.setEnabled(true);
            if (!task.isSuccessful()) {
                Toast.makeText(
                        requireContext(),
                        "Failed to upload! Try again.",
                        Toast.LENGTH_LONG
                ).show();
            }
            return task;
        });

    }

    private void uploadData() {
        if (editMode) {
            editItem.title = binding.eventTitle.getEditText().getText().toString().trim();
            editItem.location = binding.eventLocation.getEditText().getText().toString().trim();
            editItem.description = binding.description.getEditText().getText().toString().trim();
            editItem.date = calendar.getTimeInMillis();
            editItem.image = photo;

            eventsRef.child(id).child("title").setValue(editItem.title);
            eventsRef.child(id).child("location").setValue(editItem.location);
            eventsRef.child(id).child("description").setValue(editItem.description);
            eventsRef.child(id).child("date").setValue(editItem.date);
            eventsRef.child(id).child("image").setValue(editItem.image);
        } else {
            Event eventItem = new Event();
            eventItem.id = id;
            eventItem.title = binding.eventTitle.getEditText().getText().toString().trim();
            eventItem.location = binding.eventLocation.getEditText().getText().toString().trim();
            eventItem.description = binding.description.getEditText().getText().toString().trim();
            eventItem.date = calendar.getTimeInMillis();
            eventItem.image = photo;
            eventsRef.child(id).setValue(eventItem);
        }

        Toast.makeText(
                requireContext(),
                editMode ? "Edited" : "Uploaded" + " Successfully",
                Toast.LENGTH_LONG
        ).show();
        binding.loading.setVisibility(View.GONE);
        Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment).popBackStack();

    }

    private void openPickDate() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view1, year, month, dayOfMonth) -> {

            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (view2, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                binding.eventDate.getEditText().setText(new SimpleDateFormat("'\uD83D\uDCC5' EEEE, dd MMM \uD83D\uDD54 h:mm a", Locale.getDefault()).format(calendar.getTimeInMillis()));
                binding.eventDate.setError(null);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);

            timePickerDialog.show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }


    private void checkPermissionAndOpenGal() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
        } else {
            Intent gal = new Intent(Intent.ACTION_PICK).setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            someActivityResultLauncher.launch(gal);
        }
    }

    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                AtomicBoolean b = new AtomicBoolean(true);
                permissions.forEach((key, value) -> {
                    if (!value) b.set(false);
                });
                if (b.get()) {
                    checkPermissionAndOpenGal();
                }
            });

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    // image from gallery
                    assert data != null;
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        // one image
                        binding.uploadImage.setVisibility(View.GONE);
                        binding.image.setImageURI(imageUri);
                        ImageUri = imageUri;
                        Glide.with(requireContext())
                                .load(ImageUri)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.image);
                    }
                }
            });

}