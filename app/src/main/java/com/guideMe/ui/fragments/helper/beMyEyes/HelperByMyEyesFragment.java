package com.guideMe.ui.fragments.helper.beMyEyes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.guideMe.R;
import com.guideMe.adapters.RequestsAdapter;
import com.guideMe.databinding.FragmentHelperBeMyEyesBinding;
import com.guideMe.models.RecyclerViewTouchListener;
import com.guideMe.pojo.Helper;
import com.guideMe.pojo.VideoCallRequest;
import com.guideMe.ui.activities.LiveActivity;
import com.guideMe.ui.fragments.helper.main.HelperMainFragment;

import java.util.ArrayList;
import java.util.Comparator;

public class HelperByMyEyesFragment extends Fragment {

    FragmentHelperBeMyEyesBinding binding;
    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference beMyEyesRef, currentUserRef;
    ArrayList<VideoCallRequest> requests = new ArrayList<>();
    ArrayList<VideoCallRequest> tempRequests = new ArrayList<>();
    RequestsAdapter requestsAdapter;

    public HelperByMyEyesFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentHelperBeMyEyesBinding.inflate(inflater, container, false);


        HelperMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_be_my_eyes);
        HelperMainFragment.toolbar.setTitle(getString(R.string.be_my_eyes));

        database = FirebaseDatabase.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        beMyEyesRef = database.getReference("Be My Eyes");
        currentUserRef = database.getReference("Helpers").child(firebaseUser.getUid());

        requestsAdapter = new RequestsAdapter(requireContext());
        requestsAdapter.setModels(requests);
        binding.blindsRecycler.setAdapter(requestsAdapter);
        binding.blindsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.blindsRecycler.addOnItemTouchListener(new RecyclerViewTouchListener(requireContext(), binding.blindsRecycler, new RecyclerViewTouchListener.RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {
                Intent intent = new Intent(requireContext(), LiveActivity.class);
                intent.putExtra("item", new Gson().toJson(tempRequests.get(position)));
                intent.putExtra("userID", currentUserItem.id);
                intent.putExtra("userName", currentUserItem.name);
                beMyEyesRef.child(tempRequests.get(position).id).child("helperId")
                                .setValue(firebaseUser.getUid());
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        getCurrentUser();

        return binding.getRoot();
    }

    Helper currentUserItem;

    private void getCurrentUser() {
        // Read from the database
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserItem = snapshot.getValue(Helper.class);
                getRequests();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getRequests() {
        // Read from the database
        beMyEyesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                requests = new ArrayList<>();
                tempRequests = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    VideoCallRequest item = dataSnapshot.getValue(VideoCallRequest.class);
                    if (item != null && item.end == null && item.helperId == null)
                        requests.add(item);
                }
                requests.sort(Comparator.comparing(donation -> donation.date));
                tempRequests.addAll(requests);
                requestsAdapter.setModels(tempRequests);
                binding.emptyStates.setVisibility(tempRequests.isEmpty() ? View.VISIBLE : View.GONE);
                requestsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


}