var headerRow;
var table;

var rfqSound;
var atCloseSound;
var sweepSound;
var twapSound;
var unknownSound;

$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (x) {
		eval(x)
	};

	table = $("#stockAlerts");
	headerRow = $("#header");

	rfqSound = new Audio("stockAlerts/eastsideRFQ.wav");
	atCloseSound = new Audio("stockAlerts/calf-slap.wav");
	sweepSound = new Audio("stockAlerts/sword-schwing.wav");
	twapSound = new Audio("stockAlerts/highNoon.mp3");
	unknownSound = new Audio("stockAlerts/huh-humm.wav");
});

function stockAlert(timestamp, type, symbol, msg, isOriginal) {

	var id = (type + symbol + timestamp).replace(/ |\/|\.|:/g, "_");
	var row = $('#' + id);

	if (row[0]) {
		row.remove();
	} else {
		row = table.find("#header").clone();
		row.attr("id", id);
	}

	row.addClass(type);

	row.find(".timestamp").text(timestamp);
	row.find(".type").text(type);

	var symbolCell = row.find(".symbol");
	symbolCell.text(symbol);
	symbolCell.unbind().bind("click", function () {
		launchLadder(symbol);
	});

	row.find(".msg").text(msg);
	row.insertAfter(headerRow);

	var rows = table.children();
	if (16 < rows.length) {
		table.children().last().remove();
	}

	if (isOriginal) {
		playSound(type);
	}
}

function playSound(type) {

	var sound;
	if ("RFQ" == type) {
		sound = rfqSound;
	} else if ("AT_CLOSE" == type) {
		sound = atCloseSound;
	} else if ("SWEEP" == type) {
		sound = sweepSound;
	} else if ("TWAP" == type) {
		sound = twapSound;
	} else {
		sound = unknownSound;
	}
	if (!sound.readyState) {
		sound.load();
	}
	sound.play();
}
