package com.drwtrading.london.reddal.ladders;

import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import java.net.InetSocketAddress;

public class WorkspaceRequestHandler implements HttpHandler {

    private final LadderWorkspace ladderWorkspace;
    private final String host;
    private final int webPort;

    public WorkspaceRequestHandler(final LadderWorkspace ladderWorkspace, final String host, final int webPort) {
        this.ladderWorkspace = ladderWorkspace;
        this.host = host;
        this.webPort = webPort;
    }

    @Override
    public void handleHttpRequest(final HttpRequest request, final HttpResponse response, final HttpControl control) throws Exception {
        if ("GET".equals(request.method())) {
            final String user;
            if (request.remoteAddress() instanceof InetSocketAddress) {
                final InetSocketAddress socketAddress = (InetSocketAddress) request.remoteAddress();
                user = socketAddress.getAddress().getHostAddress();
            } else {
                final String addrString = request.remoteAddress().toString();
                user = addrString.substring(addrString.lastIndexOf(':'));
            }
            final String symbol = request.queryParam("symbol");
            final String content;
            response.status(200);
            if (symbol == null) {
                content = "fail";
            } else if (!ladderWorkspace.openLadderForUser(user, symbol)) {
                content = host + ":" + webPort + "/ladder#" + symbol;
            } else {
                content = "success";
            }
            final String reply;
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
