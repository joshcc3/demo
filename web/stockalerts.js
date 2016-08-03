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

function stockAlert(timestamp, type, symbol, msg) {

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
}

