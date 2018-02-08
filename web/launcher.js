const DEV_URLS = new Set(["localhost", "lnhq-wudrn01", "wud-ldnrn01"]);

function openLink(ladderHost, symbol) {
	const priceSplitPos = symbol.indexOf(';');
	let rawSymbol;
	if (0 < priceSplitPos) {
		rawSymbol = symbol.substr(0, priceSplitPos);
	} else {
		rawSymbol = symbol;
	}
	const link = 'http://' + ladderHost + '/ladder#' + symbol;
	window.open(link, rawSymbol, "dialog=yes,width=262,height=350");
}

function launchLadderAtPrice(symbol, price) {
	launchLadder(symbol + ";" + price);
}

function getLadderHosts(symbol) {

	const isDev = DEV_URLS.has(window.location.hostname);

	if (isDev) {
		return {ladderHost: "localhost:9044", workspaceHost: "localhost:9045"};
	} else if (symbol.match(/^[^:]*[FGHJKMNQUVXZ][0-9](;.*)?$/)) {
		return {ladderHost: "prod-futures-ladder.eeif.drw:9044", workspaceHost: "prod-futures-ladder.eeif.drw:9045"};
	} else {
		return {ladderHost: "prod-equities-ladder.eeif.drw:9044", workspaceHost: "prod-equities-ladder.eeif.drw:9045"};
	}
}

function getLadderUrl(symbol) {
	return getLadderHosts(symbol).ladderHost + "/ladder#" + symbol;
}

function launchLadder(symbol) {
	symbol = symbol.toUpperCase();
	const hosts = getLadderHosts(symbol);
	$.ajax({
		success: function (d, s, x) {
			if (d != "success") {
				openLink(hosts.ladderHost, symbol);
			}
		},
		error: function (e) {
			console.log('error', e)
		},
		url: "http://" + hosts.workspaceHost + "/open?symbol=" + symbol,
		dataType: "jsonp",
		crossDomain: true
	});
	launchBasket(symbol, true);
}

function launchBasket(symbol, noPopUp) {
	if (symbol.indexOf("-") != -1) {
		symbol = symbol.split("-")[0];
	}
	const basketHost = "http://prod-bop.eeif.drw:8113";
	$.ajax({
		success: function (d, s, x) {
			if (d != "success" && d != "none" && !noPopUp) {
				window.open(basketHost + "/ladders#" + symbol, "Basket " + symbol, "dialog=yes,width=800,height=470");
			}
		},
		error: function (e) {
			console.log('error', e);
		},
		url: basketHost + "/open?basket=" + symbol,
		dataType: "jsonp",
		crossDomain: true
	});
}

function webwormLink(symbols, date) {

	const d = date || new Date();

	const symbolTemplate = "(exchange:{{EXCHANGE}},name:{{SYMBOL}})";
	const linkTemplate = "http://grid:18222/?date:%27{{DATE}}%27,symbols:!({{SYMBOLS}})";

	const symbolList = symbols.map(function (s) {
		let symbol = s.symbol;
		const exchange = s.exchange.toUpperCase();
		const parts = symbol.split(" ");

		if (parts.length == 2) {
			symbol = parts[0];
		}

		return symbolTemplate
			.split("{{EXCHANGE}}").join(exchange)
			.split("{{SYMBOL}}").join(symbol);
	}).join(",");

	const link = linkTemplate
		.split("{{SYMBOLS}}").join(symbolList)
		.split("{{DATE}}").join(d.toISOString().split("T")[0]);

	console.log(link);
	popUp(link, undefined, 1400, 1000);

}

function popUp(url, name, width, height) {
	let windowHandle = window.open(url, name, 'dialog=yes,width=' + width + ',height=' + height);
	windowHandle.close();
	windowHandle = window.open(url, name, 'dialog=yes,width=' + width + ',height=' + height);
	windowHandle.focus();
	return false;
}
