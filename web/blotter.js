let ws;

let filters = [];

$(function () {
	ws = connect();
	ws.onmessage = function (data) {
		eval(data);
	};

	ws.send("subscribe");

	const nibblerFilterList = $("#nibblerList");
	nibblerFilterList.val("ALL");
	nibblerFilterList.change(updateFilters);
	updateFilters();

	const blotterView = $("#blotter");
	const isHiddenCheckBox = $("#isLowPriorityHidden");
	isHiddenCheckBox.unbind('click').bind('click', function () {
		blotterView.toggleClass("hideLowPriority", isHiddenCheckBox.prop('checked'));
	});
});

function updateFilters() {

	filters = [];

	const nibblerFilterList = $("#nibblerList");
	nibblerFilterList.find(":selected").each(function (i, selected) {
		filters[i] = new RegExp($(selected).attr("data"));
		refreshAllFilteredRows();
	});
}

function refreshAllFilteredRows() {

	const allRows = $("#blotter").find(".row:not(.headerRow)");

	allRows.each(function (i, row) {
		row = $(row);
		row.toggleClass("hidden", isFiltered(row));
	});
}

function isFiltered(row) {

	const source = row.find(".source").text();

	let filtered = true;
	filters.forEach(function (filter) {
		if (source.match(filter)) {
			filtered = false;
		}
	});
	return filtered;
}

function setNibblerConnected(nibblerName, isConnected) {

	let row = $("#" + nibblerName);
	if (row.length < 1) {
		const table = $("#nibblers");
		const header = table.find(".headerRow");
		row = header.clone();

		row.attr("id", nibblerName);
		row.removeClass("headerRow");

		row.find(".nibblerName").text(nibblerName);

		addSortedDiv(table.find(".row"), row, compareNibblerRow);

		const option = $("<option value=\"" + nibblerName + "\">" + nibblerName + "</option>");
		option.addClass(nibblerName);
		option.attr("data", nibblerName);

		addSortedDiv($("#nibblerList").find("option"), option, compareOptionRow);
	}

	row.toggleClass("connectionDown", !isConnected);
}

function compareNibblerRow(a, b) {

	const aName = a.find(".nibblerName").text();
	const bName = b.find(".nibblerName").text();

	return aName < bName ? -1 : aName == bName ? 0 : 1;
}

function compareOptionRow(a, b) {

	const aName = a.text();
	const bName = b.text();

	return aName < bName ? -1 : aName == bName ? 0 : 1;
}

function addRow(id, timestamp, source, text, isLowPriority) {

	let row = $("#" + id);

	if (row.length < 1) {
		const table = $("#blotter");
		const header = table.find(".headerRow");
		row = header.clone();

		row.attr("id", id);
		row.removeClass("headerRow");

		row.find(".time").text(timestamp);
		row.find(".source").text(source);
		row.find(".text").text(text);
		row.toggleClass("isLowPriority", isLowPriority);

		addSortedDiv(table.find(".row"), row, compareBlotterRow);

		row.toggleClass("hidden", isFiltered(row));
	}
}

function removeRow(id) {

	const row = $("#" + id);
	row.remove();
}

function compareBlotterRow(a, b) {

	const aTime = a.find(".time").text();
	const bTime = b.find(".time").text();

	return aTime < bTime ? 1 : aTime == bTime ? 0 : -1;
}
