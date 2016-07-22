var headerRow;
var table;

$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (x) {
		eval(x)
	};

	table = $("#stockAlerts");
	headerRow = $("#header");
});

function stockAlert(timestamp, type, symbol) {

	var id = (type + symbol).replace(/ |\/|\.|:/g, "_");
	var row = $('#' + id);

	if (row[0]) {
		row.remove();
	} else {
		row = table.find(".template").clone().removeClass("template");
		row.attr("id", id);
	}

	row.addClass(type);

	row.find(".timestamp").text(timestamp);

	var symbolCell = row.find(".symbol");
	symbolCell.text(symbol);
	symbolCell.die().bind("click", function () {
		launchLadder(symbol);
	});

	row.find(".msg").text(type);
	row.insertAfter(headerRow);
}

