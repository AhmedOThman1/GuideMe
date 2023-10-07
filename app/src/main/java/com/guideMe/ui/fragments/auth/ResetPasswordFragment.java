package com.guideMe.ui.fragments.auth;

import static com.guideMe.ui.activities.MainActivity.openKeyboard;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.guideMe.R;

public class ResetPasswordFragment extends Fragment {
    View view;

    TextInputLayout email;
    ProgressBar loading;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_reset_password, container, false);

        email = view.findViewById(R.id.email);
        loading = view.findViewById(R.id.loading);

        email.requestFocus();
        openKeyboard(email.getEditText(), requireActivity());

        Bundle arg = getArguments();
        if (arg != null) {
            String emailText = arg.getString("email", "");
            if (!emailText.isEmpty()) {
                email.getEditText().setText(emailText.trim());
            }
        }

        view.findViewById(R.id.send_instructions).setOnClickListener(v -> {
            loading.setVisibility(View.VISIBLE);
            if (email.getEditText().getText().toString().trim().isEmpty()) {
                email.setError(getResources().getString(R.string.empty));
                email.requestFocus();
                openKeyboard(email.getEditText(), requireActivity());
                loading.setVisibility(View.GONE);
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email.getEditText().getText().toString().trim())
                    .matches()) {
                email.setError(getResources().getString(R.string.email_not_valid));
                email.requestFocus();
                openKeyboard(email.getEditText(), requireActivity());
                loading.setVisibility(View.GONE);
            } else {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email.getEditText().getText().toString().trim())
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                loading.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "An email has been sent. Please check your inbox.", Toast.LENGTH_SHORT).show();
                            } else {
                                loading.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "An error has been occurred.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            loading.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        view.findViewById(R.id.back_layout).setOnClickListener(v ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .popBackStack());

        return view;
    }

}
