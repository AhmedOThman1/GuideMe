package com.guideMe.ui.fragments.common.events.one;

import static com.guideMe.ui.fragments.common.LauncherFragment.type;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.guideMe.R;
import com.guideMe.adapters.UsersAdapter;
import com.guideMe.databinding.FragmentOneEventBinding;
import com.guideMe.pojo.Event;
import com.guideMe.pojo.Helper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OneEventFragment extends Fragment {
    FragmentOneEventBinding binding;

    FirebaseDatabase database;
    DatabaseReference eventsRef, helpersRef, favEventsRef;

    Event item;

    UsersAdapter usersAdapter;
    ArrayList<Helper> users = new ArrayList<>();

    FirebaseUser firebaseUser;


    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentOneEventBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        helpersRef = database.getReference("Helpers");
        favEventsRef = database.getReference("Favorite Events").child(firebaseUser.getUid());

        usersAdapter = new UsersAdapter(requireContext());
        usersAdapter.setModels(users);
        binding.usersRecycler.setAdapter(usersAdapter);
        binding.usersRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        Bundle arg = getArguments();
        if (arg != null) {
            String id = arg.getString("id", "");
            eventsRef = database.getReference("Events").child(id);
            getUsers();
        }


        binding.eventTitle.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text",
                    binding.eventTitle.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Copied Text!", Toast.LENGTH_SHORT).show();
            return true;
        });

        binding.eventDescription.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text",
                    binding.eventDescription.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Copied Text!", Toast.LENGTH_SHORT).show();
            return true;
        });

        binding.address.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text",
                    binding.address.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Copied Text!", Toast.LENGTH_SHORT).show();
            return true;
        });
        binding.date.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text",
                    binding.date.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Copied Text!", Toast.LENGTH_SHORT).show();
            return true;
        });

        binding.subscribe.setOnClickListener(v -> {
            if (!item.usersId.contains(firebaseUser.getUid())) {
                item.usersId.add(firebaseUser.getUid());
                eventsRef.child("usersId").setValue(item.usersId);
                Toast.makeText(requireContext(), "Joined Successfully", Toast.LENGTH_SHORT).show();
                binding.subscribe.setVisibility(
                        item.usersId.contains(firebaseUser.getUid()) ? View.GONE :
                                View.VISIBLE);
            }
        });

        binding.joined.setOnClickListener(v -> {
            if (item.usersId.contains(firebaseUser.getUid())) {
                item.usersId.remove(firebaseUser.getUid());
                eventsRef.child("usersId").setValue(item.usersId);

                binding.joined.setVisibility(
                        item.usersId.contains(firebaseUser.getUid()) ? View.VISIBLE :
                                View.GONE);
            }
        });

        binding.favLayout.setOnClickListener(v -> {
            //TO DO put this apartment in the fav or delete it from fav
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            DatabaseReference favRef = database.getReference("Favorite Events").child(firebaseUser.getUid());
            if (item.loved != null && item.loved)
                favRef.child(item.id).setValue(null);
            else {
                favRef.child(item.id).setValue(true);
            }
            item.loved = !item.loved;
            binding.favLayout.setBackgroundResource(item.loved ? R.drawable.background_shimmer_red_circle : R.drawable.background_shimmer_circle);
            binding.favImg.setImageResource(item.loved ? R.drawable.ic_favorite_white : R.drawable.ic_favorite_red);
        });


        return binding.getRoot();
    }


    @SuppressLint("SetTextI18n")
    private void initUi() {

        //////////////////////////// Post body  /////////////////////////////////

        binding.eventTitle.setText(item.title);
        binding.eventDescription.setText(item.description);
        binding.address.setText(item.location);
        binding.date.setText(new SimpleDateFormat("'\uD83D\uDCC5' d MMMM yyyy '\uD83D\uDD54' h:mm a", Locale.getDefault()).format(item.date));

        Glide.with(requireContext())
                .load(item.image)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(binding.eventCover);

        binding.favLayout.setVisibility(type.equals("Helper") ? View.VISIBLE : View.GONE);

        //////////////////////////// End Post body  /////////////////////////////////


        //////////////////////////////// bottom buttons     ////////////////////////////////////////////

        if (item.loved != null) {
            binding.favLayout.setBackgroundResource(item.loved ? R.drawable.background_shimmer_red_circle : R.drawable.background_shimmer_circle);
            binding.favImg.setImageResource(item.loved ? R.drawable.ic_favorite_white : R.drawable.ic_favorite_red);
        }

        binding.subscribe.setVisibility(
                item.usersId.contains(firebaseUser.getUid()) ? View.GONE :
                        View.VISIBLE);
        binding.joined.setVisibility(
                item.usersId.contains(firebaseUser.getUid()) ? View.VISIBLE :
                        View.GONE);
        //////////////////////////////// End of buttons     ////////////////////////////////////////////

        users = new ArrayList<>();
        for (String id : item.usersId) {
            users.add(userItemMap.get(id));
        }

        binding.emptyStates.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        usersAdapter.setModels(users);
        usersAdapter.notifyDataSetChanged();

    }

    Map<String, Helper> userItemMap = new HashMap<>();

    private void getUsers() {
        // Read from the database
        helpersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                userItemMap = new HashMap<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Helper item = dataSnapshot.getValue(Helper.class);
                    assert item != null;
                    userItemMap.put(item.id, item);
                }

                getFavEvents();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    ArrayList<String> favEventsIds = new ArrayList<>();

    private void getFavEvents() {
        // Read from the database
        favEventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                favEventsIds = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String id = dataSnapshot.getKey();
                    assert id != null;
                    favEventsIds.add(id);
                }
                getEvent();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void getEvent() {
        // Read from the database
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                item = snapshot.getValue(Event.class);
                assert item != null;
                item.loved = (favEventsIds.contains(item.id));
                initUi();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}
