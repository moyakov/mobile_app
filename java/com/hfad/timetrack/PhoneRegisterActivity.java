package com.hfad.timetrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hfad.timetrack.databinding.ActivityPhoneRegisterBinding;

import java.util.concurrent.TimeUnit;

public class PhoneRegisterActivity extends AppCompatActivity {

    private ActivityPhoneRegisterBinding binding;
    private FirebaseAuth mAuth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setupClickListeners();
        setupPhoneInput();
    }

    private void setupClickListeners() {
        binding.registerButton.setOnClickListener(v -> attemptPhoneRegistration());
        binding.backButton.setOnClickListener(v -> navigateToRegister());
        binding.goToEmailRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void navigateToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }

    private void setupPhoneInput() {
        binding.phoneEditt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && s.toString().equals("8")) {
                    binding.phoneEditt.setText("+7");
                    binding.phoneEditt.setSelection(2);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void attemptPhoneRegistration() {
        String phoneNumber = binding.phoneEditt.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+7" + phoneNumber.replaceFirst("^7|^8", "");
        }

        showLoading(true);
        sendVerificationCode(phoneNumber);
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                showLoading(false);
                                signInWithPhoneAuthCredential(credential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                showLoading(false);
                                Toast.makeText(PhoneRegisterActivity.this,
                                        "Verification failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId,
                                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                showLoading(false);
                                PhoneRegisterActivity.this.verificationId = verificationId;
                                resendingToken = token;
                                navigateToOtpVerification(binding.phoneEditt.getText().toString().trim());
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(PhoneRegisterActivity.this,
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToOtpVerification(String phoneNumber) {
        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra("phoneNumber", phoneNumber);
        intent.putExtra("verificationId", verificationId);
        startActivity(intent);
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }

    private void showLoading(boolean isLoading) {
        binding.registerButton.setEnabled(!isLoading);
        binding.backButton.setEnabled(!isLoading);

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
        binding = null;
    }
}