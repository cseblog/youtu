package youtubecrawler.internal;

import com.github.khangnt.youtubecrawler.Const;
import com.github.khangnt.youtubecrawler.exception.HttpClientException;
import com.github.khangnt.youtubecrawler.model.youtube.WindowSettings;
import com.github.khangnt.youtubecrawler.model.youtube.data.YouTubeDataResponse;
import com.github.khangnt.youtubecrawler.model.youtube.data.YouTubeDataResponseParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;
import rx.Emitter;
import rx.Observable;
import rx.functions.Func1;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.khangnt.youtubecrawler.internal.Headers.*;


/**
 * Created by Khang NT on 10/30/17.
 * Email: khang.neon.1997@gmail.com
 */

public class Utils {
    private static final Pattern XS_DURATION_PATTERN =
            Pattern.compile("^(-)?P(([0-9]*)Y)?(([0-9]*)M)?(([0-9]*)D)?"
                    + "(T(([0-9]*)H)?(([0-9]*)M)?(([0-9.]*)S)?)?$");
    private static final String REG_WS_BUILD_ID = "(?i)['\"](build_id|PAGE_CL)['\"][\\s\\n]*?:[\\s\\n]*?['\"]?([^'\"]+?)['\"]?[\\s\\n]*?[,}]";
    private static final String REG_WS_BUILD_LABEL = "(?i)['\"](build_label|PAGE_BUILD_LABEL)['\"][\\s\\n]*?:[\\s\\n]*?['\"]?([^'\"]+?)['\"]?[\\s\\n]*?[,}]";
    private static final String REG_WS_CLIENT_NAME = "(?i)['\"](client_name|INNERTUBE_CONTEXT_CLIENT_NAME)['\"][\\s\\n]*?:[\\s\\n]*?['\"]?([^'\"]+?)['\"]?[\\s\\n]*?[,}]";
    private static final String REG_WS_CLIENT_VERSION = "(?i)['\"](client_version|INNERTUBE_CONTEXT_CLIENT_VERSION)['\"][\\s\\n]*?:[\\s\\n]*?['\"]?([^'\"]+?)['\"]?[\\s\\n]*?[,}]";
    private static final String REG_WS_VARIANT_CHECKSUM = "(?i)['\"](variants_checksum|VARIANTS_CHECKSUM)['\"][\\s\\n]*?:[\\s\\n]*?['\"]?([^'\"]+?)['\"]?[\\s\\n]*?[,}]";

    static void addXYouTubeHeader(Request.Builder builder, WindowSettings windowSettings) {
        builder.header(X_YOUTUBE_VARIANTS_CHECKSUM, windowSettings.getVariantChecksum())
                .header(X_YOUTUBE_PAGE_LABEL, windowSettings.getBuildLabel())
                .header(X_YOUTUBE_PAGE_CL, windowSettings.getBuildId())
                .header(X_YOUTUBE_CLIENT_VERSION, windowSettings.getClientVersion())
                .header(X_YOUTUBE_CLIENT_NAME, windowSettings.getClientName());
    }

