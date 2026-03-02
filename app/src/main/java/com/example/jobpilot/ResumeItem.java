package com.example.jobpilot;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ResumeItem {
    public String content;
    // Firestrore 문서 ID 저장용 필드 추가
    public String documentId;
    public String date;
    public String title;
    public String growth;
    public String strength;
    public String motivation;
    public String future;


    // Firestore용
    public ResumeItem() { }


    //  모든 경우에 사용
    public ResumeItem(String content, String date, String title, String growth,
                      String strength, String motivation, String future) {
        this.content = content;
        this.date = date;
        this.title = title;
        this.growth = growth;
        this.strength = strength;
        this.motivation = motivation;
        this.future = future;
    }
    // getter, setter (필요하면)
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGrowth() { return growth; }
    public void setGrowth(String growth) { this.growth = growth; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }

    public String getFuture() { return future; }
    public void setFuture(String future) { this.future = future; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

}
