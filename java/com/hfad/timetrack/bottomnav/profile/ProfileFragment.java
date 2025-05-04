package com.hfad.timetrack.bottomnav.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.hfad.timetrack.IntroActivity;
import com.hfad.timetrack.R;
import com.hfad.timetrack.databinding.FragmentProfileBinding;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private Uri filePath;
    private DatabaseReference userRef;

    private ActivityResultLauncher<Intent> pickImageActivityResultLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        pickImageActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        filePath = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    requireContext().getContentResolver(),
                                    filePath
                            );
                            binding.profileImageView.setImageBitmap(bitmap);
                            uploadImage();
                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка загрузки изображения", e);
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        loadUserInfo();
        loadTodayStats();
        setupClickListeners();

        return binding.getRoot();
    }

    private void setupClickListeners() {
        binding.profileImageView.setOnClickListener(v -> selectImage());
        binding.logoutBtn.setOnClickListener(v -> logoutUser());
        binding.checkTimeButton.setOnClickListener(v -> showTotalWorkTime());

        binding.editProfileBtn.setOnClickListener(v -> showEditProfileDialog());
    }


    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать профиль");

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etFirstName = view.findViewById(R.id.etFirstName);
        EditText etLastName = view.findViewById(R.id.etLastName);
        EditText etBirthDate = view.findViewById(R.id.etBirthDate);

        // Загружаем текущие данные
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    etFirstName.setText(snapshot.child("firstName").getValue(String.class));
                    etLastName.setText(snapshot.child("lastName").getValue(String.class));
                    etBirthDate.setText(snapshot.child("birthDate").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(view);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String birthDate = etBirthDate.getText().toString().trim();

            if (!TextUtils.isEmpty(firstName)) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("firstName", firstName);
                updates.put("lastName", lastName);
                updates.put("birthDate", birthDate);
                updates.put("fullName", firstName + " " + lastName);

                userRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            binding.usernameEt.setText(firstName + " " + lastName);
                            Toast.makeText(requireContext(), "Профиль обновлен", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showTotalWorkTime() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Общее время работы");

        // Получаем статистику
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("userStats").child(userId);

        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long weeklyTime = snapshot.child("weeklyTime").getValue(Long.class) != null ?
                        snapshot.child("weeklyTime").getValue(Long.class) : 0;
                long monthlyTime = snapshot.child("monthlyTime").getValue(Long.class) != null ?
                        snapshot.child("monthlyTime").getValue(Long.class) : 0;
                double productivity = snapshot.child("productivity").getValue(Double.class) != null ?
                        snapshot.child("productivity").getValue(Double.class) : 0.0;

                String weeklyStr = formatTime(weeklyTime);
                String monthlyStr = formatTime(monthlyTime);

                String message = String.format(
                        "За неделю: %s\n" +
                                "За месяц: %s\n" +
                                "Продуктивность: %.1f%%",
                        weeklyStr, monthlyStr, productivity * 100
                );

                builder.setMessage(message);
                builder.setPositiveButton("OK", null);
                builder.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Ошибка загрузки статистики", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatTime(long millis) {
        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
        return String.format(Locale.getDefault(), "%02d ч %02d мин", hours, minutes);
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getContext(), IntroActivity.class));
        requireActivity().finish();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageActivityResultLauncher.launch(intent);
    }

    private void loadUserInfo() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "Пользователь не авторизован");
            binding.usernameEt.setText("Не авторизован");
            return;
        }

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    binding.usernameEt.setText("Данные не найдены");
                    return;
                }

                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);

                if (firstName != null && lastName != null) {
                    binding.usernameEt.setText(firstName + " " + lastName);
                } else if (snapshot.child("name").exists()) {
                    binding.usernameEt.setText(snapshot.child("name").getValue(String.class));
                } else {
                    binding.usernameEt.setText("Имя не указано");
                }

                loadProfileImage(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTodayStats() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        FirebaseDatabase.getInstance().getReference()
                .child("userTasks")
                .child(userId)
                .child(today)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long totalTime = 0;
                        StringBuilder tasksList = new StringBuilder();

                        for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                            if (taskSnapshot.child("timeSpent").exists() && taskSnapshot.child("name").exists()) {
                                Long time = taskSnapshot.child("timeSpent").getValue(Long.class);
                                String name = taskSnapshot.child("name").getValue(String.class);
                                totalTime += time != null ? time : 0L;
                                tasksList.append("• ").append(name).append("\n");
                            }
                        }

                        String timeString = formatTime(totalTime);
                        binding.todayWorkTime.setText("Сегодня: " + timeString);
                        binding.todayTasks.setText("Задачи сегодня:\n" + tasksList.toString());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Ошибка загрузки статистики", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProfileImage(DataSnapshot snapshot) {
        if (snapshot.hasChild("profileImage")) {
            String imageUrl = snapshot.child("profileImage").getValue(String.class);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(requireContext())
                        .load(imageUrl)
                        .into(binding.profileImageView);
            }
        }
    }

    private void uploadImage() {
        if (filePath == null) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseStorage.getInstance().getReference()
                .child("images/" + userId)
                .putFile(filePath)
                .addOnSuccessListener(taskSnapshot -> {
                    FirebaseStorage.getInstance().getReference()
                            .child("images/" + userId)
                            .getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                userRef.child("profileImage").setValue(uri.toString());
                                Toast.makeText(getContext(), "Фото загружено", Toast.LENGTH_SHORT).show();
                            });
                });
    }
}