package com.guideMe.ui.fragments.common.events;

import static com.guideMe.ui.activities.MainActivity.formatPrice;
import static com.guideMe.ui.activities.MainActivity.openKeyboard;
import static com.guideMe.ui.activities.MainActivity.sendNotificationWithToken;
import static com.guideMe.ui.activities.MainActivity.space;
import static com.guideMe.ui.fragments.common.LauncherFragment.type;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.guideMe.R;
import com.guideMe.adapters.EventAdapter;
import com.guideMe.databinding.DialogDonateBinding;
import com.guideMe.databinding.FragmentShowEventsBinding;
import com.guideMe.models.RecyclerViewTouchListener;
import com.guideMe.pojo.Donation;
import com.guideMe.pojo.Event;
import com.guideMe.pojo.Helper;
import com.guideMe.pojo.PaymentCardInfo;
import com.guideMe.ui.fragments.helper.main.HelperMainFragment;
import com.guideMe.ui.fragments.manager.main.ManagerMainFragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class ShowEventsFragment extends Fragment {

    FragmentShowEventsBinding binding;

    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference eventsRef, favEventsRef, donationsRef, currentUserRef, managerTokensRef;
    ArrayList<Event> eventItems = new ArrayList<>();
    ArrayList<Event> tempEventItems = new ArrayList<>();
    EventAdapter eventAdapter;
    int navId;
    ArrayList<String> managerTokens = new ArrayList<>();

    boolean fav = false;

    public ShowEventsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentShowEventsBinding.inflate(inflater, container, false);
        Bundle args = getArguments();

        if (args != null) {
            fav = args.getBoolean("fav", false);
            if (fav)
                binding.fav.setVisibility(View.GONE);
        }

        if (type.equals("Manager")) {
            navId = R.id.nav_manager_host_fragment;
            ManagerMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_events);
            ManagerMainFragment.toolbar.setTitle(getString(R.string.events));
            binding.addEvent.setVisibility(View.VISIBLE);
            binding.donateCard.setVisibility(View.GONE);
            binding.fav.setVisibility(View.GONE);
        } else {//TO DO
            navId = R.id.nav_helper_host_fragment;
            HelperMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_events);
            HelperMainFragment.toolbar.setTitle(fav ? "Favourite Events" : getString(R.string.events));
            binding.addEvent.setVisibility(View.GONE);
            binding.donateCard.setVisibility(View.VISIBLE);
            binding.fav.setVisibility(View.VISIBLE);
        }
        Log.w("EVENTS", type + "," + type.equals("Manager") + "," + binding.addEvent.getVisibility());

        binding.fav.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("fav", true);
            Navigation.findNavController(requireActivity(), navId)
                    .navigate(R.id.showFavEventsFragment, bundle);
        });

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        eventsRef = database.getReference("Events");
        favEventsRef = database.getReference("Favorite Events").child(firebaseUser.getUid());
        currentUserRef = database.getReference("Helpers").child(firebaseUser.getUid());
        donationsRef = database.getReference("Donations");
        managerTokensRef = database.getReference("App Manager Tokens");


        eventAdapter = new EventAdapter(requireContext());
        eventAdapter.setModels(eventItems);
        binding.eventsRecycler.setAdapter(eventAdapter);
        binding.eventsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventsRecycler.addOnItemTouchListener(new RecyclerViewTouchListener(requireContext(), binding.eventsRecycler, new RecyclerViewTouchListener.RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                bundle.putString("id", tempEventItems.get(position).id);
                Navigation.findNavController(requireActivity(),
                                type.equals("Manager") ? R.id.nav_manager_host_fragment :
                                        R.id.nav_helper_host_fragment)
                        .navigate(R.id.oneEventFragment, bundle);
            }

            @Override
            public void onLongClick(View view, int position) {
                if (type.equals("Manager")) {
                    PopupMenu popupMenu = new PopupMenu(requireContext(), view);
                    popupMenu.getMenu().add("Edit");
                    popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getTitle().equals("Edit")) {
                            //TO DO edit
                            Bundle bundle = new Bundle();
                            bundle.putString("item", new Gson().toJson(tempEventItems.get(position)));
                            Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment)
                                    .navigate(R.id.action_managerEventsFragment_to_managerAddEventFragment, bundle);
                        }
                        return true;
                    });
                    popupMenu.show();
                }
            }
        }));


        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tempEventItems = new ArrayList<>();
                String query = s.toString().trim().toLowerCase();

                if (query.isEmpty()) {
                    tempEventItems = new ArrayList<>();
                    tempEventItems.addAll(eventItems);
                } else for (int i = 0; i < eventItems.size(); i++) {
                    if (eventItems.get(i).title.toLowerCase().contains(query) ||
                            eventItems.get(i).location.toLowerCase().contains(query) ||
                            eventItems.get(i).description.toLowerCase().contains(query)
                    )
                        tempEventItems.add(eventItems.get(i));
                }


                binding.noResultDesign.noResultDesign.setVisibility(tempEventItems.isEmpty() && !eventItems.isEmpty() ? View.VISIBLE : View.GONE);

                eventAdapter.setModels(tempEventItems);
                eventAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        getFavEvents();
        getManagerTokens();
        if (type.equals("Helper"))
            getCurrentUser();

        binding.addEvent.setOnClickListener(v ->
                Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment)
                        .navigate(R.id.action_managerEventsFragment_to_managerAddEventFragment));


        binding.donateCard.setOnClickListener(v -> {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_donate, null);
            AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();
            DialogDonateBinding donateBinding = DialogDonateBinding.bind(view);

            donateBinding.donate.setOnClickListener(vv -> {
                if (donateBinding.amount.getEditText().getText().toString().trim().isEmpty()) {
                    donateBinding.amount.setError(getString(R.string.empty));
                    donateBinding.amount.requestFocus();
                    openKeyboard(donateBinding.amount.getEditText(), requireActivity());
                } else {
                    donateBinding.amount.setError(null);
                    dialog.dismiss();
                    showEditCreditCardDialog(Double.parseDouble(
                            donateBinding.amount.getEditText().getText().toString().trim()
                    ));
                }
            });

            dialog.show();
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        });

        return binding.getRoot();
    }


    Helper currentHelper;

    private void getCurrentUser() {
        // Read from the database
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentHelper = snapshot.getValue(Helper.class);
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
                getEvents();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getEvents() {
        // Read from the database
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                eventItems = new ArrayList<>();
                tempEventItems = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event item = dataSnapshot.getValue(Event.class);
                    assert item != null;

                    item.loved = (favEventsIds.contains(item.id));
                    //TO DO Check date and validate
                    Log.w("validate", item.date + "," + System.currentTimeMillis() + "," + TimeUnit.DAYS.toMillis(2) + "=" + (item.date > (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2))));
                    if (item.date > (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2))
                            && type.equals("Helper"))
                        continue;

                    if (!fav || item.loved)
                        eventItems.add(item);
                }
                eventItems.sort(Comparator.comparing(event -> event.date));
                tempEventItems.addAll(eventItems);
                binding.emptyStates.setVisibility(tempEventItems.isEmpty() ? View.VISIBLE : View.GONE);
                binding.search.setVisibility(tempEventItems.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                eventAdapter.setModels(tempEventItems);
                eventAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    TextInputLayout card_no, month, year, cvv;

    @SuppressLint("SetTextI18n")
    private void showEditCreditCardDialog(Double amount) {
        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.dialog_user_edit_credit_card, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

        card_no = dialogView.findViewById(R.id.card_no);
        month = dialogView.findViewById(R.id.month);
        year = dialogView.findViewById(R.id.year);
        cvv = dialogView.findViewById(R.id.cvv);
        MaterialButton pay = dialogView.findViewById(R.id.edit_credit_card);
        pay.setText("Pay " + formatPrice(amount));

        if (currentHelper.paymentCardInfo != null) {
            card_no.getEditText().setText(currentHelper.paymentCardInfo.getCard_number());
            month.getEditText().setText(currentHelper.paymentCardInfo.getExpiration_month());
            year.getEditText().setText(currentHelper.paymentCardInfo.getExpiration_year());
            cvv.getEditText().setText(currentHelper.paymentCardInfo.getCvv());
        }

        card_no.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && (s.length() % 5) == 0) {
                    Log.w("TAG", "afterTextChanged: " + s.length() + "," + (s.length() % 5));
                    final char c = s.charAt(s.length() - 1);
                    if (space == c) {
                        // Remove spacing char
                        s.delete(s.length() - 1, s.length());
                    } else
                        // Insert char where needed.
                        s.insert(s.length() - 1, String.valueOf(space));
                }
            }
        });

        month.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 1 && Integer.parseInt(editable.toString()) > 1) {
                    editable.insert(editable.length() - 1, "0");
                    year.requestFocus();
                }

            }
        });


        pay.setOnClickListener(v -> {
            if (card_no.getEditText().getText().toString().trim().isEmpty()) {
                card_no.setError(getString(R.string.empty));
                card_no.requestFocus();
                openKeyboard(card_no.getEditText(), requireActivity());
            } else if (card_no.getEditText().getText().toString().trim().length() != 19) {
                card_no.setError(getString(R.string.card_error));
                card_no.requestFocus();
                openKeyboard(card_no.getEditText(), requireActivity());
            } else if (month.getEditText().getText().toString().trim().isEmpty()) {
                card_no.setError(null);
                month.setError(getString(R.string.empty));
                month.requestFocus();
                openKeyboard(month.getEditText(), requireActivity());
            } else if (year.getEditText().getText().toString().trim().isEmpty()) {
                card_no.setError(null);
                month.setError(null);
                year.setError(getString(R.string.empty));
                year.requestFocus();
                openKeyboard(year.getEditText(), requireActivity());
            } else if (cvv.getEditText().getText().toString().trim().isEmpty()) {
                card_no.setError(null);
                month.setError(null);
                year.setError(null);
                cvv.setError(getString(R.string.empty));
                cvv.requestFocus();
                openKeyboard(cvv.getEditText(), requireActivity());
            } else {
                card_no.setError(null);
                month.setError(null);
                year.setError(null);
                cvv.setError(null);

                // payment info
                PaymentCardInfo paymentCardInfo = new PaymentCardInfo();
                paymentCardInfo.setCard_number(card_no.getEditText().getText().toString().trim());
                paymentCardInfo.setExpiration_month(month.getEditText().getText().toString().trim());
                paymentCardInfo.setExpiration_year(year.getEditText().getText().toString().trim());
                paymentCardInfo.setCvv(cvv.getEditText().getText().toString().trim());
                currentUserRef.child("paymentCardInfo").setValue(paymentCardInfo);
                ////
                // Donation
                Donation donation = new Donation();
                donation.id = donationsRef.push().getKey();
                donation.date = System.currentTimeMillis();
                donation.userId = currentHelper.id;
                donation.amount = amount;
                donation.helper = currentHelper;
                donationsRef.child(donation.id).setValue(donation);

                for (String token : managerTokens)
                    sendNotificationWithToken(token,
                            "New Donation",
                            donation.helper.name + " has donated an amount of " + formatPrice(donation.amount) + ".");

                Toast.makeText(requireContext(), "Donate has send successfully, thank you for your generosity!", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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
                    Navigation.findNavController(requireActivity(), navId)
                            .popBackStack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

    }
}