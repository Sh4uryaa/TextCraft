package com.textcraft.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A saved set of flashcards, optionally linked to a cloud document. */
public class FlashcardSet {
    private Long id;
    private Long userId;
    private Long documentId;
    private String title;
    private LocalDateTime createdAt;
    private List<Flashcard> cards = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<Flashcard> getCards() { return cards; }
    public void setCards(List<Flashcard> cards) { this.cards = cards; }
}
