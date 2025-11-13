package com.example.ddquest;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ddquest.model.Question;
import com.example.ddquest.model.Score;
import com.example.ddquest.utils.FirebaseUtil;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DailyTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_test);

        fetchTodayQuiz();
    }

    private void fetchTodayQuiz() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String subject = getCurrentSubject();

        FirebaseUtil.getDb().collection("quizzes")
                .document(subject + "_" + today)
                .collection("questions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Convert to List<Question>
                    List<Question> questions = queryDocumentSnapshots.toObjects(Question.class);
                    // TODO: Display in RecyclerView
                    Toast.makeText(this, "Loaded " + questions.size() + " questions", Toast.LENGTH_SHORT).show();
                });
    }

    private String getCurrentSubject() {
        String[] subjects = {"Mathematics", "Physics", "Chemistry", "English", "Aptitude"};
        int day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
        return subjects[day % subjects.length];
    }

    public void submitQuiz(int score, int total) {
        Score s = new Score();
        s.setUserId(FirebaseUtil.currentUserId());
        s.setQuizId("daily_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        s.setScore(score);
        s.setTotal(total);
        s.setTimestamp(Timestamp.now());

        FirebaseUtil.getDb().collection("scores").add(s)
                .addOnSuccessListener(aVoid -> {
                    updateUserStreak();
                    Toast.makeText(this, "Score saved!", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserStreak() {
        String uid = FirebaseUtil.currentUserId();
        FirebaseUtil.getDb().collection("users").document(uid)
                .update("streak", com.google.firebase.firestore.FieldValue.increment(1));
    }
}