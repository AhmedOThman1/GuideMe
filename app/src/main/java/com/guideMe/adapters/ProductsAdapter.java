package com.guideMe.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.guideMe.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.guideMe.databinding.OneProductItemBinding;
import com.guideMe.pojo.Product;
import com.guideMe.ui.activities.MainActivity;

import java.util.ArrayList;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class ProductsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private ArrayList<Product> Models;
    private boolean adminMode = false;
    public MutableLiveData<Double> totalPrice = new MutableLiveData<>();

    public ProductsAdapter(@NonNull Context context) {
        this.context = context;
        totalPrice.setValue(0.0);
    }

    public void setModels(ArrayList<Product> models) {
        Models = models;
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_product_item, parent, false);
        return new AdapterViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        final AdapterViewHolder ViewHolder = (AdapterViewHolder) holder;


        Glide.with(context)
                .load(Models.get(position).image)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(ViewHolder.binding.oneImage);


        ViewHolder.binding.title.setText(Models.get(position).name);
        ViewHolder.binding.category.setText(Models.get(position).category);
        ViewHolder.binding.price.setText(MainActivity.formatPrice(Models.get(position).price));

        ViewHolder.binding.itemMinus.setEnabled(!adminMode);
        ViewHolder.binding.itemPlus.setEnabled(!adminMode);
        ViewHolder.binding.itemMinus.setBackgroundResource(adminMode ? R.drawable.background_plus_minus_black : R.drawable.background_plus_minus);
        ViewHolder.binding.itemPlus.setBackgroundResource(adminMode ? R.drawable.background_plus_minus_black : R.drawable.background_plus_minus);
        ViewHolder.binding.itemQuantity.setText(adminMode ? String.valueOf(Models.get(position).quantity) : String.valueOf(Models.get(position).requestedQuantity));

        ViewHolder.binding.itemPlus.setOnClickListener(v -> {
            if (Models.get(position).requestedQuantity < Models.get(position).quantity) {
                updateDb(position, Models.get(position).requestedQuantity + 1);
                Models.get(position).requestedQuantity = (Models.get(position).requestedQuantity + 1);
                totalPrice.setValue(totalPrice.getValue() + Models.get(position).price);
                Models.get(position).quantity = (Models.get(position).quantity - 1);
                ViewHolder.binding.itemQuantity.setText(String.valueOf(Models.get(position).requestedQuantity));
                this.notifyItemChanged(position);
            } else {
                Toast.makeText(
                        context,
                        "there are only " + Models.get(position).quantity + " pieces of this product",
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        ViewHolder.binding.description.setOnClickListener(v ->
                new MaterialTapTargetPrompt.Builder(((Activity) context))
                        .setTarget(ViewHolder.binding.description)
                        .setPrimaryText(Models.get(position).name)
                        .setSecondaryText(Models.get(position).description)
                        .show());

        ViewHolder.binding.itemMinus.setOnClickListener(v -> {
            if (Models.get(position).requestedQuantity > 0) {
                updateDb(position, Models.get(position).requestedQuantity - 1);
                Models.get(position).requestedQuantity = (Models.get(position).requestedQuantity - 1);
                Models.get(position).quantity = (Models.get(position).quantity + 1);
                totalPrice.setValue(totalPrice.getValue() - Models.get(position).price);
                ViewHolder.binding.itemQuantity.setText(String.valueOf(Models.get(position).requestedQuantity));
                this.notifyItemChanged(position);
            } else {
                updateDb(position, 0);
            }
        });

        ViewHolder.binding.outOfStock.setVisibility(Models.get(position).quantity > 0 ? View.GONE : View.VISIBLE);
    }

    private void updateDb(int position, int newVal) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference currentUserCartRef = database.getReference("Helpers Cart").child(firebaseUser.getUid());
        currentUserCartRef.child(Models.get(position).id).setValue(newVal > 0 ? newVal : null);
    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    static class AdapterViewHolder extends RecyclerView.ViewHolder {
        // views
        OneProductItemBinding binding;

        public AdapterViewHolder(@NonNull View itemView) {
            super(itemView);
            // find view by id
            binding = OneProductItemBinding.bind(itemView);
        }
    }

}
