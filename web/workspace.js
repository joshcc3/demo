var ws;

var ladderWidth = 245;
var ladderHeight = 332;

ws = connect("ws://" + document.location.host + "/workspace/ws/");
ws.logToConsole = false;
ws.onmessage = function (x) {
	eval(x);
};

var LastRows = 0;
var LastCols = 0;
var NumBoxes = LastRows * LastCols;

var Frames = [];
var Ladders = {};
var Locked = false;
var Sets = false;

$(function () {

	var parameters = parseHashcode();

	if (parameters.locking) {
		toggleLock();
	}

	if (parameters.sets) {
		toggleSets();
	}

	setInterval(function () {

		// Compute number of rows and columns
		var height = document.body.offsetHeight;
		var width = $(window).width();

		var rows = parseInt(height / ladderHeight);
		var cols = parseInt(width / ladderWidth);

		if (rows != LastRows || cols != LastCols) {
			LastRows = rows;
			LastCols = cols;
			NumBoxes = LastRows * LastCols;

			doLayout();
			doLadders();
			return;
		}

	}, 250);

	$("#lock").click(function () {
		toggleLock();
	});

	$("#sets").click(function () {
		toggleSets();
	});

	window.onhashchange = function () {
		doLadders();
	}

});

function sendReplace(from, to) {
	ws.send(command("replace", [from, to]));
}

function replace(from, to) {
	document.location.hash = document.location.hash.split(from).join(to);
}

function refreshHashCode() {
	var params = parseHashcode();
	document.location.hash = generateHashcode(params.symbols);
}

function doLayout() {

	var template = $('.ladder.template').clone().removeClass('template');
	template.width(ladderWidth);
	template.height(ladderHeight);

	var existing = $('.ladder:not(.template)');
	var numExisting = existing.size();

	if (numExisting > NumBoxes) {
		var spliced = Frames.splice(NumBoxes);
		spliced.forEach(function (frame) {
			frame.remove();
		});
	}

	for (var i = numExisting; i < NumBoxes; i++) {
		var frame = template.clone();
		$('#workspace').append(frame);
		Frames.push(frame);
	}

}

function doLadders() {
	var parameters = parseHashcode();
	var symbols = parameters.symbols;
	for (var i = 0; i < Math.min(symbols.length, Frames.length); i++) {

		var frameTarget = symbols[i];

		var symbolEnd = frameTarget.indexOf(';', 0);
		var isStack;
		if (symbolEnd < 0) {
			symbolEnd = frameTarget.length;
			isStack = false;
		} else {
			var symbolSwitch = frameTarget.substr(symbolEnd + 1);
			isStack = symbolSwitch == 'S'
		}

		var symbol = frameTarget.substr(0, symbolEnd);

		setFrame(Frames[i], symbol, isStack);
	}
}

function moveFrameToFront(index) {
	if (index == 0) {
		return;
	}
	if (index < Frames.length) {
		var el = Frames[index];
		Frames.splice(index, 1);
		Frames.unshift(el);
		el.detach();
		$("#workspace").prepend(el);
	}
}

function addSymbol(symbol) {
	var parameters = parseHashcode();
	var symbols = parameters.symbols;
	if (symbols.indexOf(symbol) != -1) {
		// Move the symbol to the front and move its frame
		var index = symbols.indexOf(symbol);
		moveFrameToFront(index, Frames);
		symbols.splice(index, 1);
		symbols.unshift(symbol);
	} else {
		// Add the symbol to the front
		symbols.unshift(symbol);
		// Roll the last frame around to the front
		moveFrameToFront(Frames.length - 1, Frames);
	}
	document.location.hash = generateHashcode(symbols);
	doLadders();
}

function toggleLock() {
	Locked = !Locked;
	ws.send(command(Locked ? "lock" : "unlock", []));
	$("#lock").toggleClass("enabled", Locked);
	refreshHashCode();
}

function toggleSets() {
	Sets = !Sets;
	ws.send(command(Sets ? "setify" : "unsetify", []));
	$("#sets").toggleClass("enabled", Sets);
	refreshHashCode();
}

// -- utilities

function parseHashcode() {
	var hash = document.location.hash.substr(1);
	var symbols, locking, sets;
	locking = hash.indexOf("!") != -1;
	sets = hash.indexOf("*") != -1;
	symbols = hash.split("!").join("").split("*").join("").split(",");
	if (symbols.length == 1 && symbols[0] == "") {
		symbols = [];
	}
	return {symbols: symbols, locking: locking, sets: sets};
}

function generateHashcode(symbols) {
	return symbols.join(",")
		+ (Locked ? "!" : "")
		+ (Sets ? "*" : "");
}

function getUrl(symbol) {
	var port = parseInt(document.location.port);
	var host = document.location.hostname;
	return 'http://' + host + ':' + (port - 1) + '/ladder#' + symbol;
}

function setFrame(ladder, symbol, isStack) {

	var ladderDiv = $(ladder);

	var idSymbol;
	var urlSymbol;
	if (isStack) {
		idSymbol = symbol + "-S";
		urlSymbol = symbol + ";S";
	} else {
		idSymbol = symbol;
		urlSymbol = symbol;
	}

	if (ladderDiv.attr("id") != symbolId(idSymbol)) {
		ladderDiv.attr("id", symbolId(idSymbol));
		ladderDiv.find(".frame").attr("src", getUrl(urlSymbol));
	}
}

function symbolId(symbol) {
	return symbol.replace(/\W/g, '_');
}