package com.gh.nps;

public class ConnectPortResponseMsg extends Message{

    public ConnectPortResponseMsg(String userId) {
        super(4, userId);
    }
    long connectId;
    int result;
    String Key;
}