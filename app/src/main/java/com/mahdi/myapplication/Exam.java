package com.mahdi.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;


public class Exam extends AppCompatActivity {
    private Question[] data;
    private String quizID;
    private String uid;
    private int oldTotalPoints = 0;
    private int oldTotalQuestions = 0;

    // Countdown Timer
    private CountDownTimer countDownTimer;
    private TextView timerTextView;
    private boolean isSubmitted = false;

    // Start a countdown timer with given time in milliseconds
    private void startTimer(long millisInFuture)    {
        timerTextView = findViewById(R.id.timerTextView);

        countDownTimer = new CountDownTimer(millisInFuture, 1000) {

            @SuppressLint("DefaultLocale")
            @Override
            public void onTick(long millisUntilFinished) {
                // Convert remaining time to minutes and seconds
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;

                // Update timer text on UI
                timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                if (!isSubmitted) {
                    Toast.makeText(Exam.this, "زمان آزمون تمام شد", Toast.LENGTH_SHORT).show();
                    gotoResultActivity();
                }
            }
        };

        // Start the timer
        countDownTimer.start();
    }

    // Navigate to the Result activity and pass quiz ID
    private void gotoResultActivity() {
        Intent intent = new Intent(Exam.this, Result.class);
        intent.putExtra("Quiz ID", quizID);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        quizID = getIntent().getStringExtra("Quiz ID");
        uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        ListView listview = findViewById(R.id.listview);
        Button submit = findViewById(R.id.submit);
        TextView title = findViewById(R.id.title);
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        ValueEventListener listener = new ValueEventListener() {

            // Helper method to get safe string from snapshot
            private String getStringSafe(DataSnapshot snapshot, String key) {
                if (snapshot.hasChild(key)) {
                    Object value = snapshot.child(key).getValue();
                    return value != null ? value.toString() : "";
                }
                return "";
            }

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.child("Quizzes").hasChild(quizID)) {
                    finish();
                    return;
                }

                DataSnapshot ref = snapshot.child("Quizzes").child(quizID);

                // Set title
                title.setText(getStringSafe(ref, "Title").isEmpty() ? "بدون عنوان" : getStringSafe(ref, "Title"));

                // Start timer
                String timeStr = getStringSafe(ref, "Time");
                if (!timeStr.isEmpty()) {
                    long timeInMinutes = Long.parseLong(timeStr);
                    startTimer(timeInMinutes * 60 * 1000);
                }

                // Load questions
                String totalQStr = getStringSafe(ref, "Total Questions");
                if (!totalQStr.isEmpty()) {
                    int num = Integer.parseInt(totalQStr);
                    data = new Question[num];

                    for (int i = 0; i < num; i++) {
                        DataSnapshot qRef = ref.child("Questions").child(String.valueOf(i));
                        Question q = new Question();
                        q.setQuestion(getStringSafe(qRef, "Question"));
                        q.setOption1(getStringSafe(qRef, "Option 1"));
                        q.setOption2(getStringSafe(qRef, "Option 2"));
                        q.setOption3(getStringSafe(qRef, "Option 3"));
                        q.setOption4(getStringSafe(qRef, "Option 4"));

                        String ansStr = getStringSafe(qRef, "Ans");
                        q.setCorrectAnswer(ansStr.isEmpty() ? -1 : Integer.parseInt(ansStr));

                        data[i] = q;
                    }

                    listview.setAdapter(new ListAdapter(data));
                }

                // Load user stats
                DataSnapshot userRef = snapshot.child("Users").child(uid);
                oldTotalPoints = Integer.parseInt(getStringSafe(userRef, "Total Points").isEmpty() ? "0" : getStringSafe(userRef, "Total Points"));
                oldTotalQuestions = Integer.parseInt(getStringSafe(userRef, "Total Questions").isEmpty() ? "0" : getStringSafe(userRef, "Total Questions"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", error.getMessage());
            }
        };

        database.addValueEventListener(listener);

        // Submit answers
        submit.setOnClickListener(v -> {
            if (data == null || data.length == 0) {
                Toast.makeText(Exam.this, "سؤالات بارگذاری نشده‌اند", Toast.LENGTH_SHORT).show();
                return;
            }

            // Stop the timer when user submits manually
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            isSubmitted = true;

            int points = 0;
            int totalPoints = oldTotalPoints;
            DatabaseReference answerRef = database.child("Quizzes").child(quizID).child("Answers").child(uid);

            for (int i = 0; i < data.length; i++) {
                int selected = data[i].getSelectedAnswer();
                answerRef.child(String.valueOf(i + 1)).setValue(selected);
                if (selected != -1 && selected == data[i].getCorrectAnswer()) {
                    points++;
                    totalPoints++;
                }
            }

            answerRef.child("Points").setValue(points);

            int totalQuestions = oldTotalQuestions + data.length;
            DatabaseReference userRef = database.child("Users").child(uid);
            userRef.child("Total Points").setValue(totalPoints);
            userRef.child("Total Questions").setValue(totalQuestions);
            userRef.child("Quizzes Solved").child(quizID).setValue("");

            // Move to result screen
            Intent i = new Intent(Exam.this, Result.class);
            i.putExtra("Quiz ID", quizID);
            startActivity(i);
            finish();
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public class ListAdapter extends BaseAdapter {

        private final Question[] arr;

        public ListAdapter(Question[] arr) {
            this.arr = arr;
        }

        @Override
        public int getCount() {
            return arr.length;
        }

        @Override
        public Object getItem(int i) {
            return arr[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(Exam.this).inflate(R.layout.question, parent, false);
                holder = new ViewHolder();
                holder.question = convertView.findViewById(R.id.question);
                holder.option1 = convertView.findViewById(R.id.option1);
                holder.option2 = convertView.findViewById(R.id.option2);
                holder.option3 = convertView.findViewById(R.id.option3);
                holder.option4 = convertView.findViewById(R.id.option4);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Question q = arr[i];
            holder.question.setText(q.getQuestion());
            holder.option1.setText(q.getOption1());
            holder.option2.setText(q.getOption2());
            holder.option3.setText(q.getOption3());
            holder.option4.setText(q.getOption4());

            // Clear listeners to prevent multiple triggers
            holder.option1.setOnCheckedChangeListener(null);
            holder.option2.setOnCheckedChangeListener(null);
            holder.option3.setOnCheckedChangeListener(null);
            holder.option4.setOnCheckedChangeListener(null);

            holder.option1.setChecked(q.getSelectedAnswer() == 1);
            holder.option2.setChecked(q.getSelectedAnswer() == 2);
            holder.option3.setChecked(q.getSelectedAnswer() == 3);
            holder.option4.setChecked(q.getSelectedAnswer() == 4);

            holder.option1.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) q.setSelectedAnswer(1);
            });
            holder.option2.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) q.setSelectedAnswer(2);
            });
            holder.option3.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) q.setSelectedAnswer(3);
            });
            holder.option4.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) q.setSelectedAnswer(4);
            });

            return convertView;
        }

        class ViewHolder {
            TextView question;
            RadioButton option1, option2, option3, option4;
        }
    }


}