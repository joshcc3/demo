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
	twapSound = new Audio("stockAlerts/TWAP.wav");
	unknownSound = new Audio("stockAlerts/huh-humm.wav");
});

function stockAlert(timestamp, type, symbol, msg, isOriginal) {

	var id = (type + symbol).replace(/ |\/|\.|:/g, "_");
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
	symbolCell.die().bind("click", function () {
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
	if ("RFQ" == type) {
		rfqSound.play();
	} else if ("AT_CLOSE" == type) {
		atCloseSound.play();
	} else if ("SWEEP" == type) {
		sweepSound.play();
	} else if ("TWAP" == type) {
		twapSound.play();
	} else {
		unknownSound.play();
	}
}
