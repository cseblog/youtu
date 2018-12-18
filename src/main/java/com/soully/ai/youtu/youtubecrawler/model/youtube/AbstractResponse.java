package com.soully.ai.youtu.youtubecrawler.model.youtube;

/**
 * Created by Khang NT on 10/31/17.
 * Email: khang.neon.1997@gmail.com
 */

public abstract class AbstractResponse {
    public static final String RESULT_OK = "ok";

    private String result;
    private long timestamp;

    public AbstractResponse(String result, long timestamp) {
        this.result = result;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return RESULT_OK.equalsIgnoreCase(result);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public abstract boolean hasContinuation();

    public abstract Continuation getContinuation();

}
