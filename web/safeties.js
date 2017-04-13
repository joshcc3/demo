var ws;

var dogBarkSound;

$(function () {
	ws = connect();
	ws.onmessage = function (data) {
		eval(data);
	};

	ws.send("subscribe");

	dogBarkSound = new Audio("sounds/quack.wav");
	checkWarning();
	setInterval(checkWarning, 3000);
});

function checkWarning() {

	var count = $(".isWarning, .isError").length;
	if (0 < count) {

		if (!dogBarkSound.readyState) {
			dogBarkSound.load();
		}
		dogBarkSound.play();
	}
}

function setNibblerConnected(source, isConnected) {

	var table = $('#' + source);
	if (table.length < 1) {

		table = $("#nibblerTemplate").clone();
		table.attr("id", source);
		table.removeClass("hidden");

		table.find(".nibblerName").text(source);

		var nibblers = $("#nibblers");
		addSortedDiv(nibblers.find(".nibblerBlock"), table, compareNibblerRow);
	}

	table.find(".nibblerName").toggleClass("connectionDown", !isConnected);
}

function compareNibblerRow(a, b) {

	var aName = a.find(".nibblerName").text();
	var bName = b.find(".nibblerName").text();

	return aName < bName ? -1 : aName == bName ? 0 : 1;
}

function setRow(id, source, safetyName, limit, warning, current, lastSymbol, isWarning, isError) {

	var row = $("#" + id);

	if (row.length < 1) {

		var table = $('#' + source);

		var header = table.find(".headerRow");
		row = header.clone();

		row.attr("id", id);
		row.removeClass("headerRow");

		addSortedDiv(table.find(".row"), row, compareBlotterRow);
	}

	row.toggleClass("isWarning", isWarning);
	row.toggleClass("isError", isError);

	row.find(".safetyName").text(safetyName);
	row.find(".limit").text(limit);
	row.find(".warning").text(warning);
	row.find(".current").text(current);

	if (isWarning || isError) {
		row.find(".lastSymbol").text(lastSymbol);
	} else {
		row.find(".lastSymbol").text("");
	}
}

function removeRow(id) {

	var row = $("#" + id);
	row.remove();
}

function compareBlotterRow(a, b) {

	var aTime = a.find(".time").text();
	var bTime = b.find(".time").text();

	return aTime < bTime ? 1 : aTime == bTime ? 0 : -1;
}

function removeAllRows(source) {

	var table = $('#' + source);
	var rows = table.find(".row:not(.headerRow)");
	rows.remove();
}
