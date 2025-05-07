package com.mahdi.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class ListQuizzes extends AppCompatActivity {

    private boolean showGrade;
    private boolean solvedQuizzes;
    private boolean createdQuizzes;
    private boolean quizGrades;
    private String uid;
    private ArrayList<String> ids;
    private ArrayList<String> grades;
    private String quizID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_quizzes);

        String oper = getIntent().getStringExtra("Operation");
        TextView title = findViewById(R.id.title);
        ListView listview = findViewById(R.id.listview);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ids = new ArrayList<>();
        grades = new ArrayList<>();
        ArrayList<String> data = new ArrayList<>();

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        if (oper.equals("List Solved Quizzes")) {
            showGrade = false;
            solvedQuizzes = true;
        } else if (oper.equals("List Created Quizzes")) {
            showGrade = false;
            createdQuizzes = true;
        } else if (oper.equals("List Quiz Grades")) {
            quizID = getIntent().getStringExtra("Quiz ID");
            title.setText(quizID);
            quizGrades = true;
            showGrade = true;

            title.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Quiz ID", quizID);
                clipboard.setPrimaryClip(clip);
                return true;
            });
        }

        if (solvedQuizzes) {
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    DataSnapshot ds = snapshot.child("Users").child(uid).child("Quizzes Solved");
                    for (DataSnapshot f : ds.getChildren()) {
                        ids.add(f.getKey());
                        DataSnapshot quiz = snapshot.child("Quizzes").child(f.getKey());
                        String title = quiz.child("Title").getValue() != null ?
                                quiz.child("Title").getValue().toString() : "Untitled Quiz";
                        data.add(title);
                    }

                    listview.setAdapter(new ListAdapter(data));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ListQuizzes.this, "Can't connect", Toast.LENGTH_SHORT).show();
                }
            });

        } else if (createdQuizzes) {
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    DataSnapshot ds = snapshot.child("Users").child(uid).child("Quizzes Created");
                    for (DataSnapshot f : ds.getChildren()) {
                        ids.add(f.getKey());
                        DataSnapshot quiz = snapshot.child("Quizzes").child(f.getKey());
                        String title = quiz.child("Title").getValue() != null ?
                                quiz.child("Title").getValue().toString() : "Untitled Quiz";
                        data.add(title);
                    }

                    listview.setAdapter(new ListAdapter(data));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ListQuizzes.this, "Can't connect", Toast.LENGTH_SHORT).show();
                }
            });

        } else if (quizGrades) {
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    DataSnapshot ds = snapshot.child("Quizzes").child(quizID).child("Answers");
                    for (DataSnapshot f : ds.getChildren()) {
                        String userId = f.getKey();
                        ids.add(userId);

                        String firstName = snapshot.child("Users").child(userId)
                                .child("First Name").getValue(String.class);
                        String lastName = snapshot.child("Users").child(userId)
                                .child("Last Name").getValue(String.class);
                        String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                        data.add(fullName);

                        String points = f.child("Points").getValue() != null ?
                                f.child("Points").getValue().toString() : "0";
                        String total = snapshot.child("Quizzes").child(quizID)
                                .child("Total Questions").getValue() != null ?
                                snapshot.child("Quizzes").child(quizID).child("Total Questions").getValue().toString() : "?";
                        grades.add(points + "/" + total);
                    }

                    listview.setAdapter(new ListAdapter(data));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ListQuizzes.this, "Can't connect", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    public class ListAdapter extends BaseAdapter {
        ArrayList<String> arr;

        ListAdapter(ArrayList<String> arr2) {
            arr = arr2;
        }

        @Override
        public int getCount() {
            return arr.size();
        }

        @Override
        public Object getItem(int i) {
            return arr.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = getLayoutInflater();

            // اگر کاربر لیست آزمون‌های ساخته‌شده رو می‌بینه
            if (createdQuizzes) {
                @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.quiz_created_item, null);

                TextView quiz = v.findViewById(R.id.quiz);
                ImageView deleteBtn = v.findViewById(R.id.delete_btn);
                RelativeLayout item = v.findViewById(R.id.item);

                quiz.setText(arr.get(i));

                item.setOnClickListener(view1 -> {
                    Intent intent = new Intent(ListQuizzes.this, ListQuizzes.class);
                    intent.putExtra("Operation", "List Quiz Grades");
                    intent.putExtra("Quiz ID", ids.get(i));
                    intent.putExtra("Quiz Title", arr.get(i));
                    startActivity(intent);
                });

                deleteBtn.setOnClickListener(view12 -> {
                    new android.app.AlertDialog.Builder(ListQuizzes.this)
                            .setTitle("حذف آزمون")
                            .setMessage("آیا از حذف این آزمون اطمینان دارید؟")
                            .setPositiveButton("بله", (dialog, which) -> {
                                String quizIdToDelete = ids.get(i);
                                DatabaseReference db = FirebaseDatabase.getInstance().getReference();

                                // 1. حذف آزمون اصلی
                                db.child("Quizzes").child(quizIdToDelete).removeValue();

                                // 2. حذف از لیست ساخته‌شده‌ها
                                db.child("Users").child(uid).child("Quizzes Created").child(quizIdToDelete).removeValue();

                                // 3. حذف از Quizzes Solved همه‌ی کاربران
                                db.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                            if (userSnapshot.child("Quizzes Solved").hasChild(quizIdToDelete)) {
                                                userSnapshot.getRef().child("Quizzes Solved").child(quizIdToDelete).removeValue();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(ListQuizzes.this, "خطا در حذف از Quizzes Solved", Toast.LENGTH_SHORT).show();
                                    }
                                });

                                // حذف از لیست نمایش‌داده‌شده
                                arr.remove(i);
                                ids.remove(i);
                                notifyDataSetChanged();

                                Toast.makeText(ListQuizzes.this, "آزمون حذف شد", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("خیر", null)
                            .show();
                });


                return v;
            }

            @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.quizzes_listitem, null);
            TextView grade = v.findViewById(R.id.grade);
            TextView quiz = v.findViewById(R.id.quiz);
            RelativeLayout item = v.findViewById(R.id.item);

            quiz.setText(arr.get(i));

            if (showGrade) {
                grade.setVisibility(View.VISIBLE);
            } else {
                grade.setVisibility(View.GONE);
            }

            if (solvedQuizzes) {
                item.setOnClickListener(view1 -> {
                    Intent intent = new Intent(ListQuizzes.this, Result.class);
                    intent.putExtra("Quiz ID", ids.get(i));
                    startActivity(intent);
                });
            } else if (quizGrades) {
                grade.setText(grades.get(i));
                item.setOnClickListener(view1 -> {
                    Intent intent = new Intent(ListQuizzes.this, Result.class);
                    intent.putExtra("Quiz ID", quizID);
                    intent.putExtra("User UID", ids.get(i));
                    startActivity(intent);
                });
            }

            return v;
        }

    }
}
