package com.hfad.timetrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hfad.timetrack.bottomnav.chats.ChatFragment;
import com.hfad.timetrack.databinding.ActivityChatBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hfad.timetrack.models.Message;
import com.hfad.timetrack.models.MessagesAdapter;

import android.app.AlertDialog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    private String chatId;
    private String recipientId;
    private MessagesAdapter messagesAdapter;
    private List<Message> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        chatId = getIntent().getStringExtra("chatId");
        recipientId = getIntent().getStringExtra("recipientId");
        String recipientName = getIntent().getStringExtra("recipientName");

        if (recipientName == null && recipientId != null) {
            loadRecipientInfo(recipientId);
        } else {
            binding.textName.setText(recipientName);
        }

        binding.textName.setText(recipientName);

        binding.ImageBack.setOnClickListener(v -> {
            finish();
        });

        setupRecyclerView();
        loadMessages();

        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.imageInfo.setOnClickListener(v -> showChatInfoDialog());
    }

    private void showChatInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chat Information");

        FirebaseDatabase.getInstance().getReference("chats")
                .child(chatId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long messageCount = snapshot.child("messages").getChildrenCount();

                        String lastMessage = snapshot.child("lastMessage").getValue(String.class);
                        long timestamp = snapshot.child("timestamp").getValue(Long.class);
                        String lastMessageTime = timestamp > 0 ?
                                new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                        .format(new Date(timestamp)) : "N/A";

                        List<String> participants = new ArrayList<>();
                        if (snapshot.child("participants").exists()) {
                            for (DataSnapshot participant : snapshot.child("participants").getChildren()) {
                                participants.add(participant.getKey());
                            }
                        }

                        String infoMessage = String.format(
                                "Total messages: %d\n" +
                                        "Last message: %s\n" +
                                        "Last message time: %s\n" +
                                        "Participants: %d",
                                messageCount,
                                lastMessage != null ? lastMessage : "N/A",
                                lastMessageTime,
                                participants.size()
                        );


                        builder.setMessage(infoMessage);
                        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                        builder.create().show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this,
                                "Failed to load chat info",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupRecyclerView() {
        messagesAdapter = new MessagesAdapter(messages, FirebaseAuth.getInstance().getCurrentUser().getUid());
        binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.chatRecyclerView.setAdapter(messagesAdapter);
        binding.chatRecyclerView.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
    }

    private void loadRecipientInfo(String recipientId) {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(recipientId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null) {
                            binding.textName.setText(name);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this, "Ошибка загрузки имени", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMessages() {
        FirebaseDatabase.getInstance().getReference("chats")
                .child(chatId)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null) {
                                if (message.getSenderImageUrl() == null) {
                                    loadSenderProfile(message.getSenderId(), message, messageSnapshot.getKey());
                                } else {
                                    messages.add(message);
                                }
                            }
                        }
                        messagesAdapter.notifyDataSetChanged();
                        if (!messages.isEmpty()) {
                            binding.chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSenderProfile(String senderId, Message message, String messageId) {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(senderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String profileImageUrl = snapshot.child("profileImage").getValue(String.class);
                        message.setSenderImageUrl(profileImageUrl);

                        // Обновляем сообщение в базе с URL аватара
                        FirebaseDatabase.getInstance().getReference("chats")
                                .child(chatId)
                                .child("messages")
                                .child(messageId)
                                .child("senderImageUrl")
                                .setValue(profileImageUrl);

                        messages.add(message);
                        messagesAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Добавляем сообщение без аватара
                        messages.add(message);
                        messagesAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void sendMessage() {
        String messageText = binding.inputMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;

        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();

        FirebaseDatabase.getInstance().getReference("Users")
                .child(senderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String profileImageUrl = snapshot.child("profileImage").getValue(String.class);

                        Message message = new Message(senderId, messageText, timestamp, profileImageUrl);

                        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);

                        String messageId = chatRef.child("messages").push().getKey();
                        if (messageId == null) return;

                        chatRef.child("messages").child(messageId).setValue(message);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("lastMessage", messageText);
                        updates.put("timestamp", timestamp);
                        chatRef.updateChildren(updates);

                        binding.inputMessage.setText("");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this, "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}