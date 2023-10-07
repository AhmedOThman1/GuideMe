package com.guideMe.ui.fragments.helper.profile;

import static com.guideMe.ui.activities.MainActivity.close_keyboard;
import static com.guideMe.ui.activities.MainActivity.openKeyboard;
import static com.guideMe.ui.activities.MainActivity.restartApp;
import static com.guideMe.ui.activities.MainActivity.space;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.guideMe.R;
import com.guideMe.databinding.FragmentHelperProfileBinding;
import com.guideMe.pojo.Helper;
import com.guideMe.pojo.PaymentCardInfo;
import com.guideMe.ui.fragments.helper.main.HelperMainFragment;

import java.util.concurrent.atomic.AtomicBoolean;

import de.hdodenhof.circleimageview.CircleImageView;

public class HelperProfileFragment extends Fragment {
    FragmentHelperProfileBinding binding;
    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference currentUserRef;
    StorageReference usersStorageRef;
    Helper currentUserItem;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentHelperProfileBinding.inflate(inflater, container, false);

        HelperMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        HelperMainFragment.toolbar.setTitle(getString(R.string.profile));

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        currentUserRef = database.getReference("Helpers").child(firebaseUser.getUid());
        usersStorageRef = FirebaseStorage.getInstance().getReference("Helpers");

        getCurrentUser();

        binding.camBg.setOnClickListener(v -> showEditPhotoDialog());
        binding.userImg.setOnClickListener(v -> showEditPhotoDialog());

