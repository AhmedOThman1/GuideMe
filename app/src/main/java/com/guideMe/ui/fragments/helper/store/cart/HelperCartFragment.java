package com.guideMe.ui.fragments.helper.store.cart;

import static com.guideMe.ui.activities.MainActivity.formatPrice;
import static com.guideMe.ui.activities.MainActivity.openKeyboard;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.guideMe.R;
import com.guideMe.adapters.CartAdapter;
import com.guideMe.databinding.DialogUserEditCreditCardBinding;
import com.guideMe.pojo.Helper;
import com.guideMe.pojo.Payment;
import com.guideMe.pojo.PaymentCardInfo;
import com.guideMe.ui.activities.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.guideMe.databinding.FragmentHelperCartBinding;
import com.guideMe.pojo.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HelperCartFragment extends Fragment {

    FragmentHelperCartBinding binding;
    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference productsRef, currentUserRef, currentUserCartRef, paymentsRef, managerTokensRef;

    ArrayList<Product> cartItems = new ArrayList<>();
    ArrayList<Product> tempCartItems = new ArrayList<>();
    CartAdapter cartAdapter;

    public HelperCartFragment() {
    }

    double totalPrice = 0;
    ArrayList<String> managerTokens = new ArrayList<>();


    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentHelperCartBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        currentUserCartRef = database.getReference("Helpers Cart").child(firebaseUser.getUid());
        currentUserRef = database.getReference("Helpers").child(firebaseUser.getUid());
        productsRef = database.getReference("Products");
        paymentsRef = database.getReference("Payment");
        managerTokensRef = database.getReference("App Manager Tokens");

        cartAdapter = new CartAdapter(requireContext());
        cartAdapter.setModels(cartItems);
        binding.cartRecycler.setAdapter(cartAdapter);
        binding.cartRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.checkout.setOnClickListener(v -> {
            totalPrice = 0;
            tempCartItems = cartAdapter.getModels();
            for (int i = 0; i < cartItems.size(); i++) {
                if (cartItems.get(i).requestedQuantity > 0) {
                    totalPrice += (cartItems.get(i).price * cartItems.get(i).requestedQuantity);
                }
            }
            if (tempCartItems.isEmpty())
                Toast.makeText(requireContext(), "Select some products first!", Toast.LENGTH_SHORT).show();
            else {
                showPaymentDialog();
            }

        });


        cartAdapter.totalPrice.observe(requireActivity(), price -> {
            totalPrice = price;
            binding.totalPrice.setText(formatPrice(totalPrice));
        });

        cartAdapter.count.observe(requireActivity(), count -> {
            binding.cartItemsNum.setText(Html.fromHtml("There is <b>" + count + "</b> items in your cart.", Html.FROM_HTML_MODE_LEGACY));
            binding.totalItemsNum.setText("Total: (" + count + " items)");
            binding.emptyStates.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        });

        getCurrentUser();
        getManagerTokens();

        return binding.getRoot();
    }

    Helper currentUserItem;

    private void getCurrentUser() {
        // Read from the database
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                currentUserItem = snapshot.getValue(Helper.class);
                getProducts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getProducts() {
        // Read from the database
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                productItemMap = new HashMap<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Product product = dataSnapshot.getValue(Product.class);
                    assert product != null;
                    productItemMap.put(product.id, product);
                }

                getCurrentUserCart();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    Map<String, Product> productItemMap = new HashMap<>();

    private void getCurrentUserCart() {
        // Read from the database
        currentUserCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                ArrayList<Product> arrayList = new ArrayList<>();
                cartItems = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String productId = dataSnapshot.getKey();
                    Integer requestedQuantity = dataSnapshot.getValue(Integer.class);
                    arrayList.add(new Product(productId, requestedQuantity));
                }
                totalPrice = 0;
                for (Product item : arrayList) {
                    Objects.requireNonNull(productItemMap.get(item.id)).requestedQuantity = (item.requestedQuantity);
                    cartItems.add(productItemMap.get(item.id));
                    int lastIndex = cartItems.size() - 1;

                    totalPrice += (cartItems.get(lastIndex).requestedQuantity * cartItems.get(lastIndex).price);
                }
                binding.totalPrice.setText(formatPrice(totalPrice));
                cartAdapter.totalPrice.setValue(totalPrice);

                cartItems.sort(Comparator.comparing(p -> p.name));

                binding.cartItemsNum.setText("" + cartItems.size());
                binding.totalItemsNum.setText("Total: (" + cartItems.size() + " items)");
                cartAdapter.setModels(cartItems);
                binding.emptyStates.setVisibility(cartItems.isEmpty() ? View.VISIBLE : View.GONE);
                cartAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    AlertDialog dialog;
    private static final char space = ' ';

    @SuppressLint("SetTextI18n")
    private void showPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.dialog_user_edit_credit_card, null);
        DialogUserEditCreditCardBinding dialogBinding = DialogUserEditCreditCardBinding.bind(dialogView);

        dialogBinding.editCreditCard.setText("Pay " + formatPrice(totalPrice));

        if (currentUserItem.paymentCardInfo != null) {
            dialogBinding.cardNo.getEditText().setText(currentUserItem.paymentCardInfo.getCard_number());
            dialogBinding.month.getEditText().setText(currentUserItem.paymentCardInfo.getExpiration_month());
            dialogBinding.year.getEditText().setText(currentUserItem.paymentCardInfo.getExpiration_year());
            dialogBinding.cvv.getEditText().setText(currentUserItem.paymentCardInfo.getCvv());
        }

        dialogBinding.cardNo.getEditText().addTextChangedListener(new TextWatcher() {
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

        dialogBinding.month.getEditText().addTextChangedListener(new TextWatcher() {
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
                    dialogBinding.year.requestFocus();
                }

            }
        });


        dialogView.findViewById(R.id.edit_credit_card).setOnClickListener(v -> {
            if (dialogBinding.cardNo.getEditText().getText().toString().trim().isEmpty()) {
                dialogBinding.cardNo.setError(getString(R.string.empty));
                dialogBinding.cardNo.requestFocus();
                openKeyboard(dialogBinding.cardNo.getEditText(), requireActivity());
            } else if (dialogBinding.cardNo.getEditText().getText().toString().trim().length() != 19) {
                dialogBinding.cardNo.setError(getString(R.string.card_error));
                dialogBinding.cardNo.requestFocus();
                openKeyboard(dialogBinding.cardNo.getEditText(), requireActivity());
            } else if (dialogBinding.month.getEditText().getText().toString().trim().isEmpty()) {
                dialogBinding.cardNo.setError(null);
                dialogBinding.month.setError(getString(R.string.empty));
                dialogBinding.month.requestFocus();
                openKeyboard(dialogBinding.month.getEditText(), requireActivity());
            } else if (dialogBinding.year.getEditText().getText().toString().trim().isEmpty()) {
                dialogBinding.cardNo.setError(null);
                dialogBinding.month.setError(null);
                dialogBinding.year.setError(getString(R.string.empty));
                dialogBinding.year.requestFocus();
                openKeyboard(dialogBinding.year.getEditText(), requireActivity());
            } else if (dialogBinding.cvv.getEditText().getText().toString().trim().isEmpty()) {
                dialogBinding.cardNo.setError(null);
                dialogBinding.month.setError(null);
                dialogBinding.year.setError(null);
                dialogBinding.cvv.setError(getString(R.string.empty));
                dialogBinding.cvv.requestFocus();
                openKeyboard(dialogBinding.cvv.getEditText(), requireActivity());
            } else {
                dialogBinding.cardNo.setError(null);
                dialogBinding.month.setError(null);
                dialogBinding.year.setError(null);
                dialogBinding.cvv.setError(null);

                // payment info
                PaymentCardInfo paymentCardInfo = new PaymentCardInfo();
                paymentCardInfo.setCard_number(dialogBinding.cardNo.getEditText().getText().toString().trim());
                paymentCardInfo.setExpiration_month(dialogBinding.month.getEditText().getText().toString().trim());
                paymentCardInfo.setExpiration_year(dialogBinding.year.getEditText().getText().toString().trim());
                paymentCardInfo.setCvv(dialogBinding.cvv.getEditText().getText().toString().trim());
                currentUserRef.child("paymentCardInfo").setValue(paymentCardInfo);
                ////


                //// store notifications
                StringBuilder stringBuilder = new StringBuilder("Bought ");
                for (int i = 0; i < tempCartItems.size(); i++) {
                    if (i != 0 && i != tempCartItems.size() - 1) {
                        stringBuilder.append(", ");
                    } else if (i != 0 && i == tempCartItems.size() - 1) {
                        stringBuilder.append(" and ");
                    }
                    stringBuilder.append(tempCartItems.get(i).name);
                    Log.w("HERE", i + ": " + tempCartItems.get(i).name);
                }
                Payment item = new Payment();
                item.id = paymentsRef.push().getKey();
                item.userId = (firebaseUser.getUid());
                item.notificationId = (paymentsRef.push().getKey());
                item.date = (System.currentTimeMillis());
                stringBuilder.append(" for ");
                stringBuilder.append(formatPrice(totalPrice));
                item.notificationBody = (stringBuilder.toString());
                item.totalPrice = (totalPrice);
                paymentsRef.child(item.id).setValue(item);


                for (String token : managerTokens)
                    MainActivity.sendNotificationWithToken(token,
                            currentUserItem.name + "",
                            item.notificationBody);
                //TO DO Notify


                currentUserCartRef.setValue(null);
                Toast.makeText(requireContext(), "Done successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                Navigation.findNavController(requireActivity(), R.id.nav_helper_host_fragment).popBackStack();
            }
        });
        builder.setView(dialogView);
        dialog = builder.create();
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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

}