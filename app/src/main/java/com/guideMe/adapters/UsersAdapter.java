package com.guideMe.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.guideMe.R;
import com.guideMe.databinding.OneUserItemBinding;
import com.guideMe.pojo.Helper;

import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private ArrayList<Helper> Models = new ArrayList<>();

    public UsersAdapter(@NonNull Context context) {
        this.context = context;
    }

    public void setModels(ArrayList<Helper> models) {
        Models = models;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_user_item, parent, false);
        return new AdapterViewHolder(OneUserItemBinding.bind(view));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        final AdapterViewHolder ViewHolder = (AdapterViewHolder) holder;

        Glide.with(context)
                .load(Models.get(position).photo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ViewHolder.binding.userImg);

        ViewHolder.binding.userName.setText(Models.get(position).name);
        ViewHolder.binding.phone.setText(Models.get(position).phone);
    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    static class AdapterViewHolder extends RecyclerView.ViewHolder {
        // views
        OneUserItemBinding binding;

        public AdapterViewHolder(@NonNull OneUserItemBinding binding) {
            super(binding.getRoot());
            // find view by id
            this.binding = binding;

        }
    }

}
