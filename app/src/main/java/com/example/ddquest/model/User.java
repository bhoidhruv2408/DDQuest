package com.example.ddquest.model;

public class User {
    private String name;
    private String email;
    private int streak;
    private int completion;

    public User() {}

    public User(String name, String email, int streak, int completion) {
        this.name = name;
        this.email = email;
        this.streak = streak;
        this.completion = completion;
    }

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
    public int getCompletion() { return completion; }
    public void setCompletion(int completion) { this.completion = completion; }

    public int getDailyProgress() {
        return 0;
    }
}