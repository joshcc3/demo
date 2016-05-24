package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.reddal.ladders.LadderWorkspace;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import java.net.InetSocketAddress;
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
            final String user;
            if (request.remoteAddress() instanceof InetSocketAddress) {
                InetSocketAddress socketAddress = (InetSocketAddress) request.remoteAddress();
                user = socketAddress.getAddress().getHostAddress();
            } else {
                String addrString = request.remoteAddress().toString();
                user = addrString.substring(addrString.lastIndexOf(':'));
            }
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
