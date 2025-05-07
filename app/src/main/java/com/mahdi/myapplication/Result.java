package com.mahdi.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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


public class Result extends AppCompatActivity {

    private Question[] data;
    private String uid;
    private String quizID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        quizID = getIntent().getStringExtra("Quiz ID");
        uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (getIntent().hasExtra("User UID")) uid = getIntent().getStringExtra("User UID");

        TextView title = findViewById(R.id.title);
        ListView listview = findViewById(R.id.listview);
        TextView total = findViewById(R.id.total);

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ValueEventListener listener = new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Check if the quiz exists
                if (snapshot.child("Quizzes").hasChild(quizID)) {
                    DataSnapshot ansRef = snapshot.child("Quizzes").child(quizID).child("Answers").child(uid);
                    DataSnapshot qRef = snapshot.child("Quizzes").child(quizID);

                    // Set quiz title
                    String quizTitle = qRef.child("Title").getValue() != null ? qRef.child("Title").getValue().toString() : "بدون عنوان";
                    title.setText(quizTitle);

                    // Get total questions count
                    String totalQuestionsStr = qRef.child("Total Questions").getValue() != null ? qRef.child("Total Questions").getValue().toString() : "0";
                    int num;
                    try {
                        num = Integer.parseInt(totalQuestionsStr);
                    } catch (NumberFormatException e) {
                        num = 0;
                    }

                    data = new Question[num];
                    int correctAns = 0;

                    // Iterate through all questions
                    for (int i = 0; i < num; i++) {
                        DataSnapshot qRef2 = qRef.child("Questions").child(String.valueOf(i));
                        Question question = new Question();

                        // Set the question and options
                        question.setQuestion(getSafeString(qRef2, "Question"));
                        question.setOption1(getSafeString(qRef2, "Option 1"));
                        question.setOption2(getSafeString(qRef2, "Option 2"));
                        question.setOption3(getSafeString(qRef2, "Option 3"));
                        question.setOption4(getSafeString(qRef2, "Option 4"));

                        // Get selected answer
                        int selectedAns = getSafeInt(ansRef, String.valueOf(i + 1));
                        question.setSelectedAnswer(selectedAns);

                        // Check if the answer is correct
                        int correct = getSafeInt(qRef2, "Ans");
                        if (correct == selectedAns) correctAns++;

                        question.setCorrectAnswer(correct);
                        data[i] = question;
                    }

                    // Set the total score
                    total.setText("امتیاز کل: " + correctAns + " از " + data.length);
                    ListAdapter listAdapter = new ListAdapter(data);
                    listview.setAdapter(listAdapter);
                } else {
                    finish();
                }
            }

            private String getSafeString(DataSnapshot snapshot, String key) {
                Object val = snapshot.child(key).getValue();
                return val != null ? val.toString() : "";
            }

            private int getSafeInt(DataSnapshot snapshot, String key) {
                Object val = snapshot.child(key).getValue();
                if (val instanceof Long) return ((Long) val).intValue();
                if (val instanceof String) {
                    try {
                        return Integer.parseInt((String) val);
                    } catch (NumberFormatException ignored) {}
                }
                return -1;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Result.this, "متاسفانه نمی‌توان به سرور متصل شد.", Toast.LENGTH_SHORT).show();
            }
        };
        database.addValueEventListener(listener);
    }

    public class ListAdapter extends BaseAdapter {
        Question[] arr;

        ListAdapter(Question[] arr2) {
            arr = arr2;
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

        @SuppressLint("SetTextI18n")
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            LayoutInflater inflater = getLayoutInflater();
            @SuppressLint({"InflateParams", "ViewHolder"}) View v = inflater.inflate(R.layout.question, null);

            TextView question = v.findViewById(R.id.question);
            RadioButton option1 = v.findViewById(R.id.option1);
            RadioButton option2 = v.findViewById(R.id.option2);
            RadioButton option3 = v.findViewById(R.id.option3);
            RadioButton option4 = v.findViewById(R.id.option4);
            TextView result = v.findViewById(R.id.result);

            // Set question and options
            question.setText(data[i].getQuestion());
            option1.setText(data[i].getOption1());
            option2.setText(data[i].getOption2());
            option3.setText(data[i].getOption3());
            option4.setText(data[i].getOption4());

            // Mark the selected option
            switch (data[i].getSelectedAnswer()) {
                case 1:
                    option1.setChecked(true);
                    break;
                case 2:
                    option2.setChecked(true);
                    break;
                case 3:
                    option3.setChecked(true);
                    break;
                case 4:
                    option4.setChecked(true);
                    break;
            }

            // Disable all options after answering
            option1.setEnabled(false);
            option2.setEnabled(false);
            option3.setEnabled(false);
            option4.setEnabled(false);

            result.setVisibility(View.VISIBLE);

            // Check if the answer is correct and set result message
            if (data[i].getSelectedAnswer() == data[i].getCorrectAnswer()) {
                result.setBackgroundResource(R.drawable.green_background);
                result.setTextColor(ContextCompat.getColor(Result.this, R.color.green_dark));
                result.setText("پاسخ صحیح");
            } else {
                result.setBackgroundResource(R.drawable.red_background);
                result.setTextColor(ContextCompat.getColor(Result.this, R.color.red_dark));
                result.setText("پاسخ اشتباه");

                // Highlight the correct answer
                switch (data[i].getCorrectAnswer()) {
                    case 1:
                        option1.setBackgroundResource(R.drawable.green_background);
                        break;
                    case 2:
                        option2.setBackgroundResource(R.drawable.green_background);
                        break;
                    case 3:
                        option3.setBackgroundResource(R.drawable.green_background);
                        break;
                    case 4:
                        option4.setBackgroundResource(R.drawable.green_background);
                        break;
                }
            }
            return v;
        }
    }
}
