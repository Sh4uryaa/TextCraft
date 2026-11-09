package com.textcraft.model;

import java.time.LocalDateTime;

/** One row of the "My study sets" list (a flashcard set or a quiz). */
public class StudySetInfo {
    public enum Kind { FLASHCARDS, QUIZ }

    private final Kind kind;
    private final long id;
    private final String title;
    private final LocalDateTime createdAt;
    private final int items;

    public StudySetInfo(Kind kind, long id, String title, LocalDateTime createdAt, int items) {
        this.kind = kind;
        this.id = id;
        this.title = title;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Kind getKind() { return kind; }
    public long getId() { return id; }
    public String getTitle() { return title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getItems() { return items; }
}
