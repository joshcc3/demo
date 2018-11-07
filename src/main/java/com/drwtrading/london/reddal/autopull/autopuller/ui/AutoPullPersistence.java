package com.drwtrading.london.reddal.autopull.autopuller.ui;

import com.drwtrading.london.reddal.autopull.autopuller.rules.PullRule;
import com.drwtrading.london.reddal.util.TinyJsonDb;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AutoPullPersistence {

    private final TinyJsonDb jsonDb;

    public AutoPullPersistence(final Path persistenceFile) throws IOException {

        jsonDb = new TinyJsonDb(persistenceFile);
    }

    public Map<Long, PullRule> getPullRules() {

        final Map<Long, PullRule> rules = new HashMap<>();
        for (final Map.Entry<String, JSONObject> entry : jsonDb.entries()) {
            try {
                final PullRule pullRule = PullRule.fromJSON(entry.getValue());
                rules.put(pullRule.ruleID, pullRule);
            } catch (final JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return rules;
    }

    public void updateRule(final PullRule pullRule) {

        try {
            jsonDb.put(Long.toString(pullRule.ruleID), pullRule);
        } catch (final JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteRule(final long ruleID) {

        final String ruleIdentifier = Long.toString(ruleID);
        jsonDb.remove(ruleIdentifier);
    }
}