        binding.nameCard.setOnClickListener(v -> showEditNameDialog());
        binding.phoneCard.setOnClickListener(v -> showEditPhoneDialog());
        binding.passwordCard.setOnClickListener(v -> showEditPasswordDialog());
        binding.moneyCard.setOnClickListener(v -> showEditCreditCardDialog());
        binding.logoutCard.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            new Handler().postDelayed(() -> restartApp(requireActivity()), 1000);
        });
        return binding.getRoot();
    }


    private void getCurrentUser() {
        // Read from the database
        currentUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                currentUserItem = snapshot.getValue(Helper.class);
                assert currentUserItem != null;

                ImageUri = Uri.parse(currentUserItem.photo);
                Glide.with(requireContext())
                        .load(currentUserItem.photo)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .into(binding.userImg);

                binding.name.setText(currentUserItem.name);
                binding.phone.setText(currentUserItem.phone);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    AlertDialog dialog;
    TextInputLayout current_password;
    TextInputLayout new_password;

    private void showEditPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_chanage_password, null);
        current_password = view.findViewById(R.id.current_password);
        new_password = view.findViewById(R.id.new_password);


        view.findViewById(R.id.change_password).setOnClickListener(v -> {
            if (current_password.getEditText().getText().toString().trim().isEmpty()) {
                current_password.setError(getString(R.string.empty));
                current_password.requestFocus();
                openKeyboard(current_password.getEditText(), requireActivity());
            } else if (new_password.getEditText().getText().toString().trim().isEmpty()) {
                current_password.setError(null);
                new_password.setError(getString(R.string.empty));
                new_password.requestFocus();
                openKeyboard(new_password.getEditText(), requireActivity());
            } else if (new_password.getEditText().getText().toString().trim().length() < 6) {
                current_password.setError(null);
                new_password.setError(getString(R.string.can_not_be_less_than_6));
                new_password.requestFocus();
                openKeyboard(new_password.getEditText(), requireActivity());
            } else {
                new_password.setError(null);
                current_password.setError(null);

                AuthCredential authCredential = EmailAuthProvider.getCredential(currentUserItem.email, current_password.getEditText().getText().toString().trim());

                firebaseUser.reauthenticate(authCredential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        firebaseUser.updatePassword(new_password.getEditText().getText().toString().trim()).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                close_keyboard(requireActivity());
                                Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(requireContext(), "Failed, try again!", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Failed, try again!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed, try again!\nmake sure that the current password is correct", Toast.LENGTH_SHORT).show();
                });


            }
        });

        builder.setView(view);
        dialog = builder.create();
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    AlertDialog alertDialog;
    TextInputLayout full_name;

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_chanage_name, null);
        full_name = view.findViewById(R.id.full_name);

        full_name.getEditText().setText(currentUserItem.name);

        view.findViewById(R.id.change_name).setOnClickListener(v -> {

            if (full_name.getEditText().getText().toString().trim().isEmpty()) {
                full_name.setError(getString(R.string.empty));
                full_name.requestFocus();
                openKeyboard(full_name.getEditText(), requireActivity());
            } else {
                full_name.setError(null);
                UserProfileChangeRequest profileUpdates =
                        new UserProfileChangeRequest.Builder()
                                .setDisplayName(full_name.getEditText().getText().toString().trim())
                                .build();

                firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(task1 -> {
                    currentUserRef.child("name").setValue(full_name.getEditText().getText().toString().trim());
                    Toast.makeText(requireContext(), "Your Name has been modified successfully", Toast.LENGTH_SHORT).show();
                    close_keyboard(requireActivity());
                    alertDialog.dismiss();
                });
            }
        });

        builder.setView(view);
        alertDialog = builder.create();
        alertDialog.show();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void showEditPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_chanage_phone, null);
        TextInputLayout phone = view.findViewById(R.id.phone);

        phone.getEditText().setText(currentUserItem.phone);

        view.findViewById(R.id.change_phone).setOnClickListener(v -> {
            if (phone.getEditText().getText().toString().trim().isEmpty()) {
                phone.setError(getString(R.string.empty));
                phone.requestFocus();
                openKeyboard(phone.getEditText(), requireActivity());
            } else {
                phone.setError(null);
                currentUserRef.child("phone").setValue(phone.getEditText().getText().toString().trim());
                Toast.makeText(requireContext(), "Your phone has been modified successfully", Toast.LENGTH_SHORT).show();
                close_keyboard(requireActivity());
                alertDialog.dismiss();
            }
        });

        builder.setView(view);
        alertDialog = builder.create();
        alertDialog.show();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    TextInputLayout card_no, month, year, cvv;
    private void showEditCreditCardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.dialog_user_edit_credit_card, null);

        card_no = dialogView.findViewById(R.id.card_no);
        month = dialogView.findViewById(R.id.month);
        year = dialogView.findViewById(R.id.year);
        cvv = dialogView.findViewById(R.id.cvv);

        if (currentUserItem.paymentCardInfo != null) {
            card_no.getEditText().setText(currentUserItem.paymentCardInfo.getCard_number());
            month.getEditText().setText(currentUserItem.paymentCardInfo.getExpiration_month());
            year.getEditText().setText(currentUserItem.paymentCardInfo.getExpiration_year());
            cvv.getEditText().setText(currentUserItem.paymentCardInfo.getCvv());
        }

        card_no.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && (s.length() % 5) == 0) {
                    Log.w("TAG", "afterTextChanged: " + s.length() + "," + (s.length() % 5));
                    final char c = s.charAt(s.length() - 1);
                    if (space == c) {
                        // Remove spacing char
                        s.delete(s.length() - 1, s.length());
                    } else
                        // Insert char where needed.
                        s.insert(s.length() - 1, String.valueOf(space));
                }
            }
        });

        month.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 1 && Integer.parseInt(editable.toString()) > 1) {
                    editable.insert(editable.length() - 1, "0");
                    year.requestFocus();
                }

            }
        });


        dialogView.findViewById(R.id.edit_credit_card).setOnClickListener(v -> {
            if (card_no.getEditText().getText().toString().trim().isEmpty()) {
                card_no.setError(getString(R.string.empty));
                card_no.requestFocus();
                openKeyboard(card_no.getEditText(), requireActivity());
            } else if (card_no.getEditText().getText().toString().trim().length() != 19) {
                card_no.setError(getString(R.string.card_error));
                card_no.requestFocus();
                openKeyboard(card_no.getEditText(), requireActivity());
            } else if (month.getEditText().getText().toString().trim().isEmpty()) {
                card_no.setError(null);
                month.setError(getString(R.string.empty));
                month.requestFocus();
                openKeyboard(month.getEditText(), requireActivity());
            } else if (year.getEditText().getText().toString().trim().isEmpty()) {
                card_no.setError(null);
                month.setError(null);
                year.setError(getString(R.string.empty));
                year.requestFocus();
                openKeyboard(year.getEditText(), requireActivity());
            } else if (cvv.getEditText().getText().toString().trim().isEmpty()) {
                card_no.setError(null);
                month.setError(null);
                year.setError(null);
                cvv.setError(getString(R.string.empty));
                cvv.requestFocus();
                openKeyboard(cvv.getEditText(), requireActivity());
            } else {
                card_no.setError(null);
                month.setError(null);
                year.setError(null);
                cvv.setError(null);

                // payment info
                PaymentCardInfo paymentCardInfo = new PaymentCardInfo();
                paymentCardInfo.setCard_number(card_no.getEditText().getText().toString().trim());
                paymentCardInfo.setExpiration_month(month.getEditText().getText().toString().trim());
                paymentCardInfo.setExpiration_year(year.getEditText().getText().toString().trim());
                paymentCardInfo.setCvv(cvv.getEditText().getText().toString().trim());


                currentUserRef.child("paymentCardInfo").setValue(paymentCardInfo);

                Toast.makeText(requireContext(), "Credit card edited successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        builder.setView(dialogView);
        dialog = builder.create();
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    AlertDialog alert;
    CircleImageView dialog_user_img;
    ProgressBar loading;

    private void showEditPhotoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialog_view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_chanage_photo, null);
        dialog_user_img = dialog_view.findViewById(R.id.user_img);
        loading = dialog_view.findViewById(R.id.loading);

        Glide.with(requireContext())
                .load(ImageUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(dialog_user_img);

        dialog_user_img.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(requireContext(), dialog_user_img);
            popupMenu.getMenu().add("Change Image");
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Change Image")) {
                    checkPermissionAndOpenGal();
                }
                return true;
            });
            popupMenu.show();

        });


        dialog_view.findViewById(R.id.change_photo).setOnClickListener(v -> changePhoto());

        builder.setView(dialog_view);
        alert = builder.create();
        alert.show();
        alert.setCanceledOnTouchOutside(false);
        alert.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    Uri ImageUri;

    private void changePhoto() {
        loading.setVisibility(View.VISIBLE);
        usersStorageRef.child(currentUserItem.id).putFile(ImageUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                usersStorageRef.child(currentUserItem.id).getDownloadUrl().addOnSuccessListener(uri -> {
                            UserProfileChangeRequest profileUpdates =
                                    new UserProfileChangeRequest.Builder()
                                            .setPhotoUri(uri).build();

                            firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(task1 -> {
                                currentUserRef.child("photo").setValue(uri.toString());
                                Toast.makeText(requireContext(), "Profile image has been modified successfully", Toast.LENGTH_SHORT).show();
                                loading.setVisibility(View.GONE);
                                alert.dismiss();
                            });
                        }
                ).addOnFailureListener(e -> {
                    Toast.makeText(
                            requireContext(),
                            "Failed to upload ! Try again." + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    loading.setVisibility(View.GONE);
                });
            }
        }).addOnProgressListener(snapshot -> {
            loading.setVisibility(View.VISIBLE);
        }).continueWith(task -> {
            loading.setVisibility(View.GONE);
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
                        ImageUri = imageUri;

                        Glide.with(requireContext())
                                .load(ImageUri)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(dialog_user_img);
                    }
                }
            });


}