    public static Request.Builder mobileWebPageDownloadRequestBuilder(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) throw new RuntimeException("Invalid url " + url);
        return new Request.Builder()
                .url(httpUrl
                        .newBuilder()
                        .setQueryParameter("disable_polymer", "true")
                        .build())
                .addHeader(HTTP_ACCEPT, C.BROWSER_ACCEPT)
                .addHeader(HTTP_ACCEPT_LANGUAGE, C.BROWSER_ACCEPT_LANGUAGE)
                .addHeader(HTTP_ACCEPT_CHARSET, C.BROWSER_ACCEPT_CHARSET)
                .addHeader(HTTP_USER_AGENT, C.MOBILE_BROWSER_USER_AGENT);
    }

    public static Request.Builder desktopWebPageDownloadRequestBuilder(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) throw new RuntimeException("Invalid url " + url);
        return new Request.Builder()
                .url(httpUrl
                        .newBuilder()
                        .setQueryParameter("disable_polymer", "true")
                        .build())
                .addHeader(HTTP_ACCEPT, C.BROWSER_ACCEPT)
                .addHeader(HTTP_ACCEPT_LANGUAGE, C.BROWSER_ACCEPT_LANGUAGE)
                .addHeader(HTTP_ACCEPT_CHARSET, C.BROWSER_ACCEPT_CHARSET)
                .addHeader(HTTP_USER_AGENT, C.DESKTOP_BROWSER_USER_AGENT);
    }

    public static <R> Observable<R> rx(Call call, Consumer<Response, R> responseConsumer) {
        return Observable.create(emitter -> {
            Response response = null;
            try {
                response = call.execute();
                emitter.onNext(responseConsumer.call(response));
                emitter.onCompleted();
            } catch (Exception ex) {
                emitter.onError(ex);
            } finally {
                closeQuietly(response);
            }
        }, Emitter.BackpressureMode.NONE);
    }

    public static Observable<String> rx(Call call) {
        return rx(call, response -> {
            if (response.code() / 100 == 2) {
                return response.body().string();
            } else {
                throw new HttpClientException(response.code(), response.message(),
                        response.body().string());
            }
        });
    }

    public static Func1<String, Observable<WindowSettings>> parseWindowSettings(Gson gson) {
        return webPage -> {
            Matcher matcher = RegexUtils.search("window\\.settings\\s*=\\s*(\\{.+?\\})\\s*;", webPage);
            if (matcher != null) {
                String windowSettingsJson = matcher.group(1);
                try {
                    WindowSettings windowSettings = gson.fromJson(windowSettingsJson, WindowSettings.class);
                    if (windowSettings.getClientName() != null
                            && windowSettings.getClientVersion() != null
                            && windowSettings.getVariantChecksum() != null) {
                        return Observable.just(windowSettings);
                    }
                } catch (Throwable ignore) {
                }
            }
            Matcher buildIdMatcher = RegexUtils
                    .search(REG_WS_BUILD_ID, webPage, "Couldn't parse window settings [bi]");
            Matcher buildLabelMatcher = RegexUtils
                    .search(REG_WS_BUILD_LABEL, webPage, "Couldn't parse window settings [bl]");
            Matcher clientNameMatcher = RegexUtils
                    .search(REG_WS_CLIENT_NAME, webPage, "Couldn't parse window settings [cn]");
            Matcher clientVersionMatcher = RegexUtils
                    .search(REG_WS_CLIENT_VERSION, webPage, "Couldn't parse window settings [cv]");
            Matcher variantChecksumMatcher = RegexUtils
                    .search(REG_WS_VARIANT_CHECKSUM, webPage, "Couldn't parse window settings [vc]");
            return Observable.just(new WindowSettings(
                    buildIdMatcher.group(2),
                    buildLabelMatcher.group(2),
                    clientNameMatcher.group(2),
                    clientVersionMatcher.group(2),
                    variantChecksumMatcher.group(2)
            ));
        };
    }

    private static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

    // unescape all \U0001F629 to 😩
    public static String unescapeUtf32(String source) {
        return RegexUtils.sub("\\\\U[0-9a-fA-F]{8}", source, matcher -> {
            String hex = matcher.group(0).replace("\\U", "0x");
            try {
                return new String(intToByteArray(Integer.decode(hex)), "utf-32");
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        });
    }

    public static <T> Func1<String, T> parseAjaxResponse(Gson gson, Class<T> tClass) {
        return ajaxRes -> {
            int offset = ajaxRes.indexOf("{");
            ajaxRes = unescapeUtf32(ajaxRes.substring(offset));
            return gson.fromJson(ajaxRes, tClass);
        };
    }

    public static Func1<String, YouTubeDataResponse> parseYouTubeDataResponse(Gson gson) {
        return rawAjaxRes -> {
            JsonObject jsonObject = gson.fromJson(handleRawAjaxResponse(rawAjaxRes), JsonObject.class);
            return YouTubeDataResponseParser.parse(jsonObject);
        };
    }

    private static String handleRawAjaxResponse(String ajaxRes) {
        int offset = ajaxRes.indexOf("{");
        return unescapeUtf32(ajaxRes.substring(offset));
    }

    public static Request createAjaxRequest(String url, String referer, WindowSettings windowSettings) {
        url = getYouTubeFullUrl(url);
        Request.Builder requestBuilder = new Request.Builder().url(url)
                .addHeader(HTTP_ACCEPT, C.BROWSER_ACCEPT)
                .addHeader(HTTP_REFERER, referer)
                .addHeader(HTTP_ACCEPT_CHARSET, C.BROWSER_ACCEPT_CHARSET)
                .addHeader(HTTP_ACCEPT_LANGUAGE, C.BROWSER_ACCEPT_LANGUAGE)
                .addHeader(HTTP_USER_AGENT, C.MOBILE_BROWSER_USER_AGENT);
        Utils.addXYouTubeHeader(requestBuilder, windowSettings);
        return requestBuilder.build();
    }

    public static String getYouTubeFullUrl(String endpoint) {
        if (endpoint.startsWith("//")) {
            return "https:" + endpoint;
        } else if (endpoint.startsWith("/")) {
            return "https://m.youtube.com" + endpoint;
        }
        return endpoint;
    }

    public static Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
        final Map<String, List<String>> queryPairs = new LinkedHashMap<>();
        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!queryPairs.containsKey(key)) {
                queryPairs.put(key, new LinkedList<>());
            }
            final String value = idx > 0 && pair.length() > idx + 1 ?
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            queryPairs.get(key).add(value);
        }
        return queryPairs;
    }

    public static void closeQuietly(@Nullable Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable ignore) {
        }
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static <T> boolean isEmpty(Collection<T> list) {
        return list == null || list.isEmpty();
    }

    public static <T> boolean isEmpty(List<String> list) {
        return list == null || list.isEmpty() || isEmpty(list.get(0));
    }

    public static String simpleXmlUnescape(String text) {
        StringBuilder result = new StringBuilder(text.length());
        int i = 0;
        int n = text.length();
        while (i < n) {
            char charAt = text.charAt(i);
            if (charAt != '&') {
                result.append(charAt);
                i++;
            } else {
                if (text.startsWith("&amp;", i)) {
                    result.append('&');
                    i += 5;
                } else if (text.startsWith("&apos;", i)) {
                    result.append('\'');
                    i += 6;
                } else if (text.startsWith("&quot;", i)) {
                    result.append('"');
                    i += 6;
                } else if (text.startsWith("&lt;", i)) {
                    result.append('<');
                    i += 4;
                } else if (text.startsWith("&gt;", i)) {
                    result.append('>');
                    i += 4;
                } else i++;
            }
        }
        return result.toString();
    }

    public static int safeParse(String number, int fallback) {
        try {
            return Integer.parseInt(number);
        } catch (Throwable ignore) {
        }
        return fallback;
    }

    public static float safeParse(String number, float fallback) {
        try {
            return Integer.parseInt(number);
        } catch (Throwable ignore) {
        }
        return fallback;
    }

    /**
     * Parses an xs:duration attribute value, returning the parsed duration in milliseconds.
     *
     * @param value The attribute value to decode.
     * @return The parsed duration in milliseconds.
     */
    public static long parseXsDuration(String value) {
        if (isEmpty(value)) return Const.UNKNOWN_VALUE;
        Matcher matcher = XS_DURATION_PATTERN.matcher(value);
        if (matcher.matches()) {
            boolean negated = !isEmpty(matcher.group(1));
            // Durations containing years and months aren't completely defined. We assume there are
            // 30.4368 days in a month, and 365.242 days in a year.
            String years = matcher.group(3);
            double durationSeconds = (years != null) ? Double.parseDouble(years) * 31556908 : 0;
            String months = matcher.group(5);
            durationSeconds += (months != null) ? Double.parseDouble(months) * 2629739 : 0;
            String days = matcher.group(7);
            durationSeconds += (days != null) ? Double.parseDouble(days) * 86400 : 0;
            String hours = matcher.group(10);
            durationSeconds += (hours != null) ? Double.parseDouble(hours) * 3600 : 0;
            String minutes = matcher.group(12);
            durationSeconds += (minutes != null) ? Double.parseDouble(minutes) * 60 : 0;
            String seconds = matcher.group(14);
            durationSeconds += (seconds != null) ? Double.parseDouble(seconds) : 0;
            long durationMillis = (long) (durationSeconds * 1000);
            return negated ? -durationMillis : durationMillis;
        } else {
            return (long) (Double.parseDouble(value) * 3600 * 1000);
        }
    }

    public static int compare(int var0, int var1) {
        return var0 < var1 ? -1 : (var0 == var1 ? 0 : 1);
    }

    public static String join(Collection<String> list, String delimiter) {
        if (isEmpty(list)) return "";
        Iterator<String> iterator = list.iterator();
        StringBuilder res = new StringBuilder(iterator.next());
        while (iterator.hasNext()) {
            res.append(delimiter).append(iterator.next());
        }
        return res.toString();
    }

    public static RuntimeException propagate(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw ((RuntimeException) throwable);
        } else {
            throw new RuntimeException(throwable);
        }
    }

    @Nullable
    public static String parseYouTubePlaylistIdFromUrl(String src) {
        if (isEmpty(src)) return null;
        Matcher matcher = Pattern.compile("[?|&|\\/]list=([a-zA-Z0-9-_]+)").matcher(src);
        return matcher.find() ? matcher.group(1) : null;
    }

}
