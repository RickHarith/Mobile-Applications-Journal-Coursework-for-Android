package com.example.mobileappcw.ui.home;

import java.util.List;

public class Note {
    private String id;
    private String title;
    private String content;
    private List<String> imageUrls;
    private String date;

    public Note() {
    }

    public Note(String id, String title, String content, List<String> imageUrls, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.imageUrls = imageUrls;
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }
}
