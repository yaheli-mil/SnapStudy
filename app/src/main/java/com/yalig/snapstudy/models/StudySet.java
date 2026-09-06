package com.yalig.snapstudy.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StudySet implements Serializable {
    private String id;
    private String title;
    private String category;
    private String updatedInfo;
    private int masteryPercentage;
    private List<Flashcard> flashcards;

    public StudySet() {
        this.flashcards = new ArrayList<>();
    }

    public StudySet(String id, String title, String category, String updatedInfo, int masteryPercentage) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.updatedInfo = updatedInfo;
        this.masteryPercentage = masteryPercentage;
        this.flashcards = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUpdatedInfo() { return updatedInfo; }
    public void setUpdatedInfo(String updatedInfo) { this.updatedInfo = updatedInfo; }
    public int getMasteryPercentage() { return masteryPercentage; }
    public void setMasteryPercentage(int masteryPercentage) { this.masteryPercentage = masteryPercentage; }
    public List<Flashcard> getFlashcards() { return flashcards; }
    public void setFlashcards(List<Flashcard> flashcards) { this.flashcards = flashcards; }
    @com.google.firebase.firestore.Exclude
    public int getCardCount() { return flashcards != null ? flashcards.size() : 0; }
    public void addFlashcard(Flashcard card) {
        if (this.flashcards == null) {
            this.flashcards = new ArrayList<>();
        }
        this.flashcards.add(card);
    }
}
