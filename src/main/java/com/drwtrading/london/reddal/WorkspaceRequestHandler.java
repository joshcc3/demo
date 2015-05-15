package com.drwtrading.london.reddal;

import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import java.net.MalformedURLException;

public class WorkspaceRequestHandler implements HttpHandler {
    private final LadderWorkspace ladderWorkspace;
    private String host;
    private int webPort;

    public WorkspaceRequestHandler(LadderWorkspace ladderWorkspace, String host, int webPort) throws MalformedURLException {
        this.ladderWorkspace = ladderWorkspace;
        this.host = host;
        this.webPort = webPort;
    }

    @Override
    public void handleHttpRequest(final HttpRequest request, final HttpResponse response, final HttpControl control) throws Exception {
        if ("GET".equals(request.method())) {
            String user = request.remoteAddress().toString().split(":")[0].substring(1);
            String symbol = request.queryParam("symbol");
            String content;
            response.status(200);
            if (symbol == null) {
                content = "fail";
            } else if (!ladderWorkspace.openLadderForUser(user, symbol)) {
                content = host + ":" + webPort + "/ladder#" + symbol;
            } else {
                content = "success";
            }
            String reply;
            if (request.queryParamKeys().contains("callback")) {
                reply = request.queryParam("callback") + "(\"" + content + "\")";
            } else {
                reply = content;
            }
            response.content(reply);
            response.end();
        }
    }
}
