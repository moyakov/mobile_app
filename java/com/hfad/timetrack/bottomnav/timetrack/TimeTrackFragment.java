package com.hfad.timetrack.bottomnav.timetrack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hfad.timetrack.R;
import com.hfad.timetrack.databinding.FragmentTimeBinding;
import com.hfad.timetrack.models.Task;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimeTrackFragment extends Fragment {
    private FragmentTimeBinding binding;
    private List<Task> tasks = new ArrayList<>();
    private Task currentTask;
    private long startTime;
    private Uri proofImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    proofImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                requireActivity().getContentResolver(),
                                proofImageUri);
                        binding.proofImage.setImageBitmap(bitmap);
                        binding.proofImage.setVisibility(View.VISIBLE);
                        binding.removeProofBtn.setVisibility(View.VISIBLE);
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTimeBinding.inflate(inflater, container, false);
        setupUI();
        loadTasks();
        return binding.getRoot();
    }

    private void setupUI() {
        binding.addTaskBtn.setOnClickListener(v -> addTask());
        binding.startTaskBtn.setOnClickListener(v -> startTask());
        binding.stopTaskBtn.setOnClickListener(v -> showStopConfirmation());
        binding.addProofBtn.setOnClickListener(v -> openImagePicker());
        binding.removeProofBtn.setOnClickListener(v -> removeProofImage());

        binding.proofContainer.setVisibility(View.GONE);
        binding.proofImage.setVisibility(View.GONE);
        binding.removeProofBtn.setVisibility(View.GONE);
    }

    private void addTask() {
        String taskName = binding.taskInputLayout.getText().toString().trim();
        if (taskName.isEmpty()) {
            binding.taskInputLayout.setError("Введите название задачи");
            return;
        }

        Task newTask = new Task(taskName, System.currentTimeMillis(), 0, false, "");
        String taskId = FirebaseDatabase.getInstance().getReference()
                .child("userTasks")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(getCurrentDate())
                .push()
                .getKey();

        newTask.setId(taskId);

        FirebaseDatabase.getInstance().getReference()
                .child("userTasks")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(getCurrentDate())
                .child(taskId)
                .setValue(newTask)
                .addOnSuccessListener(aVoid -> {
                    binding.taskInputLayout.setText("");
                    Toast.makeText(getContext(), "Задача добавлена", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTasks() {
        FirebaseDatabase.getInstance().getReference()
                .child("userTasks")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(getCurrentDate())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        tasks.clear();
                        for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                            Task task = taskSnapshot.getValue(Task.class);
                            if (task != null) {
                                task.setId(taskSnapshot.getKey());
                                tasks.add(task);

                                if (task.isActive()) {
                                    currentTask = task;
                                    startTime = task.getStartTime();
                                    updateTimer();
                                }
                            }
                        }
                        updateUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Ошибка загрузки задач", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startTask() {
        if (tasks.isEmpty()) {
            Toast.makeText(getContext(), "Сначала добавьте задачи", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTask != null) {
            Toast.makeText(getContext(), "Завершите текущую задачу", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите задачу")
                .setItems(getTaskNames(), (dialog, which) -> {
                    currentTask = tasks.get(which);
                    startTime = System.currentTimeMillis();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("active", true);
                    updates.put("startTime", startTime);
                    FirebaseDatabase.getInstance().getReference()
                            .child("userTasks")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child(getCurrentDate())
                            .child(currentTask.getId())
                            .updateChildren(updates);

                    updateUI();
                })
                .show();
    }

    private void showStopConfirmation() {
        if (proofImageUri == null) {
            Toast.makeText(getContext(), "Добавьте подтверждение работы", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Завершить задачу?")
                .setMessage("Вы уверены, что хотите завершить текущую задачу?")
                .setPositiveButton("Да", (dialog, which) -> stopTask())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void stopTask() {
        long endTime = System.currentTimeMillis();
        long timeSpent = endTime - startTime + currentTask.getTimeSpent();

        uploadProofImage(proofImageUri, imageUrl -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("active", false);
            updates.put("timeSpent", timeSpent);
            updates.put("completed", true);
            updates.put("proofImageUrl", imageUrl);
            updates.put("endTime", endTime);

            FirebaseDatabase.getInstance().getReference()
                    .child("userTasks")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child(getCurrentDate())
                    .child(currentTask.getId())
                    .updateChildren(updates);

            currentTask = null;
            proofImageUri = null;
            binding.proofImage.setVisibility(View.GONE);
            binding.removeProofBtn.setVisibility(View.GONE);
            binding.proofContainer.setVisibility(View.GONE);

            updateUI();
        });
    }

    private void uploadProofImage(Uri imageUri, ProofUploadCallback callback) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("task_proofs")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(currentTask.getId() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(callback::onSuccess);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки подтверждения", Toast.LENGTH_SHORT).show();
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void removeProofImage() {
        proofImageUri = null;
        binding.proofImage.setVisibility(View.GONE);
        binding.removeProofBtn.setVisibility(View.GONE);
    }

    private void updateUI() {
        if (currentTask != null) {
            binding.currentTaskText.setText("Текущая задача: " + currentTask.getName());
            binding.startTaskBtn.setVisibility(View.GONE);
            binding.stopTaskBtn.setVisibility(View.VISIBLE);
            binding.proofContainer.setVisibility(View.VISIBLE);
            binding.timer.setVisibility(View.VISIBLE);
        } else {
            binding.currentTaskText.setText("Нет активных задач");
            binding.startTaskBtn.setVisibility(View.VISIBLE);
            binding.stopTaskBtn.setVisibility(View.GONE);
            binding.proofContainer.setVisibility(View.GONE);
            binding.timer.setVisibility(View.GONE);
        }
    }

    private void updateTimer() {
        if (currentTask != null) {
            long elapsedMillis = System.currentTimeMillis() - startTime + currentTask.getTimeSpent();
            long seconds = (elapsedMillis / 1000) % 60;
            long minutes = (elapsedMillis / (1000 * 60)) % 60;
            long hours = (elapsedMillis / (1000 * 60 * 60)) % 24;

            String timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
            binding.timer.setText(timeString);

            if (currentTask != null) {
                binding.getRoot().postDelayed(this::updateTimer, 1000);
            }
        }
    }

    private String[] getTaskNames() {
        String[] names = new String[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            names[i] = tasks.get(i).getName();
        }
        return names;
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    interface ProofUploadCallback {
        void onSuccess(Uri imageUrl);
    }
}