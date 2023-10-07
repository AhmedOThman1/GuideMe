package com.guideMe.ui.fragments.helper.main;

import static com.guideMe.ui.activities.MainActivity.restartApp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.guideMe.R;
import com.guideMe.databinding.FragmentHelperMainBinding;

import java.util.ArrayList;


public class HelperMainFragment extends Fragment {
    FragmentHelperMainBinding binding;

    public static BottomNavigationView bottomNavigationView;
    public static Toolbar toolbar;
    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference helperRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHelperMainBinding.inflate(inflater, container, false);

        toolbar = binding.toolbar;
        bottomNavigationView = binding.helperBottomNavigation;

        database = FirebaseDatabase.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        helperRef = database.getReference("Helpers").child(firebaseUser.getUid());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int currentFragmentId = Navigation.findNavController(requireActivity(), R.id.nav_helper_host_fragment).getCurrentDestination().getId();
            if (item.getItemId() == R.id.nav_store) {
                if (currentFragmentId != R.id.showStoreFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_helper_host_fragment).navigate(R.id.showStoreFragment);
            } else if (item.getItemId() == R.id.nav_events) {
                if (currentFragmentId != R.id.showEventsFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_helper_host_fragment).navigate(R.id.showEventsFragment);
            } else if (item.getItemId() == R.id.nav_be_my_eyes) {
                if (currentFragmentId != R.id.helperByMyEyesFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_helper_host_fragment).navigate(R.id.helperByMyEyesFragment);
            }  else if (item.getItemId() == R.id.nav_payments) {
                if (currentFragmentId != R.id.showPaymentsFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_helper_host_fragment).navigate(R.id.showPaymentsFragment);
            } else if (item.getItemId() == R.id.nav_profile) {
                if (currentFragmentId != R.id.helperProfileFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_helper_host_fragment).navigate(R.id.helperProfileFragment);
            } else if (item.getItemId() == R.id.nav_share_app) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("*/*");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing app");
                // text
                shareIntent.putExtra(Intent.EXTRA_TEXT, "" + getString(R.string.share_text));
                //image
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("android.resource://" + requireActivity().getPackageName() +
                        "/drawable/" + "share_banner"));
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Sharing " + getString(R.string.app_name) + " App"));
                return false;
            }
//            else if (item.getItemId() == R.id.nav_logout) {
//                logout(requireContext());
//                return false;
//            }
            return true;
        });


        toolbar.getMenu().clear();
        toolbar.getMenu().add("Logout");
        toolbar.getOverflowIcon().setTint(requireActivity().getColor(R.color.white));
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Logout")) {
                FirebaseAuth.getInstance().signOut();
                new Handler().postDelayed(() -> restartApp(requireActivity()), 1000);
                return true;
            }
            return false;
        });

        getCurrentToken();


        return binding.getRoot();
    }


    private void getCurrentToken() {
        // Get token
        // [START log_reg_token]
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("TAG", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    // Get new FCM registration token
                    String token = task.getResult();
                    helperRef.child("token").setValue(token);
                    // Log and toast
                });
        // [END log_reg_token]
    }
}
