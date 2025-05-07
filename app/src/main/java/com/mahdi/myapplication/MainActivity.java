package com.mahdi.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        Button login = findViewById(R.id.login);
        TextView signup = findViewById(R.id.signup);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase authentication
        auth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // If the user is logged in, redirect to Home Activity
            Intent intent = new Intent(MainActivity.this, Home.class);
            intent.putExtra("User UID", user.getUid());
            startActivity(intent);
            finish();
        }

        // Handle login button click
        login.setOnClickListener(view -> {
            // Show ProgressBar while logging in
            progressBar.setVisibility(View.VISIBLE);

            // Get the entered email and password
            String emailText = email.getText().toString().trim();
            String passwordText = password.getText().toString().trim();

            // Validate email and password
            if (emailText.isEmpty() || passwordText.isEmpty()) {
                progressBar.setVisibility(View.GONE); // Hide ProgressBar
                Toast.makeText(MainActivity.this, "لطفاً همه فیلدها را پر کنید", Toast.LENGTH_SHORT).show();
                return;
            }

            // Attempt to sign in with Firebase authentication in a background thread
            new Thread(() -> {
                auth.signInWithEmailAndPassword(emailText, passwordText)
                        .addOnCompleteListener(MainActivity.this, task -> {
                            // Hide ProgressBar after task completion
                            runOnUiThread(() -> progressBar.setVisibility(View.GONE));

                            if (task.isSuccessful()) {
                                // If login is successful, go to Home Activity
                                FirebaseUser user1 = auth.getCurrentUser();
                                runOnUiThread(() -> {
                                    Intent intent = new Intent(MainActivity.this, Home.class);
                                    intent.putExtra("User UID", user1.getUid());
                                    startActivity(intent);
                                    finish();
                                });
                            } else {
                                // If login fails, show error message in Persian
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "نام کاربری یا رمز عبور اشتباه است", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Handle failure scenario
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "خطا در اتصال به سرور: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        });
            }).start();
        });

        // Handle signup link click
        signup.setOnClickListener(view -> {
            // Redirect to SignupActivity for user registration
            Intent intent = new Intent(MainActivity.this, Signup.class);
            startActivity(intent);
            finish();
        });
    }
}
