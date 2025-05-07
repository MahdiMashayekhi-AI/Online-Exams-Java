package com.mahdi.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;


public class Home extends AppCompatActivity {

    private String userUID;
    private String firstName;
    private ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Firebase database reference
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        // Show loading dialog
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        // Get user UID from Intent extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userUID = extras.getString("User UID");
        }

        // Initialize views
        TextView name = findViewById(R.id.name);
        TextView totalQuestions = findViewById(R.id.total_questions);
        TextView totalPoints = findViewById(R.id.total_points);
        Button startQuizButton = findViewById(R.id.startQuiz);
        Button createQuizButton = findViewById(R.id.createQuiz);
        RelativeLayout solvedQuizzesLayout = findViewById(R.id.solvedQuizzes);
        RelativeLayout yourQuizzesLayout = findViewById(R.id.your_quizzes);
        EditText quizTitleInput = findViewById(R.id.quiz_title);
        EditText startQuizIdInput = findViewById(R.id.start_quiz_id);
        ImageView signOutButton = findViewById(R.id.signout);

        // Firebase listener to fetch user data
        listener = new ValueEventListener() {
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot usersRef = snapshot.child("Users").child(userUID);

                // Get and show user's first name
                firstName = Objects.requireNonNull(usersRef.child("First Name").getValue()).toString();
                name.setText(firstName + " خوش آمدید!");

                // Display total points if exists
                if (usersRef.hasChild("Total Points")) {
                    String totalPointsStr = Objects.requireNonNull(usersRef.child("Total Points").getValue()).toString();
                    int totalPointsValue = Integer.parseInt(totalPointsStr);
                    totalPoints.setText(String.format("%03d", totalPointsValue));
                }

                // Display total questions if exists
                if (usersRef.hasChild("Total Questions")) {
                    String totalQuestionsStr = Objects.requireNonNull(usersRef.child("Total Questions").getValue()).toString();
                    int totalQuestionsValue = Integer.parseInt(totalQuestionsStr);
                    totalQuestions.setText(String.format("%03d", totalQuestionsValue));
                }

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Home.this, "خطا در اتصال به پایگاه داده", Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        };

        // Add the Firebase listener
        database.addValueEventListener(listener);

        // Sign out and return to login screen
        signOutButton.setOnClickListener(view -> {
            database.removeEventListener(listener);
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Home.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Create new quiz
        createQuizButton.setOnClickListener(v -> {
            String title = quizTitleInput.getText().toString().trim();

            if (title.isEmpty()) {
                quizTitleInput.setError("عنوان آزمون نمی‌تواند خالی باشد");
                return;
            }

            DatabaseReference quizzesRef = FirebaseDatabase.getInstance().getReference().child("Quizzes");

            quizzesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean exists = false;

                    // Check if quiz title already exists
                    for (DataSnapshot quiz : snapshot.getChildren()) {
                        String existingTitle = quiz.child("Title").getValue(String.class);
                        if (existingTitle != null && existingTitle.equalsIgnoreCase(title)) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        quizTitleInput.setError("آزمونی با این عنوان وجود دارد");
                    } else {
                        // Start quiz creation activity
                        Intent intent = new Intent(Home.this, ExamEditor.class);
                        intent.putExtra("Quiz Title", title);
                        quizTitleInput.setText("");
                        startActivity(intent);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(Home.this, "خطا در بررسی عنوان آزمون", Toast.LENGTH_LONG).show();
                }
            });
        });

        // Start quiz by ID or title
        startQuizButton.setOnClickListener(v -> {
            String input = startQuizIdInput.getText().toString().trim();

            if (input.isEmpty()) {
                startQuizIdInput.setError("عنوان یا شناسه آزمون نمی‌تواند خالی باشد");
                return;
            }

            DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("Quizzes");

            db.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String quizId = null;

                    // Try to find quiz by ID or title
                    if (snapshot.hasChild(input)) {
                        quizId = input;
                    } else {
                        for (DataSnapshot quizSnapshot : snapshot.getChildren()) {
                            String title = quizSnapshot.child("Title").getValue(String.class);
                            if (title != null && title.equalsIgnoreCase(input)) {
                                quizId = quizSnapshot.getKey();
                                break;
                            }
                        }
                    }

                    if (quizId != null) {
                        Intent intent = new Intent(Home.this, Exam.class);
                        intent.putExtra("Quiz ID", quizId);
                        startQuizIdInput.setText("");
                        startActivity(intent);
                    } else {
                        Toast.makeText(Home.this, "آزمونی با این عنوان یا شناسه پیدا نشد", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(Home.this, "خطا در ارتباط با پایگاه داده", Toast.LENGTH_LONG).show();
                }
            });
        });

        // View solved quizzes
        solvedQuizzesLayout.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, ListQuizzes.class);
            intent.putExtra("Operation", "List Solved Quizzes");
            startActivity(intent);
        });

        // View quizzes created by user
        yourQuizzesLayout.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, ListQuizzes.class);
            intent.putExtra("Operation", "List Created Quizzes");
            startActivity(intent);
        });
    }
}