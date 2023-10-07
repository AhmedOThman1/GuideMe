package com.guideMe.ui.fragments.auth;

import static com.guideMe.ui.activities.MainActivity.close_keyboard;
import static com.guideMe.ui.activities.MainActivity.openKeyboard;

import android.os.Bundle;
import android.transition.TransitionInflater;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.guideMe.R;
import com.guideMe.databinding.FragmentLoginBinding;
import com.guideMe.ui.fragments.common.LauncherFragment;


public class LoginFragment extends Fragment {

    FragmentLoginBinding binding;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    FirebaseDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        binding.login.setOnClickListener(v -> {
            if (binding.email.getEditText().getText().toString().trim().isEmpty()) {
                binding.email.setError(getResources().getString(R.string.empty));
                binding.email.requestFocus();
                openKeyboard(binding.email.getEditText(), requireActivity());
            } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.email.getEditText().getText().toString().trim())
                    .matches()) {
                binding.email.setError(getResources().getString(R.string.email_not_valid));
                binding.email.requestFocus();
                openKeyboard(binding.email.getEditText(), requireActivity());
            } else if (binding.password.getEditText().getText().toString().trim().isEmpty()) {
                binding.email.setError(null);
                binding.password.setError(getResources().getString(R.string.empty));
                binding.password.requestFocus();
                openKeyboard(binding.password.getEditText(), requireActivity());
            } else {
                binding.email.setError(null);
                binding.password.setError(null);
                binding.loading.setVisibility(View.VISIBLE);
                binding.login.setEnabled(false);
                login(binding.email.getEditText().getText().toString().trim(), binding.password.getEditText().getText().toString().trim());
            }
        });

        binding.signUp.setOnClickListener(v ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.action_loginFragment_to_signUpFragment));

        binding.forgottenPassword.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("email", binding.email.getEditText().getText().toString().trim());
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_resetPasswordFragment, bundle);
        });


        return binding.getRoot();
    }


    private void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                        requireActivity(), task -> {
                            if (task.isSuccessful()) {
                                binding.loading.setVisibility(View.GONE);
                                // Sign in success, update UI with the signed-in user's information
                                currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                close_keyboard(requireActivity());
                                if (email.contains("@manager.")) { // app@manager.com
                                    LauncherFragment.type = "Manager";//TO DO
                                    Navigation.findNavController(binding.getRoot()).navigate(R.id.action_loginFragment_to_managerMainFragment);
                                } else {       // other
                                    LauncherFragment.type = "Helper";//TO DO
                                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_loginFragment_to_helperMainFragment);
                                }
                            } else {
                                // If sign in fails, display a message to the user.
                                binding.loading.setVisibility(View.GONE);
                                binding.login.setEnabled(true);
                                Log.w("Login", "signInWithEmail:failure", task.getException());
                                Toast.makeText(
                                        requireContext(), "Email or binding.password is incorrect.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementEnterTransition(TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move));
    }

}
