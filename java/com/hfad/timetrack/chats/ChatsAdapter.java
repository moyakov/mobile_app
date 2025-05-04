package com.hfad.timetrack.chats;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hfad.timetrack.R;
import com.hfad.timetrack.chats.Chat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {
    private List<Chat> chats;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatsAdapter(List<Chat> chats, OnChatClickListener listener) {
        this.chats = chats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(chats.get(position));
    }

    @Override
    public int getItemCount() {
        return chats == null ? 0 : chats.size();
    }

    public void updateChats(List<Chat> newChats) {
        this.chats = newChats;
        notifyDataSetChanged();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView lastMessageText;
        private final TextView timeText;
        private final ImageView profileImage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_name);
            lastMessageText = itemView.findViewById(R.id.text_last_message);
            timeText = itemView.findViewById(R.id.text_time);
            profileImage = itemView.findViewById(R.id.image_profile);
        }

        public void bind(Chat chat) {
            lastMessageText.setText(chat.getLastMessage());

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeText.setText(sdf.format(new Date(chat.getTimestamp())));

            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String recipientId = null;

            if (chat.getParticipants() != null) {
                for (Map.Entry<String, Boolean> entry : chat.getParticipants().entrySet()) {
                    if (!entry.getKey().equals(currentUserId)) {
                        recipientId = entry.getKey();
                        break;
                    }
                }
            }

            if (recipientId != null) {
                FirebaseDatabase.getInstance().getReference("Users").child(recipientId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String name = snapshot.child("name").getValue(String.class);
                                    String profileImageUrl = snapshot.child("profileImage").getValue(String.class);

                                    nameText.setText(name);

                                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                        Glide.with(itemView.getContext())
                                                .load(profileImageUrl)
                                                .placeholder(R.drawable.avatar)
                                                .into(profileImage);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(chat);
                }
            });
        }
    }
}