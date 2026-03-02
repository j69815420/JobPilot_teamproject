package com.example.jobpilot;

public class Message {
    private final String text;    // 메시지 내용
    private final boolean isUser; // true = 사용자, false = AI

    public Message(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }
}
