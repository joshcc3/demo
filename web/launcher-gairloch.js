var IN_DEV=false;
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
    if (IN_DEV) {
        return {ladderHost: "localhost:9044", workspaceHost: "localhost:9045"};
    }
    return {ladderHost: "prod-ladder.gairloch.drw:9044", workspaceHost: "prod-ladder.gairloch.drw:9045"};
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
