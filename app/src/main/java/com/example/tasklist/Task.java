package com.example.tasklist;

public class Task {
    private long id;
    private String title;
    private String description;
    private boolean done;
    private long createdAt;

    public Task() {}


    public Task(long id, String title, String description, boolean done, long createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.done = done;
        this.createdAt = createdAt;
    }

    // старые конструкторы: без createdAt (создавая, createdAt присвоится в DBHelper)
    public Task(long id, String title, String description, boolean done) {
        this(id, title, description, done, 0);
    }

    public Task(String title, String description, boolean done) {
        this(-1, title, description, done, 0);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
