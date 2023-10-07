package com.guideMe.ui.fragments.manager.store.add;

import static com.guideMe.ui.activities.MainActivity.openKeyboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.guideMe.R;
import com.guideMe.databinding.FragmentManagerAddProductBinding;
import com.guideMe.pojo.Product;
import com.guideMe.ui.activities.MainActivity;
import com.guideMe.ui.fragments.manager.main.ManagerMainFragment;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManagerAddProductFragment extends Fragment {

    FragmentManagerAddProductBinding binding;

    FirebaseDatabase database;
    DatabaseReference productsRef;
    StorageReference productsStorageRef;
    Product editItem;
    boolean editMode;
    Uri ImageUri;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentManagerAddProductBinding.inflate(inflater, container, false);


        database = FirebaseDatabase.getInstance();
        productsRef = database.getReference("Products");
        productsStorageRef = FirebaseStorage.getInstance().getReference("Products");

        Bundle args = getArguments();
        if (args != null) {
            String json = args.getString("item", "");
            if (!json.isEmpty()) {
                editItem = new Gson().fromJson(json, Product.class);
                binding.uploadImage.setVisibility(View.GONE);
                binding.productName.getEditText().setText(editItem.name.trim());
                ((AutoCompleteTextView) binding.category.getEditText()).setText(editItem.category,false);
                binding.productPrice.getEditText().setText(String.valueOf(editItem.price));
                binding.productQuantity.getEditText().setText(String.valueOf(editItem.quantity));
                binding.description.getEditText().setText(editItem.description);
                editMode = true;

                Glide.with(requireContext())
                        .load(editItem.image)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .into(binding.productImage);

                photo = editItem.image;
                ImageUri = Uri.parse(editItem.image);
                binding.uploadProduct.setText("Edit product");
                ManagerMainFragment.toolbar.setTitle("Edit product");
            }

        }

        binding.uploadImage.setOnClickListener(v -> checkPermissionAndOpenGal());
        binding.productImage.setOnClickListener(v -> checkPermissionAndOpenGal());

        binding.uploadProduct.setOnClickListener(v -> {
            if (ImageUri == null) {
                Toast.makeText(requireContext(), "Pick image first", Toast.LENGTH_SHORT).show();
            } else if (binding.productName.getEditText().getText().toString().trim().isEmpty()) {
                binding.productName.setError(getString(R.string.empty));
                binding.productName.requestFocus();
                openKeyboard(binding.productName.getEditText(), requireActivity());
            } else if (binding.category.getEditText().getText().toString().trim().isEmpty()) {
                binding.productName.setError(null);
                binding.category.setError(getString(R.string.empty));
                binding.category.requestFocus();
                openKeyboard(binding.category.getEditText(), requireActivity());
            } else if (binding.productPrice.getEditText().getText().toString().trim().isEmpty()) {
                binding.productName.setError(null);
                binding.category.setError(null);
                binding.productPrice.setError(getString(R.string.empty));
                binding.productPrice.requestFocus();
                openKeyboard(binding.productPrice.getEditText(), requireActivity());
            } else if (binding.productQuantity.getEditText().getText().toString().trim().isEmpty()) {
                binding.productName.setError(null);
                binding.category.setError(null);
                binding.productPrice.setError(null);
                binding.productQuantity.setError(getString(R.string.empty));
                binding.productQuantity.requestFocus();
                openKeyboard(binding.productQuantity.getEditText(), requireActivity());
            } else if (binding.description.getEditText().getText().toString().trim().isEmpty()) {
                binding.productName.setError(null);
                binding.category.setError(null);
                binding.productPrice.setError(null);
                binding.productQuantity.setError(null);
                binding.description.setError(getString(R.string.empty));
                binding.description.requestFocus();
                openKeyboard(binding.description.getEditText(), requireActivity());
            } else {
                binding.productName.setError(null);
                binding.category.setError(null);
                binding.productPrice.setError(null);
                binding.productQuantity.setError(null);
                binding.description.setError(null);

                binding.loading.setVisibility(View.VISIBLE);
                binding.uploadProduct.setEnabled(false);
                id = (editMode ? editItem.id : productsRef.push().getKey());
                if (editMode && ImageUri.equals(Uri.parse(photo))) {
                    uploadData();
                } else
                    uploadImg();
            }

        });

        getProducts();

        return binding.getRoot();
    }

    String id, photo;

    private void uploadImg() {
        productsStorageRef.child(id).putFile(ImageUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                productsStorageRef.child(id).getDownloadUrl().addOnSuccessListener(uri -> {
                    photo = uri.toString();
                    uploadData();
                });
            } else {
                binding.loading.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to upload image! Try again.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            binding.loading.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Failed to upload image! Try again." + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadData() {
        if (editMode) {
            editItem.name = (binding.productName.getEditText().getText().toString().trim());
            editItem.category = (binding.category.getEditText().getText().toString().trim());
            editItem.price = (Double.parseDouble(binding.productPrice.getEditText().getText().toString().trim()));
            editItem.quantity = (Integer.parseInt(binding.productQuantity.getEditText().getText().toString().trim()));
            editItem.description = (binding.description.getEditText().getText().toString().trim());
            editItem.image = photo;

            productsRef.child(id).child("image").setValue(editItem.image);
            productsRef.child(id).child("name").setValue(editItem.name);
            productsRef.child(id).child("category").setValue(editItem.category);
            productsRef.child(id).child("price").setValue(editItem.price);
            productsRef.child(id).child("quantity").setValue(editItem.quantity);
            productsRef.child(id).child("description").setValue(editItem.description);
        } else {
            Product item = new Product();
            item.id = id;
            item.image = photo;
            item.name = (binding.productName.getEditText().getText().toString().trim());
            item.category = (binding.category.getEditText().getText().toString().trim());
            item.price = (Double.parseDouble(binding.productPrice.getEditText().getText().toString().trim()));
            item.quantity = (Integer.parseInt(binding.productQuantity.getEditText().getText().toString().trim()));
            item.description = (binding.description.getEditText().getText().toString().trim());
            productsRef.child(id).setValue(item);
        }

        Toast.makeText(requireContext(), editMode ? "Updated successfully!" : "Added successfully!", Toast.LENGTH_SHORT).show();
        binding.loading.setVisibility(View.GONE);
        binding.uploadProduct.setEnabled(true);
        Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment).popBackStack();

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
                        binding.productImage.setImageURI(imageUri);
                        ImageUri = imageUri;
                        Glide.with(requireContext())
                                .load(ImageUri)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.productImage);
                    }
                }
            });


    ArrayList<String> categories = new ArrayList<>();

    private void getProducts() {
        // Read from the database
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Product product = dataSnapshot.getValue(Product.class);
                    categories.add(product.category);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                ((AutoCompleteTextView) binding.category.getEditText()).setAdapter(adapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}