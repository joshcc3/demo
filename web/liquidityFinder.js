let ws;

const bidDistances = new Map();
const askDistances = new Map();

let bidTable;
let askTable;
let templateRow;

let queued;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (msg) {
		eval(msg);
	};

	bidTable = $("#bidDistances");
	askTable = $("#askDistances");
	templateRow = $("#templateRow");
});

function remove(side, symbol) {

	const rowMap = getRowMap(side);
	const row = rowMap.get(symbol);

	if (row) {
		rowMap.delete(symbol);
		row.remove();
	}
}

function set(side, symbol, displaySymbol, bpsAway) {

	const rowMap = getRowMap(side);
	let row = rowMap.get(symbol);
	if (row) {

		row.find(".bpsAway").text(bpsAway);
	} else {

		row = templateRow.clone().removeClass("hidden");
		const key = toId(symbol);
		row.attr("id", key);

		row.find(".symbol").text(displaySymbol);

		row.click(() => {
			launchLadder(symbol);
			if (symbol.endsWith(" RFQ")) {
				let origSymbol = symbol.split(" ")[0];
				let origSuffix = origSymbol.slice(origSymbol.length - 2, origSymbol.length);
				let origPrefix = origSymbol.slice(0, origSymbol.length - 2);
				launchLadder(origPrefix + " " + origSuffix);
			}
		});
		row.find(".bpsAway").text(bpsAway);

		const table = getTable(side);

		rowMap.set(symbol, row);
		table.append(row);
	}

	row.data("bps", parseFloat(bpsAway));
	queueSort();
}

function getTable(side) {

	if ("BID" === side) {
		return bidTable;
	} else {
		return askTable;
	}
}

function getRowMap(side) {

	if ("BID" === side) {
		return bidDistances;
	} else {
		return askDistances;
	}
}

function toId(symbol) {
	return symbol.replace(/\W/g, "");
}

function queueSort() {
	if (!queued) {
		queued = setTimeout(sortPicards, 300);
	}
}

function sortPicards() {

	sortTable(bidTable);
	sortTable(askTable);
	queued = null;
}

function sortTable(table) {

	let sorted = table.find(".row");
	sorted.sort(function (a, b) {
		return $(a).data("bps") - $(b).data("bps");
	});
	sorted.detach();
	table.append(sorted);
}
