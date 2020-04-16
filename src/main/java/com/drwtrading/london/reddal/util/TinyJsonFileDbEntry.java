package com.drwtrading.london.reddal.util;

import drw.london.json.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TinyJsonFileDbEntry implements Jsonable {

    public final String id;
    public final Long time;
    public final JSONObject value;
    final boolean deleted;

    TinyJsonFileDbEntry(final String id, final Long time, final JSONObject data, final boolean deleted) {
        this.id = id;
        this.time = time;
        this.value = data;
        this.deleted = deleted;
    }

    static TinyJsonFileDbEntry fromJson(final JSONObject jsonObject) throws JSONException {
        return new TinyJsonFileDbEntry(jsonObject.getString("id"), jsonObject.getLong("time"), jsonObject.getJSONObject("value"),
                jsonObject.has("deleted") && jsonObject.getBoolean("deleted"));
    }

    @Override
    public void toJson(final Appendable out) throws IOException {

        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("time", time);
            jsonObject.put("value", value);
            jsonObject.put("deleted", deleted);
            out.append(jsonObject.toString());
        } catch (final JSONException ignored) {
        }
    }
}
