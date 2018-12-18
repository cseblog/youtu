package youtubecrawler.model.youtube;

import com.google.gson.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.List;

import static com.github.khangnt.youtubecrawler.model.youtube.TypeAdapterUtils.*;

/**
 * Created by Khang NT on 10/27/17.
 * Email: khang.neon.1997@gmail.com
 */

public class ItemSection extends MultipleItemContent implements Continuation {
    public static final String ITEM_TYPE = "item_section";

    private String continuationToken;
    private String clickTrackingParams;

    public ItemSection(List<Content> contents, String continuationToken, String clickTrackingParams) {
        super(ITEM_TYPE, contents);
        this.continuationToken = continuationToken;
        this.clickTrackingParams = clickTrackingParams;
    }

    @Override
    public @Nullable String getContinuationToken() {
        return continuationToken;
    }

    @Override
    public @Nullable String getClickTrackingParams() {
        return clickTrackingParams;
    }

    public static final class TypeAdapter implements JsonDeserializer<ItemSection> {

        @Override
        public ItemSection deserialize(JsonElement json, Type typeOfT,
                                       JsonDeserializationContext context) throws JsonParseException {
            if (json instanceof JsonObject) {
                JsonObject jsonObj = json.getAsJsonObject();
                JsonArray contentsJsonArr = jsonObj.getAsJsonArray("contents");
                List<Content> contentList = parse(contentsJsonArr, context);
                String continuationToken = null;
                String clickTrackingParams = null;
                JsonArray continuations = jsonObj.getAsJsonArray("continuations");
                if (continuations != null) {
                    for (JsonElement jsonElement : continuations) {
                        JsonObject continuationObj = jsonElement.getAsJsonObject();
                        String itemType = safeGet(continuationObj, KEY_ITEM_TYPE, "");
                        if (NEXT_CONTINUATION_DATA_TYPE.equalsIgnoreCase(itemType)) {
                            continuationToken = continuationObj.get("continuation").getAsString();
                            clickTrackingParams = continuationObj.get("click_tracking_params")
                                    .getAsString();
                            break;
                        }
                    }
                }
                return new ItemSection(contentList, continuationToken, clickTrackingParams);
            } else {
                throw new JsonParseException("Invalid ItemSection structure");
            }
        }
    }
}
