package com.hfad.timetrack;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.hfad.timetrack.databinding.ActivityOtpVerificationBinding;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class OtpVerificationActivity extends AppCompatActivity {

    private ActivityOtpVerificationBinding binding;
    private FirebaseAuth mAuth;
    private String verificationId;
    private String phoneNumber;
    private CountDownTimer countDownTimer;
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        resendingToken = getIntent().getParcelableExtra("resendingToken");

        setupUI();
        setupOTPInputs();
        startCountdownTimer();
    }

    private void setupUI() {
        binding.phoneNumberText.setText("Код отправлен на " + formatPhoneNumber(phoneNumber));

        binding.verifyButton.setOnClickListener(v -> verifyOtp());
        binding.resendOtpButton.setOnClickListener(v -> resendOtp());
        binding.backButton.setOnClickListener(v -> navigateToRegister()); // Изменено здесь
    }

    private void navigateToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber.startsWith("+7")) {
            return "+7 (" + phoneNumber.substring(2, 5) + ") " +
                    phoneNumber.substring(5, 8) + "-" + phoneNumber.substring(8);
        }
        return phoneNumber;
    }

    private void setupOTPInputs() {
        EditText[] otpFields = {
                binding.otp1, binding.otp2, binding.otp3,
                binding.otp4, binding.otp5, binding.otp6
        };

        for (int i = 0; i < otpFields.length; i++) {
            final int currentIndex = i;
            final int nextIndex = i < otpFields.length - 1 ? i + 1 : i;
            final int prevIndex = i > 0 ? i - 1 : i;

            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && currentIndex < otpFields.length - 1) {
                        otpFields[nextIndex].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        otpFields[currentIndex].getText().toString().isEmpty() &&
                        currentIndex > 0) {
                    otpFields[prevIndex].requestFocus();
                    otpFields[prevIndex].setText("");
                    return true;
                }
                return false;
            });
        }
    }

    private void startCountdownTimer() {
        binding.resendOtpButton.setEnabled(false);
        binding.timerText.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                binding.timerText.setText(String.format("00:%02d", seconds));
            }

            @Override
            public void onFinish() {
                binding.resendOtpButton.setEnabled(true);
                binding.timerText.setVisibility(View.GONE);
            }
        }.start();
    }

    private void verifyOtp() {
        StringBuilder otpBuilder = new StringBuilder();
        otpBuilder.append(binding.otp1.getText().toString());
        otpBuilder.append(binding.otp2.getText().toString());
        otpBuilder.append(binding.otp3.getText().toString());
        otpBuilder.append(binding.otp4.getText().toString());
        otpBuilder.append(binding.otp5.getText().toString());
        otpBuilder.append(binding.otp6.getText().toString());

        String otp = otpBuilder.toString();

        if (otp.length() < 6) {
            Toast.makeText(this, "Пожалуйста, введите полный код OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        saveUserPhoneToDatabase();
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(OtpVerificationActivity.this,
                                "Ошибка верификации: " + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserPhoneToDatabase() {
        HashMap<String, String> userInfo = new HashMap<>();
        userInfo.put("phone", phoneNumber);
        userInfo.put("profileImage", "");

        FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .setValue(userInfo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(OtpVerificationActivity.this,
                                "Телефон успешно подтвержден",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resendOtp() {
        showLoading(true);
        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        showLoading(false);
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        showLoading(false);
                        Toast.makeText(OtpVerificationActivity.this,
                                "Ошибка при повторной отправке: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String newVerificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        showLoading(false);
                        verificationId = newVerificationId;
                        resendingToken = token;
                        Toast.makeText(OtpVerificationActivity.this,
                                "Новый OTP успешно отправлен",
                                Toast.LENGTH_SHORT).show();
                        startCountdownTimer();
                    }
                };

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                callbacks,
                resendingToken);
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }

    private void showLoading(boolean isLoading) {
        binding.verifyButton.setEnabled(!isLoading);
        binding.resendOtpButton.setEnabled(!isLoading);

        if (isLoading) {
            hideKeyboard();
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        binding = null;
    }


}