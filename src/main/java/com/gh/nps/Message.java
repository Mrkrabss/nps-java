package com.gh.nps;

public abstract class Message{
     int type;
     String userId;
     long time;
     public String sign;

    public Message(int type, String userId) {
        this.type=type;
        this.userId = userId;
        this.time = System.currentTimeMillis();
    }

    public  int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
    
}