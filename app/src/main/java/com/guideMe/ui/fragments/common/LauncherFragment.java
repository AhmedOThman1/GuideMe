package com.guideMe.ui.fragments.common;


import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.guideMe.R;

public class LauncherFragment extends Fragment {
    View view;
    FirebaseUser currentUser;
    public static String type;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_launcher, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null || currentUser.getEmail() == null) { // Not login
            FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder().addSharedElement(view.findViewById(R.id.image_splash), "app_img").build();
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_launcherFragment_to_blindCheckFragment, null, null, extras);
        } else {
            new Handler().postDelayed(() -> {
                if (currentUser.getEmail().contains("@manager.")) { // app@manager.com
                    type = "Manager";//TO DO
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_launcherFragment_to_managerMainFragment);
                } else {
                    type = "Helper";//TO DO
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_launcherFragment_to_helperMainFragment);
                }
            }, 800);
        }

        return view;
    }

}

