package com.mahdi.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;


public class Signup extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        // UI components
        EditText firstNameEditText = findViewById(R.id.first_name);
        EditText lastNameEditText = findViewById(R.id.last_name);
        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);
        EditText confirmPasswordEditText = findViewById(R.id.confirm_password);
        Button signupButton = findViewById(R.id.signup);
        TextView loginTextView = findViewById(R.id.login);

        // Handle signup button click
        signupButton.setOnClickListener(view -> {

            // Get user inputs
            String firstName = firstNameEditText.getText().toString().trim();
            String lastName = lastNameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            // Validation
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(Signup.this, "لطفاً تمام فیلدها را پر کنید", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(Signup.this, "ایمیل وارد شده معتبر نیست", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(Signup.this, "رمز عبور باید حداقل ۶ کاراکتر باشد", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(Signup.this, "رمز عبور و تکرار آن مطابقت ندارند", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create user with Firebase authentication
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(Signup.this, task -> {
                if (task.isSuccessful()) {
                    // Save user info in Firebase Realtime Database
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        DatabaseReference userRef = database.child("Users").child(user.getUid());
                        userRef.child("First Name").setValue(firstName);
                        userRef.child("Last Name").setValue(lastName);

                        // Navigate to home screen
                        Intent i = new Intent(Signup.this, Home.class);
                        i.putExtra("User UID", user.getUid());
                        startActivity(i);
                        finish();
                    }
                } else {
                    Toast.makeText(Signup.this, "ثبت‌نام انجام نشد. لطفاً دوباره تلاش کنید.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Handle login link click
        loginTextView.setOnClickListener(view -> {
            Intent i = new Intent(Signup.this, MainActivity.class);
            startActivity(i);
            finish();
        });
    }
}