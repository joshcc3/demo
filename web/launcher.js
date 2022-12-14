const LADDER_HOST_PROP = "ladderHost";
const WORKSPACE_HOST_PROP = "workspaceHost";

const DEV_URLS = new Set(["localhost", "lnhq-wudrn01", "wud-ldnrn01", "lnlmp-rnewton1", "lnwld-jcoutinh1", "lnlmx-jcoutinh2", "lnwld-mlouis1", "lnlmd-mlouis1"]);

const FUTURES_LADDER_URLS = {ladderHost: "prod-futures-ladder.eeif.drw:9044", workspaceHost: "prod-futures-ladder.eeif.drw:9045"};
const SELECTA_LADDERS_URLS = "http://prod-selecta.eeif.drw:9134/ladders_urls";

let EQUITY_LADDER_URLS = {ladderHost: "prod-equities-ladder.eeif.drw:9144", workspaceHost: "prod-equities-ladder.eeif.drw:9145"};
let RFQ_LADDER_URLS = {ladderHost: "prod-rfq-ladder.eeif.drw:9244", workspaceHost: "prod-rfq-ladder.eeif.drw:9245"};

$(function () {
	$.ajax({
		url: SELECTA_LADDERS_URLS + "?" + document.cookie,
		success: setBestEquityLadder,
		error: function (error) {
			console.log("Error getting best ladders for user");
			console.log(error);
		}
	});
});

function setBestEquityLadder(newUrls) {

	let newUrlsObject;
	if (typeof newUrls === "string") {
		newUrlsObject = JSON.parse(newUrls);
	} else {
		newUrlsObject = newUrls;
	}

	if (newUrlsObject && newUrlsObject.hasOwnProperty(LADDER_HOST_PROP) && newUrlsObject.hasOwnProperty(WORKSPACE_HOST_PROP)) {
		EQUITY_LADDER_URLS = newUrlsObject;
	} else {
		console.log("The new urls were malformed: " + newUrls + " parsed to " + newUrlsObject);
	}
}

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

function getLadderUrl(symbol) {
	return getLadderHosts(symbol).ladderHost + "/ladder#" + symbol;
}

function launchLadder(symbol, skipBasket, ternaryIsFutures) {

	symbol = symbol.toUpperCase();
	const hosts = getLadderHosts(symbol, ternaryIsFutures);
	$.ajax({
		success: function (d, s, x) {
			if ("success" !== d) {
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
	if (!skipBasket) {
		launchBasket(symbol, true);
	}
}

function getLadderHosts(symbol, ternaryIsFutures) {

	const isDev = DEV_URLS.has(window.location.hostname);

	if (isDev) {

		return {ladderHost: window.location.hostname + ":9044", workspaceHost: window.location.hostname + ":9045"};

	} else if (false === ternaryIsFutures) {

		return EQUITY_LADDER_URLS;

	} else if (true === ternaryIsFutures || symbol.match(/^[^:]*[FGHJKMNQUVXZ][0-9](;.*)?$/) || symbol.match(/ FWD$/)) {

		return FUTURES_LADDER_URLS;

	} else if (symbol.match(/ RFQ$/)) {

		return RFQ_LADDER_URLS;

	} else {

		return EQUITY_LADDER_URLS;
	}
}

function launchBasket(symbol, noPopUp) {
	if (symbol.indexOf("-") != -1) {
		symbol = symbol.split("-")[0];
	}
	if(symbol.indexOf(" RFQ") != -1) {
		const rootTicker = symbol.split(" RFQ")[0];
		let length = rootTicker.length;
		symbol = rootTicker.substr(0, length - 2) + " " + rootTicker.substr(length - 2);
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

		return symbolTemplate.split("{{EXCHANGE}}").join(exchange).split("{{SYMBOL}}").join(symbol);
	}).join(",");

	const link = linkTemplate.split("{{SYMBOLS}}").join(symbolList).split("{{DATE}}").join(d.toISOString().split("T")[0]);

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
