package com.pshkrh.realtimelist.Model;

/**
 * Created by pshkr on 18-03-2018.
 */

public class ToDo {
    public String id,title,description,username;

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
}
