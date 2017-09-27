var ws;

var filters = [];

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (m) {
		eval(m);
	};

	ws.send("subscribe");

	var creationRow = $("#creationRow");
	creationRow.find("input").each(setupSymbolInput);

	var submit = creationRow.find(".button button");
	submit.off("click").click(function () {

		var quoteSymbol = creationRow.find("input[name=quote]").val();
		var forNibbler = creationRow.find("#hostNibblers").find("option:selected").text();
		var leanInstType = creationRow.find("#leanInstID").find("option:selected").text();
		var leanSymbol = creationRow.find("input[name=lean]").val();
		ws.send(command("submitSymbol", [forNibbler, quoteSymbol, leanInstType, leanSymbol]));
	});

	$("body").unbind("dblclick").bind("dblclick", showAdmin);

	var instTypeFilterList = $("#instTypesFilter");
	instTypeFilterList.val("ALL");
	instTypeFilterList.change(updateFilters);
	updateFilters();
});

function updateFilters() {


	filters = [];

	var instTypeFilterList = $("#instTypesFilter");
	instTypeFilterList.find(":selected").each(function (i, selected) {
		filters[i] = new RegExp($(selected).attr("data"));
		refreshAllFilteredRows();
	});
}

function refreshAllFilteredRows() {

	var allRows = $("#exchanges").find(".row:not(.header)");

	allRows.each(function (i, row) {
		row = $(row);
		row.toggleClass("hidden", isFiltered(row));
	});
}

function isFiltered(row) {

	var source = row.find(".instType").text();

	var filtered = true;
	filters.forEach(function (filter) {
		if (source.match(filter)) {
			filtered = false;
		}
	});
	return filtered;
}

function compareOptionRow(a, b) {

	var aName = a.text();
	var bName = b.text();

	return aName < bName ? -1 : aName == bName ? 0 : 1;
}

function setupSymbolInput(i, input) {

	input = $(input);
	var type = input.attr("name");

	input.on("input", function () {
		var symbol = input.val();
		ws.send(command("checkInst", [type, symbol]));
	});
}

function addInstType(instTypes) {

	var instTypeCombo = $("#leanInstID");
	$(instTypeCombo).find("option").remove();

	var instTypeFilterList = $("#instTypesFilter");
	var allFilter = instTypeFilterList.find("option[value=\"ALL\"]");
	var unwantedFilters = instTypeFilterList.find("option");
	unwantedFilters.remove();
	instTypeFilterList.append(allFilter);

	console.log(instTypeFilterList);

	instTypes.forEach(function (instType) {

		var option = $("<option value=\"" + instType + "\">" + instType + "</option>");
		option.addClass(instType);
		option.attr("data", instType);
		instTypeCombo.append(option);

		var filterOption = $("<option value=\"" + instType + "\">" + instType + "</option>");
		filterOption.addClass(instType);
		filterOption.attr("data", instType);
		addSortedDiv(instTypeFilterList.find("option"), filterOption, compareOptionRow);
	});

	instTypeFilterList.val("ALL");
	updateFilters();
}

function addAvailableNibblers(nibblers) {

	var nibblersCombo = $("#hostNibblers");
	$(nibblersCombo).find("option").remove();
	nibblers.forEach(function (nibbler) {

		var option = $("<option value=\"" + nibbler + "\">" + nibbler + "</option>");
		option.addClass(nibbler);
		option.attr("data", nibbler);
		nibblersCombo.append(option);
	});
}

function noInstID(type) {

	var infoRow = $("#" + type + "Info");
	infoRow.toggleClass("unknown", true);
	infoRow.find("div").text("");
}

function setInstID(type, isin, ccy, mic) {

	var infoRow = $("#" + type + "Info");
	infoRow.toggleClass("unknown", false);
	infoRow.find(".isin").text(isin);
	infoRow.find(".ccy").text(ccy);
	infoRow.find(".mic").text(mic);
}

function removeAll(nibblerName) {
	var table = getExchangeTable(nibblerName);
	table.find(".dataRow").remove();
	setOrderCount(nibblerName);
}

