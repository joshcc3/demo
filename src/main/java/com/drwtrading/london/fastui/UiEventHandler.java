package com.drwtrading.london.fastui;

import java.util.Map;

public interface UiEventHandler {

    public void onClick(final String id, final Map<String, String> data);

    public void onDblClick(final String id, final Map<String, String> data);

    public void onUpdate(final String id, final Map<String, String> data);

    public void onScroll(final String direction);

    public void onKeyDown(final int keyCode);

    public void onHeartbeat(final long sentTimeMillis);
}
