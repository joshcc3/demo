package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.reddal.util.TinyJsonDb;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AutoPullPersistence {

    private final TinyJsonDb jsonDb;

    public AutoPullPersistence(Path persistenceFile) throws IOException {
        jsonDb = new TinyJsonDb(persistenceFile);
    }

    public Map<Long, PullRule> getPullRules() {
        Map<Long, PullRule> rules = new HashMap<>();
        for (Map.Entry<String, JSONObject> entry : jsonDb.entries()) {
            try {
                PullRule pullRule = PullRule.fromJSON(entry.getValue());
                rules.put(pullRule.ruleID, pullRule);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return rules;
    }


    public void updateRule(PullRule pullRule) {
        try {
            jsonDb.put(Long.toString(pullRule.ruleID), pullRule);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteRule(PullRule pullRule) {
        jsonDb.remove(Long.toString(pullRule.ruleID));
    }

}
