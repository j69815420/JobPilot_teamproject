package com.example.jobpilot;

import java.util.ArrayList;

public class FeedBackItem {

    public enum Type { DATE, REAL, PRACTICE }

    public Type type;
    public String date;
    public int score;            // 총점
    public int total_score;

    public String mode;
    public ArrayList<String> answers;
    public String review;
    public String answerReview;
    public String aiReview;
    public String voiceReview;

    public int answerScore;
    public int aiScore;
    public int voiceScore;

    // 실전 모드
    public FeedBackItem(Type type, String date, int score, int total_score,
                        int answerScore, int aiScore, int voiceScore,
                        String mode, String review, String answerReview, String aiReview, String voiceReview) {

        this.type = type;
        this.date = date;
        this.score = score;
        this.total_score = total_score;

        this.answerScore = answerScore;
        this.aiScore = aiScore;
        this.voiceScore = voiceScore;

        this.mode = mode;
        this.answers = new ArrayList<>();

        this.review = review;
        this.answerReview = answerReview;
        this.aiReview = aiReview;
        this.voiceReview = voiceReview;
    }


    // 날짜 헤더
    public FeedBackItem(Type type, String date) {
        this.type = type;
        this.date = date;

        this.score = 0;
        this.total_score = 0;
        this.answerScore = 0;
        this.aiScore = 0;
        this.voiceScore = 0;

        this.mode = "";
        this.answers = new ArrayList<>();

        this.review = "";
        this.answerReview = "";
        this.aiReview = "";
        this.voiceReview = "";
    }
}
