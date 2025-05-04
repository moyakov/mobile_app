package com.hfad.timetrack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hfad.timetrack.bottomnav.chats.ChatFragment;
import com.hfad.timetrack.bottomnav.new_chat.NewChatFragment;
import com.hfad.timetrack.bottomnav.profile.ProfileFragment;
import com.hfad.timetrack.bottomnav.timetrack.TimeTrackFragment;
import com.hfad.timetrack.databinding.ActivityMainBinding;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (FirebaseAuth.getInstance().getCurrentUser()==null){
            startActivity(new Intent(MainActivity.this, IntroActivity.class));
            finish();
        } else {
            saveFcmToken();
        }

        getSupportFragmentManager().beginTransaction().replace(binding.FragmentContainer.getId(), new ChatFragment()).commit();
        binding.bottomNav.setSelectedItemId(R.id.chats);

        Map<Integer, Fragment> fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.timetrack, new TimeTrackFragment());
        fragmentMap.put(R.id.chats, new ChatFragment());
        fragmentMap.put(R.id.newchats, new NewChatFragment());
        fragmentMap.put(R.id.profile, new ProfileFragment());

        binding.bottomNav.setOnItemSelectedListener(Item -> {
            Fragment fragment = fragmentMap.get(Item.getItemId());

            getSupportFragmentManager().beginTransaction().replace(binding.FragmentContainer.getId(), fragment).commit();
            return true;
        });
    }

    private void saveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(currentUserId)
                                .child("fcmToken")
                                .setValue(token)
                                .addOnFailureListener(e -> Log.e("FCM", "Ошибка сохранения токена: " + e.getMessage()));
                    }
                });
    }

}
