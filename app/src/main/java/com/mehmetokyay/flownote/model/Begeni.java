package com.mehmetokyay.flownote.model;

import androidx.room.Entity;

@Entity(tableName = "begeni_table", primaryKeys = {"postId", "userId"})
public class Begeni {

    private int postId;
    private int userId;
    private long timestamp;

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
