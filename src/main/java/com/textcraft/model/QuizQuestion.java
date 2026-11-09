package com.textcraft.model;

/** One multiple choice question with 4 options (A-D). */
public class QuizQuestion {
    private Long id;
    private String question;
    private String[] options = new String[4];
    private char correct = 'A';
    private String explanation = "";

    public QuizQuestion() { }

    public QuizQuestion(String question, String[] options, char correct, String explanation) {
        this.question = question;
        this.options = options;
        this.correct = correct;
        this.explanation = explanation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String[] getOptions() { return options; }
    public void setOptions(String[] options) { this.options = options; }
    public char getCorrect() { return correct; }
    public void setCorrect(char correct) { this.correct = correct; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    /** 0..3 for A..D */
    public int getCorrectIndex() {
        return correct - 'A';
    }
}
