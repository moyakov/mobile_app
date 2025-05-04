package com.hfad.timetrack.users;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hfad.timetrack.R;

public class UserViewHolder extends RecyclerView.ViewHolder {
    public TextView username_tv;
    public ImageView profileImage_iv;

    public UserViewHolder(@NonNull View itemView) {
        super(itemView);
        username_tv = itemView.findViewById(R.id.username_tv);
        profileImage_iv = itemView.findViewById(R.id.profileImage_iv);
    }
}