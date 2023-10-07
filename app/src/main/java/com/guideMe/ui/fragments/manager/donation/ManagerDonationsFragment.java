package com.guideMe.ui.fragments.manager.donation;

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
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.guideMe.R;
import com.guideMe.adapters.DonationsAdapter;
import com.guideMe.adapters.ProductsAdapter;
import com.guideMe.databinding.FragmentManagerDonationsBinding;
import com.guideMe.pojo.Donation;
import com.guideMe.ui.fragments.manager.main.ManagerMainFragment;

import java.util.ArrayList;
import java.util.Comparator;

public class ManagerDonationsFragment extends Fragment {

    FragmentManagerDonationsBinding binding;
    FirebaseDatabase database;
    DatabaseReference donationsRef;
    ArrayList<Donation> donations = new ArrayList<>();
    ArrayList<Donation> tempDonations = new ArrayList<>();
    DonationsAdapter donationsAdapter;

    public ManagerDonationsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentManagerDonationsBinding.inflate(inflater, container, false);

        ManagerMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_donations);
        ManagerMainFragment.toolbar.setTitle(getString(R.string.donations));

        database = FirebaseDatabase.getInstance();
        donationsRef = database.getReference("Donations");

        donationsAdapter = new DonationsAdapter(requireContext());
        donationsAdapter.setModels(donations);
        binding.donationsRecycler.setAdapter(donationsAdapter);
        binding.donationsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tempDonations = new ArrayList<>();
                String query = s.toString().trim().toLowerCase();

                if (query.isEmpty()) {
                    tempDonations = new ArrayList<>();
                    tempDonations.addAll(donations);
                } else for (int i = 0; i < donations.size(); i++) {
                    if (donations.get(i).helper.name.toLowerCase().contains(query))
                        tempDonations.add(donations.get(i));
                }

                binding.noResultDesign.noResultDesign.setVisibility(tempDonations.isEmpty() && !donations.isEmpty() ? View.VISIBLE : View.GONE);

                donationsAdapter.setModels(tempDonations);
                donationsAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        getDonations();


        return binding.getRoot();
    }

    private void getDonations() {
        // Read from the database
        donationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                donations = new ArrayList<>();
                tempDonations = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Donation product = dataSnapshot.getValue(Donation.class);
                    donations.add(product);
                }
                donations.sort(Comparator.comparing(donation -> donation.date,Comparator.reverseOrder()));
                tempDonations.addAll(donations);
                donationsAdapter.setModels(tempDonations);
                binding.emptyStates.setVisibility(tempDonations.isEmpty() ? View.VISIBLE : View.GONE);
                binding.search.setVisibility(tempDonations.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                donationsAdapter.notifyDataSetChanged();
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