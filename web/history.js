var ws;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (m) {
		eval(m);
	};
});

function addSymbol(symbol) {

	var row = removeSymbol(symbol);
	if (row.length < 1) {
		var rowID = getRowID(symbol);
		row = $("#templateRow").clone();
		row.attr("id", rowID);
		row.removeClass("hidden");
		row.text(symbol);
	}

	var table = $("#symbolTable");
	table.prepend(row);

	row.unbind().bind("click", function () {
		launchLadder(symbol);
	});
}

function removeSymbol(symbol) {

	var rowID = getRowID(symbol);
	var row = $('#' + rowID);
	row.remove();
	return row;
}

function getRowID(symbol) {
	return symbol.replace(/ |\/|\.|:|;/g, "_");
}
