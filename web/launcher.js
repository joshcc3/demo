function openLink(ladderHost, symbol) {
    var link = 'http://' + ladderHost + '/ladder#' + symbol;
    window.open(link, symbol, "dialog=yes,width=250,height=315");
}
function launchLadder(symbol) {

    symbol = symbol.toUpperCase();

    var equitiesHost = "prod-equities-ladder.eeif.drw:9044";
    var futuresHost = "prod-futures-ladder.eeif.drw:9044";
//    var futuresHost = "localhost:8812";
    var ladderHost;

    if (symbol.match(/^(ES|RP|RF).*/) && symbol.length == 4) {

        ladderHost = "sup-aurdfas01:9004";
        openLink(ladderHost, symbol);
    } else if (symbol.match(/^(MPSI|JFCE|KFTI|Z|FBXF|XZ|ER|SS).*/)) {
        ladderHost = "sup-ldndf04:9004";
        openLink(ladderHost, symbol);
    } else if (symbol.match(/^[^:]*[FGHJKMNQUVXZ][0-9]$/)) {
        ladderHost = futuresHost;
    } else {
        ladderHost = equitiesHost;
    }

    $.get('http://' + ladderHost + "/open?symbol=" + symbol, null, function (result) {
        console.log(result);
        if (result != "success" && result != "fail") {
            window.open(result, symbol, "dialog=yes,width=250,height=315")
        }
    });

}

function popUp(url, name, width, height) {
    var windowHandle = window.open(url, name, 'dialog=yes,width=' + width + ',height=' + height);
    windowHandle.close();
    windowHandle = window.open(url, name, 'dialog=yes,width=' + width + ',height=' + height);
    windowHandle.focus();
    return false;
}

function httpGet(theUrl) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open("GET", theUrl, true);
    xmlHttp.send(null);
    if (xmlHttp.status == 200) {
        return xmlHttp.responseText;
    } else {
        return false;
    }
}