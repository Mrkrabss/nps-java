package com.gh.nps;

public class HeartBeatMsg extends Message{

    public HeartBeatMsg(String userId) {
        super(2,userId);
    }
    
}