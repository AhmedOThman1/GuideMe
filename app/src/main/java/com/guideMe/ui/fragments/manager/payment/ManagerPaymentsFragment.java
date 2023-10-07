package com.guideMe.ui.fragments.manager.payment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.guideMe.R;
import com.guideMe.adapters.PaymentsAdapter;
import com.guideMe.databinding.FragmentManagerPaymentsBinding;
import com.guideMe.pojo.Donation;
import com.guideMe.pojo.Payment;
import com.guideMe.ui.fragments.manager.main.ManagerMainFragment;

import java.util.ArrayList;
import java.util.Comparator;

public class ManagerPaymentsFragment extends Fragment {

    FragmentManagerPaymentsBinding binding;
    FirebaseDatabase database;
    DatabaseReference paymentsRef;
    ArrayList<Payment> payments = new ArrayList<>();
    ArrayList<Payment> tempPayments = new ArrayList<>();
    PaymentsAdapter paymentsAdapter;

    public ManagerPaymentsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentManagerPaymentsBinding.inflate(inflater, container, false);

        ManagerMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_payments);
        ManagerMainFragment.toolbar.setTitle(getString(R.string.payments));

        database = FirebaseDatabase.getInstance();
        paymentsRef = database.getReference("Payment");

        paymentsAdapter = new PaymentsAdapter(requireContext());
        paymentsAdapter.setModels(payments);
        binding.paymentsRecycler.setAdapter(paymentsAdapter);
        binding.paymentsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tempPayments = new ArrayList<>();
                String query = s.toString().trim().toLowerCase();

                if (query.isEmpty()) {
                    tempPayments = new ArrayList<>();
                    tempPayments.addAll(payments);
                } else for (int i = 0; i < payments.size(); i++) {
                    if (payments.get(i).helper.name.toLowerCase().contains(query))
                        tempPayments.add(payments.get(i));
                }

                binding.noResultDesign.noResultDesign.setVisibility(tempPayments.isEmpty() && !payments.isEmpty() ? View.VISIBLE : View.GONE);

                paymentsAdapter.setModels(tempPayments);
                paymentsAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        getPayments();


        return binding.getRoot();
    }

    private void getPayments() {
        // Read from the database
        paymentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                payments = new ArrayList<>();
                tempPayments = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Payment product = dataSnapshot.getValue(Payment.class);
                    payments.add(product);
                }
                payments.sort(Comparator.comparing(donation -> donation.date,Comparator.reverseOrder()));
                tempPayments.addAll(payments);
                paymentsAdapter.setModels(tempPayments);
                binding.emptyStates.setVisibility(tempPayments.isEmpty() ? View.VISIBLE : View.GONE);
                binding.search.setVisibility(tempPayments.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                paymentsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This callback will only be called when MyFragment is at least Started.
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event

                if (!binding.search.getText().toString().trim().isEmpty())
                    binding.search.setText("");
                else
                    Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment)
                            .popBackStack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

    }
}