function setRow(nibblerName, strategyID, quoteSymbol, quoteISIN, quoteCCY, quoteMIC, leanInstType, leanSymbol, leanISIN, leanCCY, leanMIC,
				isQuoteInstDefEventAvailable, isQuoteBookAvailable, isLeanBookAvailable, isFXAvailable, isAdditiveAvailable,
				selectedConfigType) {

	var rowID = nibblerName + strategyID;
	var row = $("#" + rowID);
	if (row.length < 1) {

		var exchangeTable = getExchangeTable(nibblerName);
		row = exchangeTable.find(".header").clone();
		row.removeClass("header");
		row.attr("id", rowID);
		row.addClass("dataRow");
		exchangeTable.append(row);

		row.find("div:not(.killStrategy)").each(function (i, d) {
			d = $(d);
			d.text("");
		});

		var killSymbol = row.find(".killStrategy button");
		killSymbol.text("kill");
		killSymbol.off("click").click(function () {
			ws.send(command("killSymbol", [nibblerName, quoteSymbol]));
		});
	}

	var quoteSymbolCell = setCellData(row, ".quote.symbol", quoteSymbol);
	quoteSymbolCell.unbind().bind("click", function () {
		launchLadder(quoteSymbol);
	});
	setCellData(row, ".quote.isin", quoteISIN);
	setCellData(row, ".quote.ccy", quoteCCY);
	setCellData(row, ".quote.mic", quoteMIC);

	var leanSymbolCell = setCellData(row, ".lean.symbol", leanSymbol);
	leanSymbolCell.unbind().bind("click", function () {
		launchLadder(leanSymbol);
	});
	setCellData(row, ".lean.instType", leanInstType);
	setCellData(row, ".lean.isin", leanISIN);
	setCellData(row, ".lean.ccy", leanCCY);
	setCellData(row, ".lean.mic", leanMIC);

	setBoolData(row, ".isQuoteAvailable", isQuoteInstDefEventAvailable);
	setBoolData(row, ".isQuoteBookAvailable", isQuoteBookAvailable);
	setBoolData(row, ".isLeanBookAvailable", isLeanBookAvailable);
	setBoolData(row, ".isFXAvailable", isFXAvailable);
	setBoolData(row, ".isAdditiveAvailable", isAdditiveAvailable);

	setCellData(row, ".selectedConfigType", selectedConfigType);

	setOrderCount(nibblerName);

	row.toggleClass("hidden", isFiltered(row));
}

function setOrderCount(nibblerName) {

	var nibbler = $("#" + nibblerName);
	var orderCountDiv = nibbler.find(".orderCount");
	var orders = nibbler.find(".exchange .row").length - 1;
	orderCountDiv.text(orders);
}

function getExchangeTable(nibblerName) {

	var nibbler = $("#" + nibblerName);
	if (nibbler.length < 1) {

		nibbler = $("#templateNibbler").clone();
		nibbler.attr("id", nibblerName);
		nibbler.removeClass("template");

		nibbler.find(".nibblerName").text(nibblerName);

		var exchangeBlock = nibbler.find(".exchange");
		nibbler.find(".serverDetails").unbind().bind("click", function () {
			exchangeBlock.toggleClass("hidden", !exchangeBlock.hasClass("hidden"))
		});

		$("#exchanges").append(nibbler);

		var headerRow = nibbler.find(".header.row");
		var killSymbol = headerRow.find(".killStrategy button");
		killSymbol.text("kill inactive");
		killSymbol.off("click").click(function () {
			ws.send(command("killInactiveSymbols", [nibblerName]));
		});
	}
	return nibbler.find(".exchange");
}

function setCellData(row, cellID, value) {

	var input = row.find(cellID);
	if (typeof value != "undefined") {
		input.attr("data", value);
		input.text(value);
	} else {
		input.attr("data", "");
		input.text("");
	}
	return input
}

function setBoolData(row, cellID, value) {

	var input = row.find(cellID);
	input.toggleClass("isTrue", value);
}

function showAdmin(event) {
	var adminDiv = $("#adminBlock");
	if (event.ctrlKey) {
		adminDiv.toggleClass("hideAdmin", !adminDiv.hasClass("hideAdmin"));
	} else {
		adminDiv.toggleClass("hideAdmin", true);
	}
	window.getSelection().removeAllRanges();
}
