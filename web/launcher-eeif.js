var IN_DEV = false;

function openLink(ladderHost, symbol) {
    var priceSplitPos = symbol.indexOf(';');
    var rawSymbol;
    if (0 < priceSplitPos) {
        rawSymbol = symbol.substr(0, priceSplitPos);
    } else {
        rawSymbol = symbol;
    }
    var link = 'http://' + ladderHost + '/ladder#' + symbol;
    window.open(link, rawSymbol, "dialog=yes,width=262,height=325");
}

function launchLadderAtPrice(symbol,price) {
    launchLadder(symbol+";"+price);
}

function getLadderHosts(symbol) {
    var equitiesHost = "prod-equities-ladder.eeif.drw:9044";
    var equitiesWorkspace = "prod-equities-ladder.eeif.drw:9045";

    var futuresHost = "prod-futures-ladder.eeif.drw:9044";
    var futuresWorkspace = "prod-futures-ladder.eeif.drw:9045";

    var devHost = "localhost:9044";
    var devWorkspace = "localhost:9045";

    var ladderHost;
    var workspaceHost;

    if (IN_DEV) {
        ladderHost = devHost;
        workspaceHost = devWorkspace;
    } else if (symbol.match(/^[^:]*[FGHJKMNQUVXZ][0-9](;.*)?$/)) {
        ladderHost = futuresHost;
        workspaceHost = futuresWorkspace;
    } else {
        ladderHost = equitiesHost;
        workspaceHost = equitiesWorkspace;
    }
    return {ladderHost: ladderHost, workspaceHost: workspaceHost};
}

function getLadderUrl(symbol) {
    return getLadderHosts(symbol).ladderHost + "/ladder#" + symbol;
}

function launchLadder(symbol) {
    symbol = symbol.toUpperCase();
    var hosts = getLadderHosts(symbol);
    $.ajax({
        success:function(d,s,x){
            if (d != "success") {
                openLink(hosts.ladderHost, symbol);
            }
        },
        error:function(e){
            console.log('error',e)
        },
        url:"http://"+hosts.workspaceHost+"/open?symbol="+symbol,
        dataType:"jsonp",
        crossDomain:true
    });
	launchBasket(symbol,true);
}

function launchBasket(symbol,noPopUp) {
    if (symbol.indexOf("-") != -1) {
	symbol = symbol.split("-")[0];
    }
    var basketHost = "http://prod-bop.eeif.drw:8113";
    $.ajax({
        success: function(d,s,x) {
            if (d != "success" && d != "none" && !noPopUp) {
				window.open(basketHost+"/ladders#" + symbol, "Basket " + symbol, "dialog=yes,width=800,height=470");
            }
        },
        error: function(e) {
            console.log('error', e);
        },
        url: basketHost + "/open?basket=" + symbol,
        dataType: "jsonp",
        crossDomain: true
    });
}

function webwormLink(symbols, date) {

    var d = date || new Date();

    var symbolTemplate = "(exchange:{{EXCHANGE}},name:{{SYMBOL}})";
    var linkTemplate = "http://grid:18222/?date:%27{{DATE}}%27,symbols:!({{SYMBOLS}})";

    var symbolList = symbols.map(function(s) {
        var symbol = s.symbol;
        var exchange = s.exchange.toUpperCase();
        var parts = symbol.split(" ");

        if (parts.length == 2) {
            symbol = parts[0];
        }

        return symbolTemplate
            .split("{{EXCHANGE}}").join(exchange)
            .split("{{SYMBOL}}").join(symbol);
    }).join(",");

    var link = linkTemplate
        .split("{{SYMBOLS}}").join(symbolList)
        .split("{{DATE}}").join(d.toISOString().split("T")[0]);

    console.log(link);
    popUp(link,  undefined, 1400, 1000);

}


function popUp(url, name, width, height) {
    var windowHandle = window.open(url, name, 'dialog=yes,width=' + width + ',height=' + height);
    windowHandle.close();
    windowHandle = window.open(url, name, 'dialog=yes,width=' + width + ',height=' + height);
    windowHandle.focus();
    return false;
}
