package com.soully.ai.youtu.youtubecrawler.exception;

/**
 * Created by Khang NT on 11/9/17.
 * Email: khang.neon.1997@gmail.com
 */

public class NotSupportedDashDynamicException extends ExtractorException {
    public NotSupportedDashDynamicException(String s, String videoId) {
        super(s, videoId);
    }

    public NotSupportedDashDynamicException(String s, Throwable throwable, String videoId) {
        super(s, throwable, videoId);
    }
}
