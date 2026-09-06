package com.yalig.snapstudy.models;

import java.io.Serializable;

public class Flashcard implements Serializable {
    private String id;
    private String question;
    private String answer;
    private String topic;
    private int intervalDays;
    private int repetitionCount;
    private int difficulty; // 1: Easy, 2: Good, 3: Hard, 4: Again

    public Flashcard() {
        this.intervalDays = 1;
        this.repetitionCount = 0;
        this.difficulty = 2;
    }

    public Flashcard(String id, String question, String answer, String topic) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.topic = topic;
        this.intervalDays = 1;
        this.repetitionCount = 0;
        this.difficulty = 2;
    }


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }
    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }
    public void incrementRepetition() { this.repetitionCount++; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

}
