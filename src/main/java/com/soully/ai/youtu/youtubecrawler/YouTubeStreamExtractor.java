package com.soully.ai.youtu.youtubecrawler;

import com.github.khangnt.youtubecrawler.model.ExtractorResult;
import rx.Observable;

/**
 * Created by Khang NT on 11/8/17.
 * Email: khang.neon.1997@gmail.com
 */

public interface YouTubeStreamExtractor {
    class Options {
        private boolean markWatched;
        private boolean parseSubtitle;
        private boolean parseDashManifest;

        public Options(Builder builder) {
            this.markWatched = builder.markWatched;
            this.parseSubtitle = builder.parseSubtitle;
            this.parseDashManifest = builder.parseDashManifest;
        }

        public boolean isMarkWatched() {
            return markWatched;
        }

        public boolean isParseSubtitle() {
            return parseSubtitle;
        }

        public boolean isParseDashManifest() {
            return parseDashManifest;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private boolean markWatched = false;
            private boolean parseSubtitle = false;
            private boolean parseDashManifest = true;

            Builder() {
            }

            public Builder setMarkWatched(boolean markWatched) {
                this.markWatched = markWatched;
                return this;
            }

            public Builder setParseSubtitle(boolean parseSubtitle) {
                this.parseSubtitle = parseSubtitle;
                return this;
            }

            public Builder setParseDashManifest(boolean parseDashManifest) {
                this.parseDashManifest = parseDashManifest;
                return this;
            }

            public Options build() {
                return new Options(this);
            }
        }
    }

    Observable<ExtractorResult> extract(String vid, Options options);
}
