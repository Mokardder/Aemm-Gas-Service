package android.iocl.dac_collector.RetrofitClient;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FlexibleDataDeserializer<T> implements JsonDeserializer<List<T>> {
    private final Class<T> clazz;

    public FlexibleDataDeserializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public List<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<T> result = new ArrayList<>();
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                result.add(context.deserialize(element, clazz));
            }
        } else if (json.isJsonObject()) {
            result.add(context.deserialize(json.getAsJsonObject(), clazz));
        }
        return result;
    }
}
