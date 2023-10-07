package com.guideMe.ui.fragments.common.store;

import static com.guideMe.ui.fragments.common.LauncherFragment.type;
import static com.guideMe.ui.fragments.manager.main.ManagerMainFragment.bottomNavigationView;
import static com.guideMe.ui.fragments.manager.main.ManagerMainFragment.toolbar;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.guideMe.R;
import com.guideMe.adapters.ProductsAdapter;
import com.guideMe.databinding.FragmentShowStoreBinding;
import com.guideMe.models.RecyclerViewTouchListener;
import com.guideMe.pojo.Helper;
import com.guideMe.pojo.Product;
import com.guideMe.ui.fragments.helper.main.HelperMainFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ShowStoreFragment extends Fragment {

    FragmentShowStoreBinding binding;
    FirebaseDatabase database;
    FirebaseUser firebaseUser;
    DatabaseReference productsRef, currentUserRef, currentUserCartRef;
    ArrayList<Product> productItems = new ArrayList<>();
    ArrayList<Product> tempProductItems = new ArrayList<>();
    ProductsAdapter productsAdapter;

    public ShowStoreFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentShowStoreBinding.inflate(inflater, container, false);

        if (type.equals("Manager")) {
            bottomNavigationView.setSelectedItemId(R.id.nav_store);
            toolbar.setTitle(getString(R.string.store));
        } else {
            HelperMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_store);
            HelperMainFragment.toolbar.setTitle(getString(R.string.store));
            binding.addProduct.setIconResource(R.drawable.ic_cart);
            binding.addProduct.setText(R.string.cart);
        }

        database = FirebaseDatabase.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        productsRef = database.getReference("Products");
        currentUserCartRef = database.getReference("Helpers Cart").child(firebaseUser.getUid());
        currentUserRef = database.getReference("Helpers").child(firebaseUser.getUid());

        productsAdapter = new ProductsAdapter(requireContext());
        productsAdapter.setAdminMode(type.equals("Manager"));
        productsAdapter.setModels(productItems);
        binding.productsRecycler.setAdapter(productsAdapter);
        binding.productsRecycler.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        binding.productsRecycler.addOnItemTouchListener(new RecyclerViewTouchListener(requireContext(), binding.productsRecycler, new RecyclerViewTouchListener.RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {

            }

            @Override
            public void onLongClick(View view, int position) {
                if (type.equals("Manager")) {
                    PopupMenu popupMenu = new PopupMenu(requireContext(), view);
                    popupMenu.getMenu().add("Edit");
                    popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getTitle().equals("Edit")) {
                            Bundle bundle = new Bundle();
                            bundle.putString("item", new Gson().toJson(tempProductItems.get(position)));
                            Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment).navigate(R.id.action_managerStoreFragment_to_managerAddProductFragment, bundle);
                        }
                        return true;
                    });
                    popupMenu.show();
                }
            }
        }));

        binding.addProduct.setOnClickListener(v ->
        {
            if (type.equals("Manager"))
                Navigation.findNavController(requireActivity(), R.id.nav_manager_host_fragment)
                        .navigate(R.id.action_managerStoreFragment_to_managerAddProductFragment);
            else {
                //TO DO Cart
                Navigation.findNavController(requireActivity(), R.id.nav_helper_host_fragment)
                        .navigate(R.id.helperCartFragment);
            }
        });

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tempProductItems = new ArrayList<>();
                String query = s.toString().trim().toLowerCase();

                if (query.isEmpty()) {
                    tempProductItems = new ArrayList<>();
                    tempProductItems.addAll(productItems);
                } else for (int i = 0; i < productItems.size(); i++) {
                    if (productItems.get(i).name.toLowerCase().contains(query) ||
                            productItems.get(i).category.toLowerCase().contains(query))
                        tempProductItems.add(productItems.get(i));
                }

                binding.noResultDesign.noResultDesign.setVisibility(tempProductItems.isEmpty() && !productItems.isEmpty() ? View.VISIBLE : View.GONE);

                productsAdapter.setModels(tempProductItems);
                productsAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        if (type.equals("Helper"))
            getCurrentUser();
        else
            getProducts();


        binding.shareCard.setOnClickListener(v -> {
            if (type.equals("Manager"))
                bottomNavigationView.setSelectedItemId(R.id.nav_share_app);
            else
                HelperMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_share_app);
        });

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
                getCurrentUserCart();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    Map<String, Integer> cartMap = new HashMap<>();

    private void getCurrentUserCart() {
        // Read from the database
        currentUserCartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                cartMap = new HashMap<>();
                ArrayList<Product> cartItems = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String productId = dataSnapshot.getKey();
                    Integer requestedQuantity = dataSnapshot.getValue(Integer.class);
                    cartItems.add(new Product(productId, requestedQuantity));
                    cartMap.put(productId, requestedQuantity);
                }
                currentUserItem.cartItems = cartItems;
                getProducts();
                Log.w("CART",""+cartItems.size());
                binding.addProduct.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        BadgeDrawable badgeDrawable = BadgeDrawable.create(requireContext());
                        badgeDrawable.setNumber(cartItems.size());
                        //Important to change the position of the Badge
                        badgeDrawable.setHorizontalOffset(30);
                        badgeDrawable.setVerticalOffset(20);

                        BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.addProduct, null);

                        binding.addProduct.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
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
                productItems = new ArrayList<>();
                tempProductItems = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Product product = dataSnapshot.getValue(Product.class);
                    product.requestedQuantity = cartMap.get(product.id);
                    if (product.requestedQuantity == null)
                        product.requestedQuantity = 0;
                    productItems.add(product);
                }
                tempProductItems.addAll(productItems);
                productsAdapter.setModels(tempProductItems);
                binding.emptyStates.setVisibility(tempProductItems.isEmpty() ? View.VISIBLE : View.GONE);
                binding.search.setVisibility(tempProductItems.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                productsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}