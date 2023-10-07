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
import com.guideMe.databinding.OneCartItemBinding;
import com.guideMe.pojo.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class CartAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private ArrayList<Product> Models;
    public MutableLiveData<Double> totalPrice = new MutableLiveData<>();
    public MutableLiveData<Integer> count = new MutableLiveData<>();

    public CartAdapter(@NonNull Context context) {
        this.context = context;
        totalPrice.setValue(0.0);
    }

    public void setModels(ArrayList<Product> models) {
        Models = models;
        count.setValue(models.size());
    }

    public ArrayList<Product> getModels() {
        return Models;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_cart_item, parent, false);
        return new AdapterViewHolder(OneCartItemBinding.bind(view));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        final AdapterViewHolder ViewHolder = (AdapterViewHolder) holder;


        Glide.with(context)
                .load(Models.get(position).image)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(ViewHolder.binding.productImage);


        ViewHolder.binding.productName.setText(Models.get(position).name);

        ViewHolder.binding.productPrice.setText("$" + String.format(Locale.getDefault(), "%.2f", Models.get(position).price));

        ViewHolder.binding.itemQuantity.setText("x" + Models.get(position).requestedQuantity);


        ViewHolder.binding.itemPlus.setOnClickListener(v -> {
            if (Models.get(position).requestedQuantity < Models.get(position).quantity) {
                Models.get(position).requestedQuantity=(Models.get(position).requestedQuantity + 1);
                totalPrice.setValue(totalPrice.getValue() + Models.get(position).price);
                Models.get(position).quantity=(Models.get(position).quantity - 1);
                ViewHolder.binding.itemQuantity.setText("x" + Models.get(position).requestedQuantity);
                updateDb(position, Models.get(position).requestedQuantity);
            } else {
                Toast.makeText(
                        context,
                        "there are only " + Models.get(position).quantity + " pieces of this product",
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        ViewHolder.binding.productImage.setOnClickListener(v ->
                new MaterialTapTargetPrompt.Builder(((Activity) context))
                        .setTarget(ViewHolder.binding.productImage)
                        .setPrimaryText(Models.get(position).name)
                        .setSecondaryText(Models.get(position).description)
                        .show());

        ViewHolder.binding.itemMinus.setOnClickListener(v -> {
            if (Models.get(position).requestedQuantity > 0) {
                int val = Models.get(position).requestedQuantity - 1;
                totalPrice.setValue(totalPrice.getValue() - Models.get(position).price);
                ViewHolder.binding.itemQuantity.setText("x" + Models.get(position).requestedQuantity);
                Models.get(position).requestedQuantity=(Models.get(position).requestedQuantity - 1);
                Models.get(position).quantity=(Models.get(position).quantity + 1);
                updateDb(position, Models.get(position).requestedQuantity);
            } else {
                updateDb(position, 0);
            }
        });

        ViewHolder.binding.remove.setOnClickListener(v -> {
            totalPrice.setValue(totalPrice.getValue() - (Models.get(position).price * Models.get(position).requestedQuantity));
            updateDb(position, 0);
        });
    }

    private void updateDb(int position, int newVal) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference currentUserCartRef = database.getReference("Helpers Cart").child(firebaseUser.getUid());
        currentUserCartRef.child(Models.get(position).id).setValue(newVal > 0 ? newVal : null);
        if (newVal == 0) {
            Models.remove(position);
            count.setValue(Models.size());
            this.notifyDataSetChanged();
        } else
            this.notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    static class AdapterViewHolder extends RecyclerView.ViewHolder {
        // views
        OneCartItemBinding binding;

        public AdapterViewHolder(@NonNull OneCartItemBinding binding) {
            super(binding.getRoot());
            // find view by id
            this.binding = binding;
        }
    }

}
