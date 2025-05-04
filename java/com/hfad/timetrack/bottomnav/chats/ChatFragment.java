package com.hfad.timetrack.bottomnav.chats;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hfad.timetrack.ChatActivity;
import com.hfad.timetrack.chats.Chat;
import com.hfad.timetrack.chats.ChatsAdapter;
import com.hfad.timetrack.databinding.FragmentChatsBinding;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private FragmentChatsBinding binding;
    private ChatsAdapter adapter;
    private List<Chat> chats = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);
        setupRecyclerView();
        loadChats();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new ChatsAdapter(chats, chat -> {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String recipientId = getRecipientId(chat, currentUserId);

            if (recipientId != null) {
                openChatActivity(chat.getId(), recipientId);
            }
        });

        binding.chatsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.chatsRv.setAdapter(adapter);
    }

    private String getRecipientId(Chat chat, String currentUserId) {
        if (chat.getParticipants() != null) {
            for (String participantId : chat.getParticipants().keySet()) {
                if (!participantId.equals(currentUserId)) {
                    return participantId;
                }
            }
        }
        return null;
    }

    private void openChatActivity(String chatId, String recipientId) {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(recipientId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String recipientName = snapshot.child("name").getValue(String.class);
                        String recipientImage = snapshot.child("profileImage").getValue(String.class);

                        Intent intent = new Intent(getActivity(), ChatActivity.class);
                        intent.putExtra("chatId", chatId);
                        intent.putExtra("recipientId", recipientId);
                        intent.putExtra("recipientName", recipientName);
                        intent.putExtra("recipientImage", recipientImage != null ? recipientImage : "");
                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadChats() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("userChats")
                .child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chats.clear();
                        for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                            String chatId = chatSnapshot.getKey();
                            loadChatDetails(chatId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Ошибка загрузки чатов", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadChatDetails(String chatId) {
        FirebaseDatabase.getInstance().getReference("chats")
                .child(chatId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot chatData) {
                        Chat chat = chatData.getValue(Chat.class);
                        if (chat != null) {
                            chat.setId(chatId);
                            if (!containsChatWithSameRecipient(chat)) {
                                chats.add(chat);
                                adapter.updateChats(chats);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Ошибка загрузки чата", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean containsChatWithSameRecipient(Chat newChat) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String newRecipientId = getRecipientId(newChat, currentUserId);

        if (newRecipientId == null) return false;

        for (Chat existingChat : chats) {
            String existingRecipientId = getRecipientId(existingChat, currentUserId);
            if (newRecipientId.equals(existingRecipientId)) {
                return true;
            }
        }
        return false;
    }
}