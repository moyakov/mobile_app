package com.hfad.timetrack.models;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hfad.timetrack.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {
    private List<Message> messages;
    private String currentUserId;

    public MessagesAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == 0 ? R.layout.item_container_sent_message
                : R.layout.item_container_received_message;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSenderId().equals(currentUserId) ? 0 : 1;
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;
        private final TextView textDateTime;
        private final CircleImageView imageProfile;

        public MessageViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textDateTime = itemView.findViewById(R.id.textDateTime);

            if (viewType == 1) {
                imageProfile = itemView.findViewById(R.id.imageProfile);
            } else {
                imageProfile = null;
            }
        }

        public void bind(Message message) {
            textMessage.setText(message.getText());
            textDateTime.setText(DateUtils.formatDateTime(
                    itemView.getContext(),
                    message.getTimestamp(),
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE
            ));

            if (imageProfile != null && message.getSenderImageUrl() != null) {
                Glide.with(itemView.getContext())
                        .load(message.getSenderImageUrl())
                        .placeholder(R.drawable.avatar)
                        .error(R.drawable.avatar)
                        .into(imageProfile);
            }
        }
    }
}