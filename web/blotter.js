var ws;

var filters = [];

$(function () {
	ws = connect();
	ws.onmessage = function (data) {
		eval(data);
	};

	ws.send("subscribe");

	var nibblerFilterList = $("#nibblerList");
	nibblerFilterList.val("ALL");
	nibblerFilterList.change(updateFilters);
	updateFilters();
});

function updateFilters() {

	filters = [];

	var nibblerFilterList = $("#nibblerList");
	nibblerFilterList.find(":selected").each(function (i, selected) {
		filters[i] = new RegExp($(selected).attr("data"));
		refreshAllFilteredRows();
	});
}

function refreshAllFilteredRows() {

	var allRows = $("#blotter").find(".row:not(.headerRow)");

	allRows.each(function (i, row) {
		row = $(row);
		row.toggleClass("hidden", isFiltered(row));
	});
}

function isFiltered(row) {

	var source = row.find(".source").text();

	var filtered = true;
	filters.forEach(function (filter) {
		if (source.match(filter)) {
			filtered = false;
		}
	});
	return filtered;
}

function setNibblerConnected(nibblerName, isConnected) {

	var row = $("#" + nibblerName);
	if (row.length < 1) {
		var table = $("#nibblers");
		var header = table.find(".headerRow");
		row = header.clone();

		row.attr("id", nibblerName);
		row.removeClass("headerRow");

		row.find(".nibblerName").text(nibblerName);

		addSortedDiv(table.find(".row"), row, compareNibblerRow);

		var option = $("<option value=\"" + nibblerName + "\">" + nibblerName + "</option>");
		option.addClass(nibblerName);
		option.attr("data", nibblerName);

		addSortedDiv($("#nibblerList").find("option"), option, compareOptionRow);
	}

	row.toggleClass("connectionDown", !isConnected);
}

function compareNibblerRow(a, b) {

	var aName = a.find(".nibblerName").text();
	var bName = b.find(".nibblerName").text();

	return aName < bName ? -1 : aName == bName ? 0 : 1;
}

function compareOptionRow(a, b) {

	var aName = a.text();
	var bName = b.text();

	return aName < bName ? -1 : aName == bName ? 0 : 1;
}

function addRow(id, timestamp, source, text) {

	var row = $("#" + id);

	if (row.length < 1) {
		var table = $("#blotter");
		var header = table.find(".headerRow");
		row = header.clone();

		row.attr("id", id);
		row.removeClass("headerRow");

		row.find(".time").text(timestamp);
		row.find(".source").text(source);
		row.find(".text").text(text);

		addSortedDiv(table.find(".row"), row, compareBlotterRow)

		row.toggleClass("hidden", isFiltered(row));
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
