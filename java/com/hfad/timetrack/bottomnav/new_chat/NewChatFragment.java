package com.hfad.timetrack.bottomnav.new_chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hfad.timetrack.ChatActivity;
import com.hfad.timetrack.databinding.FragmentNewChatBinding;
import com.hfad.timetrack.users.User;
import com.hfad.timetrack.users.UsersAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NewChatFragment extends Fragment {
    private FragmentNewChatBinding binding;
    private UsersAdapter adapter;
    private ArrayList<User> users = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @NonNull Bundle savedInstanceState) {
        binding = FragmentNewChatBinding.inflate(inflater, container, false);

        setupRecyclerView();
        loadUsers();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        binding.usersRv.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.usersRv.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));


        adapter = new UsersAdapter(users, user -> {
            startChatWithUser(user);
        });

        binding.usersRv.setAdapter(adapter);
    }

    private void loadUsers() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("userChats")
                .child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userChatsSnapshot) {
                        Set<String> usersWithChats = new HashSet<>();
                        for (DataSnapshot chatSnapshot : userChatsSnapshot.getChildren()) {
                            String chatId = chatSnapshot.getKey();
                            FirebaseDatabase.getInstance().getReference("chats")
                                    .child(chatId)
                                    .child("participants")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot participantsSnapshot) {
                                            for (DataSnapshot participantSnapshot : participantsSnapshot.getChildren()) {
                                                String participantId = participantSnapshot.getKey();
                                                if (!participantId.equals(currentUserId)) {
                                                    usersWithChats.add(participantId);
                                                }
                                            }
                                            loadAllUsersExcept(currentUserId, usersWithChats);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("NewChatFragment", "Error loading participants", error.toException());
                                        }
                                    });
                        }
                        if (!userChatsSnapshot.exists()) {
                            loadAllUsersExcept(currentUserId, new HashSet<>());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("NewChatFragment", "Error loading user chats", error.toException());
                    }
                });
    }

    private void loadAllUsersExcept(String currentUserId, Set<String> excludedUserIds) {
        FirebaseDatabase.getInstance().getReference("Users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        users.clear();
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String userId = userSnapshot.getKey();
                            if (userId.equals(currentUserId)) continue;
                            if (excludedUserIds.contains(userId)) continue;

                            String name = userSnapshot.child("name").getValue(String.class);
                            String profileImage = userSnapshot.child("profileImage").getValue(String.class);

                            users.add(new User(userId, name, profileImage != null ? profileImage : ""));
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("NewChatFragment", "Error loading users", error.toException());
                    }
                });
    }

    private void startChatWithUser(User selectedUser) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String selectedUserId = selectedUser.getId();

        DatabaseReference userChatsRef = FirebaseDatabase.getInstance().getReference("userChats");
        userChatsRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String chatId = findExistingChat(snapshot, selectedUserId);

                if (chatId != null) {
                    openChatActivity(chatId, selectedUser);
                } else {
                    createNewChat(currentUserId, selectedUserId, selectedUser);
                }

                loadUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NewChatFragment", "Error checking chats", error.toException());
            }
        });
    }

    private String findExistingChat(DataSnapshot snapshot, String selectedUserId) {
        for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
            if (chatSnapshot.child(selectedUserId).exists()) {
                return chatSnapshot.getKey();
            }
        }
        return null;
    }

    private void createNewChat(String currentUserId, String selectedUserId, User selectedUser) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        String chatId = chatsRef.push().getKey();

        if (chatId == null) return;

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("lastMessage", "");
        chatData.put("timestamp", System.currentTimeMillis());

        Map<String, Boolean> participants = new HashMap<>();
        participants.put(currentUserId, true);
        participants.put(selectedUserId, true);
        chatData.put("participants", participants);

        Map<String, Object> updates = new HashMap<>();
        updates.put("chats/" + chatId, chatData);
        updates.put("userChats/" + currentUserId + "/" + chatId, true);
        updates.put("userChats/" + selectedUserId + "/" + chatId, true);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnSuccessListener(aVoid -> openChatActivity(chatId, selectedUser))
                .addOnFailureListener(e -> Log.e("NewChatFragment", "Chat creation failed", e));
    }

    private void openChatActivity(String chatId, User recipient) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("recipientName", recipient.getUsername());
        intent.putExtra("recipientImage", recipient.getProfileImage());
        intent.putExtra("recipientId", recipient.getId());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
    }
}