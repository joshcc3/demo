package com.drwtrading.london.fastui;

import java.util.Map;

public interface UiEventHandler {

    public void onClick(String id, Map<String, String> data);

    public void onDblClick(String id, Map<String, String> data);

    public void onUpdate(String id, Map<String, String> data);

    public void onScroll(String direction);

    public void onKeyDown(int keyCode);

    public void onIncoming(String[] args);
}
