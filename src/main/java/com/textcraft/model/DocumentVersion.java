package com.textcraft.model;

import java.time.LocalDateTime;

/** One saved snapshot of a cloud document. */
public class DocumentVersion {
    private Long id;
    private Long documentId;
    private String content;
    private LocalDateTime savedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
}
