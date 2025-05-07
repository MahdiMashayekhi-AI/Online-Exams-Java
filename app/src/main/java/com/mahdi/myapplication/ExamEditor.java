package com.mahdi.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class ExamEditor extends AppCompatActivity {

    private static ArrayList<Question> data;
    private RecyclerView listview;
    private int quizID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_editor);

        // Get quiz title from intent
        Bundle b = getIntent().getExtras();
        String quizTitle = b.getString("Quiz Title");

        // Set quiz title on screen
        TextView title = findViewById(R.id.title);
        title.setText(quizTitle);

        Button submit = findViewById(R.id.submit);

        // Initialize Firebase reference and fetch quiz ID
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("Quizzes").hasChild("Last ID")) {
                    String lID = snapshot.child("Quizzes").child("Last ID").getValue().toString();
                    quizID = Integer.parseInt(lID) + 1;
                } else {
                    quizID = 100000;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExamEditor.this, "ارتباط با سرور برقرار نشد", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize question list
        data = new ArrayList<>();
        data.add(new Question());
        listview = findViewById(R.id.listview);
        listview.setLayoutManager(new LinearLayoutManager(this));
        CustomAdapter customAdapter = new CustomAdapter(data);
        listview.setAdapter(customAdapter);

        // Enable drag and drop reorder
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(listview);

        EditText quizTimeEditText = findViewById(R.id.quiz_time);

        // Handle submit button click
        submit.setOnClickListener(v -> {
            String timeStr = quizTimeEditText.getText().toString().trim();

            if (timeStr.isEmpty()) {
                quizTimeEditText.setError("مدت زمان را وارد کنید");
                return;
            }

            int time;
            try {
                time = Integer.parseInt(timeStr);
            } catch (NumberFormatException e) {
                quizTimeEditText.setError("زمان وارد شده معتبر نیست");
                return;
            }

            if (time <= 0) {
                quizTimeEditText.setError("مدت زمان باید بیشتر از صفر باشد");
                return;
            }

            if (time > 600) {
                quizTimeEditText.setError("مدت زمان نباید بیشتر از ۶۰۰ دقیقه باشد");
                return;
            }

            // Validate all questions
            for (int i = 0; i < data.size(); i++) {
                Question q = data.get(i);
                if (q.getQuestion() == null || q.getQuestion().trim().isEmpty() ||
                        q.getOption1() == null || q.getOption1().trim().isEmpty() ||
                        q.getOption2() == null || q.getOption2().trim().isEmpty() ||
                        q.getOption3() == null || q.getOption3().trim().isEmpty() ||
                        q.getOption4() == null || q.getOption4().trim().isEmpty() ||
                        q.getCorrectAnswer() == 0) {
                    Toast.makeText(this, "لطفاً ابتدا سوال " + (i + 1) + " را کامل کنید!", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Save quiz metadata to Firebase
            DatabaseReference ref = database.child("Quizzes");
            ref.child("Last ID").setValue(quizID);
            ref.child(String.valueOf(quizID)).child("Title").setValue(quizTitle);
            ref.child(String.valueOf(quizID)).child("Total Questions").setValue(data.size());
            ref.child(String.valueOf(quizID)).child("Time").setValue(time);

            // Save each question to Firebase
            DatabaseReference qRef = ref.child(String.valueOf(quizID)).child("Questions");
            for (int i = 0; i < data.size(); i++) {
                String p = String.valueOf(i);
                qRef.child(p).child("Question").setValue(data.get(i).getQuestion());
                qRef.child(p).child("Option 1").setValue(data.get(i).getOption1());
                qRef.child(p).child("Option 2").setValue(data.get(i).getOption2());
                qRef.child(p).child("Option 3").setValue(data.get(i).getOption3());
                qRef.child(p).child("Option 4").setValue(data.get(i).getOption4());
                qRef.child(p).child("Ans").setValue(data.get(i).getCorrectAnswer());
            }

            // Register quiz under user in Firebase
            database.child("Users")
                    .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                    .child("Quizzes Created")
                    .child(String.valueOf(quizID)).setValue("");

            // Copy quiz ID to clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Quiz ID", String.valueOf(quizID));
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "آی‌دی آزمون شما: " + quizID + " کپی شد", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // Callback for drag and drop
    ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder dragged, @NonNull RecyclerView.ViewHolder target) {
            int from = dragged.getAdapterPosition();
            int to = target.getAdapterPosition();
            Collections.swap(data, from, to);
            listview.getAdapter().notifyItemMoved(from, to);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
    };

    // Adapter for RecyclerView
    public static class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        private final ArrayList<Question> arr;

        public CustomAdapter(ArrayList<Question> data) {
            this.arr = data;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final EditText question;
            private final RadioButton option1rb, option2rb, option3rb, option4rb;
            private final EditText option1et, option2et, option3et, option4et;
            private final LinearLayout new_question;
            private final RadioGroup radio_group;

            public ViewHolder(View view) {
                super(view);
                question = view.findViewById(R.id.question);
                option1rb = view.findViewById(R.id.option1rb);
                option2rb = view.findViewById(R.id.option2rb);
                option3rb = view.findViewById(R.id.option3rb);
                option4rb = view.findViewById(R.id.option4rb);
                option1et = view.findViewById(R.id.option1et);
                option2et = view.findViewById(R.id.option2et);
                option3et = view.findViewById(R.id.option3et);
                option4et = view.findViewById(R.id.option4et);
                new_question = view.findViewById(R.id.new_question);
                radio_group = view.findViewById(R.id.radio_group);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.question_edit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.setIsRecyclable(false);
            Question q = arr.get(position);

            // Set existing data to UI components
            holder.question.setText(q.getQuestion());
            holder.option1et.setText(q.getOption1());
            holder.option2et.setText(q.getOption2());
            holder.option3et.setText(q.getOption3());
            holder.option4et.setText(q.getOption4());

            // Mark the selected radio button
            switch (q.getCorrectAnswer()) {
                case 1: holder.option1rb.setChecked(true); break;
                case 2: holder.option2rb.setChecked(true); break;
                case 3: holder.option3rb.setChecked(true); break;
                case 4: holder.option4rb.setChecked(true); break;
            }

            // Listen to text changes and update the model
            holder.question.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    q.setQuestion(s.toString());
                }
            });
            holder.option1et.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    q.setOption1(s.toString());
                }
            });
            holder.option2et.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    q.setOption2(s.toString());
                }
            });
            holder.option3et.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    q.setOption3(s.toString());
                }
            });
            holder.option4et.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    q.setOption4(s.toString());
                }
            });

            // Handle radio button selection
            holder.radio_group.setOnCheckedChangeListener((group, checkedId) -> {
                if (holder.option1rb.isChecked()) q.setCorrectAnswer(1);
                else if (holder.option2rb.isChecked()) q.setCorrectAnswer(2);
                else if (holder.option3rb.isChecked()) q.setCorrectAnswer(3);
                else if (holder.option4rb.isChecked()) q.setCorrectAnswer(4);
            });

            // Show add new question button only for last item
            if (position == arr.size() - 1) {
                holder.new_question.setVisibility(View.VISIBLE);
                holder.new_question.setOnClickListener(v -> {
                    arr.add(new Question());
                    notifyDataSetChanged();
                });
            } else {
                holder.new_question.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return arr.size();
        }
    }

    // A simple TextWatcher to reduce code duplication
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
