package com.guideMe.ui.fragments.manager.main;

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
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.guideMe.R;
import com.guideMe.databinding.FragmentManagerMainBinding;

import java.util.ArrayList;


public class ManagerMainFragment extends Fragment {
    FragmentManagerMainBinding binding;

    public static BottomNavigationView bottomNavigationView;
    public static Toolbar toolbar;
    FirebaseDatabase database;
    DatabaseReference managerTokensRef;
    ArrayList<String> managerTokens = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentManagerMainBinding.inflate(inflater, container, false);

        toolbar = binding.toolbar;
        bottomNavigationView = binding.bottomNavigation;

        database = FirebaseDatabase.getInstance();
        managerTokensRef = database.getReference("App Manager Tokens");

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int currentFragmentId = Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment).getCurrentDestination().getId();
            if (item.getItemId() == R.id.nav_store) {
                if (currentFragmentId != R.id.managerStoreFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment).navigate(R.id.managerStoreFragment);
            } else if (item.getItemId() == R.id.nav_events) {
                if (currentFragmentId != R.id.managerEventsFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment).navigate(R.id.managerEventsFragment);
            } else if (item.getItemId() == R.id.nav_payments) {
                if (currentFragmentId != R.id.managerPaymentsFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment).navigate(R.id.managerPaymentsFragment);
            } else if (item.getItemId() == R.id.nav_donations) {
                if (currentFragmentId != R.id.managerDonationsFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment).navigate(R.id.managerDonationsFragment);
            } else if (item.getItemId() == R.id.nav_share_app) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.setType("*/*");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing app");
                // text
                shareIntent.putExtra(Intent.EXTRA_TEXT, "" + getString(R.string.share_text));
                //image
                shareIntent.putExtra(Intent.EXTRA_STREAM,  Uri.parse("android.resource://" + requireActivity().getPackageName() +
                        "/" + R.drawable.share_banner));
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Sharing "+getString(R.string.app_name)+" App"));
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

        getManagerTokens();

        return binding.getRoot();
    }

    void getManagerTokens() {
        managerTokensRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                managerTokens = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String token = dataSnapshot.getValue(String.class);
                    managerTokens.add(token);
                }
                getManagerCurrentToken();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    private void getManagerCurrentToken() {
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
                    if (!managerTokens.contains(token)) {
                        String child = managerTokensRef.push().getKey();
                        assert child != null;
                        managerTokensRef.child(child).setValue(token);
                    }
                    // Log and toast
                });
        // [END log_reg_token]
    }
}
