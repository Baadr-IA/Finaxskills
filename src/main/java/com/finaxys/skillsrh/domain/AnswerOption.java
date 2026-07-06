package com.finaxys.skillsrh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "answer_option")
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "option_code", nullable = false, length = 1)
    private String optionCode;

    protected AnswerOption() {
    }

    public AnswerOption(Question question, String answerText, Boolean isCorrect, String optionCode) {
        this.question = question;
        this.answerText = answerText;
        this.isCorrect = isCorrect;
        this.optionCode = optionCode;
    }

    public Long getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    public String getAnswerText() {
        return answerText;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public String getOptionCode() {
        return optionCode;
    }
}

