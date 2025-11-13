package com.example.ddquest.model;

import java.util.List;

public class Question {
    private String question;
    private List<String> options;
    private int answer;
    private int time;

    public Question() {}

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public int getAnswer() { return answer; }
    public void setAnswer(int answer) { this.answer = answer; }
    public int getTime() { return time; }
    public void setTime(int time) { this.time = time; }
}