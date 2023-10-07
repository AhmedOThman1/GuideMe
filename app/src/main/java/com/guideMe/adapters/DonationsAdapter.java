package com.guideMe.adapters;

import static com.guideMe.ui.activities.MainActivity.formatPrice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.guideMe.R;
import com.guideMe.databinding.OnePaymentItemBinding;
import com.guideMe.pojo.Donation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class DonationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private ArrayList<Donation> Models;

    public DonationsAdapter(@NonNull Context context) {
        this.context = context;
    }

    public void setModels(ArrayList<Donation> models) {
        Models = models;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_payment_item, parent, false);
        return new AdapterViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        final AdapterViewHolder ViewHolder = (AdapterViewHolder) holder;


        Glide.with(context)
                .load(Models.get(position).helper.photo)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(ViewHolder.binding.bigImage);

        ViewHolder.binding.userName.setText(Models.get(position).helper.name);


        long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Models.get(position).date);
        long hours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - Models.get(position).date);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Models.get(position).date);

        String temp = days < 1 ? (hours < 1 ?
                (minutes < 1 ? "Just now" : minutes + " minutes ago") : hours + " hours ago") :
                days == 1 ? "Yesterday" :
                        days < 30 ? days + " days ago" :
                                days < 300 ?
                                        new SimpleDateFormat("d MMMM", Locale.getDefault()).format(Models.get(position).date)
                                        : new SimpleDateFormat("d MMM yy", Locale.getDefault()).format(Models.get(position).date);

        ViewHolder.binding.notificationDate.setText(temp);

        ViewHolder.binding.requestBody.setText("Has donated an amount of " + formatPrice(Models.get(position).amount)+".");
    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    static class AdapterViewHolder extends RecyclerView.ViewHolder {
        // views
        OnePaymentItemBinding binding;

        public AdapterViewHolder(@NonNull View itemView) {
            super(itemView);
            // find view by id
            binding = OnePaymentItemBinding.bind(itemView);
        }
    }


}
