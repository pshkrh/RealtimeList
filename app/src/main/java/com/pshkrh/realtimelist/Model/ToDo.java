package com.pshkrh.realtimelist.Model;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Created by pshkr on 18-03-2018.
 */

public class ToDo {
    public String id,title,description,username;

    @ServerTimestamp
    private Date date;

    public ToDo() {
    }

    public ToDo(String id, String title, String description, String username) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUsername() {
        return username;
    }

    public int getPosition(int position){
        return position;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
