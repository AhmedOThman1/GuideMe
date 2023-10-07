package com.guideMe.ui.fragments.auth;

import static com.guideMe.ui.activities.MainActivity.close_keyboard;
import static com.guideMe.ui.activities.MainActivity.openKeyboard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.guideMe.R;
import com.guideMe.databinding.FragmentSignUpBinding;
import com.guideMe.pojo.Helper;
import com.guideMe.ui.fragments.common.LauncherFragment;

import java.util.concurrent.atomic.AtomicBoolean;

public class SignUpFragment extends Fragment {

    FragmentSignUpBinding binding;

    FirebaseDatabase database;
    DatabaseReference helpersRef;
    StorageReference helpersStorageRef;
    FirebaseAuth mAuth;

    Uri ImageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSignUpBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        helpersRef = database.getReference("Helpers");
        helpersStorageRef = FirebaseStorage.getInstance().getReference("Helpers");

        binding.signUp.setOnClickListener(v -> {
            if (binding.fullName.getEditText().getText().toString().trim().isEmpty()) {
                binding.fullName.setError(getResources().getString(R.string.empty));
                binding.fullName.requestFocus();
                openKeyboard(binding.fullName.getEditText(), requireActivity());
            } else if (binding.phone.getEditText().getText().toString().trim().isEmpty()) {
                binding.fullName.setError(null);
                binding.phone.setError(getResources().getString(R.string.empty));
                binding.phone.requestFocus();
                openKeyboard(binding.phone.getEditText(), requireActivity());
            } else if (!Patterns.PHONE.matcher(binding.phone.getEditText().getText().toString().trim())
                    .matches()) {
                binding.fullName.setError(null);
                binding.phone.setError(getResources().getString(R.string.phone_not_valid));
                binding.phone.requestFocus();
                openKeyboard(binding.phone.getEditText(), requireActivity());
            } else if (binding.email.getEditText().getText().toString().trim().isEmpty()) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(getResources().getString(R.string.empty));
                binding.email.requestFocus();
                openKeyboard(binding.email.getEditText(), requireActivity());
            } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.email.getEditText().getText().toString().trim())
                    .matches()) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(getResources().getString(R.string.email_not_valid));
                binding.email.requestFocus();
                openKeyboard(binding.email.getEditText(), requireActivity());
            } else if (binding.password.getEditText().getText().toString().trim().isEmpty()) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(null);
                binding.password.setError(getResources().getString(R.string.empty));
                binding.password.requestFocus();
                openKeyboard(binding.password.getEditText(), requireActivity());
            } else if (binding.password.getEditText().getText().toString().trim().length() < 6) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(null);
                binding.password.setError(getResources().getString(R.string.can_not_be_less_than_6));
                binding.password.requestFocus();
                openKeyboard(binding.password.getEditText(), requireActivity());
            } else if (ImageUri == null) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(null);
                binding.password.setError(null);
                Toast.makeText(
                        requireContext(),
                        "Upload image first",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(null);
                binding.password.setError(null);
                binding.loading.setVisibility(View.VISIBLE);
                binding.signUp.setEnabled(false);
                close_keyboard(requireActivity());
                createNewAccount();
            }
        });


        binding.userImg.setOnClickListener(v -> checkPermissionAndOpenGal());

        binding.backLayout.setOnClickListener(v ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .popBackStack());

        return binding.getRoot();
    }

    FirebaseUser user;

    private void createNewAccount() {
        mAuth.createUserWithEmailAndPassword(
                        binding.email.getEditText().getText().toString().trim()
                        , binding.password.getEditText().getText().toString().trim())
                .addOnCompleteListener(
                        requireActivity(), task -> {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                user = mAuth.getCurrentUser();
                                ///
                                assert user != null;
                                uploadImg();
                                ///
                            } else {
                                // If sign in fails, display a message to the user.
                                binding.loading.setVisibility(View.GONE);
                                binding.signUp.setEnabled(true);
                                Toast.makeText(
                                        requireContext(), "Authentication failed.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
    }

    private void uploadImg() {
        helpersStorageRef.child(user.getUid()).putFile(ImageUri).addOnCompleteListener(task2 -> {
            if (task2.isSuccessful()) {
                helpersStorageRef.child(user.getUid()).getDownloadUrl().addOnSuccessListener(uri -> {
                    UserProfileChangeRequest profileUpdates =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(binding.fullName.getEditText().getText().toString().trim())
                                    .setPhotoUri(uri).build();
                    user.updateProfile(profileUpdates).addOnCompleteListener(
                            task1 -> uploadUserData(uri)
                    );

                }).addOnFailureListener(e -> {
                    Toast.makeText(
                            requireContext(),
                            "Failed to upload ! Try again." + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    binding.loading.setVisibility(View.GONE);
                });
            }
        }).addOnProgressListener(snapshot -> {
            binding.loading.setVisibility(View.VISIBLE);
        }).continueWith(task3 -> {
            binding.loading.setVisibility(View.GONE);
            binding.signUp.setEnabled(true);
            if (!task3.isSuccessful()) {
                Toast.makeText(
                        requireContext(),
                        "Failed to upload! Try again.",
                        Toast.LENGTH_LONG
                ).show();
            }
            return task3;
        });
    }

    private void uploadUserData(Uri uri) {
        Helper userItem = new Helper();
        userItem.id = (user.getUid());
        userItem.name = (binding.fullName.getEditText().getText().toString().trim());
        userItem.phone = (binding.phone.getEditText().getText().toString().trim());
        userItem.email = (binding.email.getEditText().getText().toString().trim());
        userItem.photo = (uri.toString());
        helpersRef.child(userItem.id).setValue(userItem);
        LauncherFragment.type = "Helper";
        binding.loading.setVisibility(View.GONE);//TO DO
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.action_signUpFragment_to_helperMainFragment);
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
                        binding.userImg.setImageURI(imageUri);
                        ImageUri = imageUri;
                    }
                }
            });

}
