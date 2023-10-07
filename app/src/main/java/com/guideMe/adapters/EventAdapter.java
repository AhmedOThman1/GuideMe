package com.guideMe.adapters;

import static com.guideMe.ui.fragments.common.LauncherFragment.type;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.guideMe.R;
import com.guideMe.databinding.OneEventItemBinding;
import com.guideMe.pojo.Event;
import com.guideMe.ui.fragments.common.LauncherFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private ArrayList<Event> Models = new ArrayList<>();
    FirebaseUser firebaseUser;

    public EventAdapter(@NonNull Context context) {
        this.context = context;
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public void setModels(ArrayList<Event> models) {
        Models = models;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_event_item, parent, false);
        return new AdapterViewHolder(OneEventItemBinding.bind(view));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        final AdapterViewHolder ViewHolder = (AdapterViewHolder) holder;

        ViewHolder.binding.title.setText(Models.get(position).title);
        ViewHolder.binding.location.setText(Models.get(position).location);
        ViewHolder.binding.date.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Models.get(position).date));
        ViewHolder.binding.time.setText(new SimpleDateFormat("hh:mm", Locale.getDefault()).format(Models.get(position).date));
        ViewHolder.binding.usersNum.setText(Models.get(position).usersId.size() + "");
        ViewHolder.binding.description.setText(Models.get(position).description);

        Glide.with(context)
                .load(Models.get(position).image)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(ViewHolder.binding.oneImage);

        ViewHolder.binding.favLayout.setVisibility(type.equals("User") ? View.VISIBLE : View.GONE);

        ViewHolder.binding.favLayout.setOnClickListener(v -> {
            //TO DO put this apartment in the fav or delete it from fav
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            DatabaseReference favRef = database.getReference("Favorite Events").child(firebaseUser.getUid());
            if (Models.get(position).loved != null && Models.get(position).loved)
                favRef.child(Models.get(position).id).setValue(null);
            else {
                favRef.child(Models.get(position).id).setValue(true);
            }
            Models.get(position).loved = !Models.get(position).loved;
            ViewHolder.binding.favLayout.setBackgroundResource(Models.get(position).loved ? R.drawable.background_shimmer_red_circle : R.drawable.background_shimmer_circle);
            ViewHolder.binding.favImg.setImageResource(Models.get(position).loved ? R.drawable.ic_favorite_white : R.drawable.ic_favorite_red);
        });
        if (Models.get(position).loved != null) {
            ViewHolder.binding.favLayout.setBackgroundResource(Models.get(position).loved ? R.drawable.background_shimmer_red_circle : R.drawable.background_shimmer_circle);
            ViewHolder.binding.favImg.setImageResource(Models.get(position).loved ? R.drawable.ic_favorite_white : R.drawable.ic_favorite_red);
        }

        ViewHolder.binding.subscribe.setVisibility((type.equals("User") && !Models.get(position).usersId.contains(firebaseUser.getUid())) ? View.VISIBLE : View.GONE);
        ViewHolder.binding.joined.setVisibility((type.equals("User") && Models.get(position).usersId.contains(firebaseUser.getUid())) ? View.VISIBLE : View.GONE);

        ViewHolder.binding.subscribe.setOnClickListener(v -> {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference eventsRef = database.getReference("Events").child(Models.get(position).id);
            Models.get(position).usersId.add(firebaseUser.getUid());
            eventsRef.child("usersId").setValue(Models.get(position).usersId);
            ViewHolder.binding.joined.setVisibility(View.VISIBLE);
            ViewHolder.binding.subscribe.setVisibility(View.GONE);
            Toast.makeText(context, "Joined Successfully!", Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    static class AdapterViewHolder extends RecyclerView.ViewHolder {
        OneEventItemBinding binding;

        public AdapterViewHolder(@NonNull OneEventItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
