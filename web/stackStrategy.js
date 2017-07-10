var ws;

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
});

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
	instTypes.forEach(function (instType) {

		var option = $("<option value=\"" + instType + "\">" + instType + "</option>");
		option.addClass(instType);
		option.attr("data", instType);
		instTypeCombo.append(option);
	});
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

		row.find("div").each(function (i, d) {

			d = $(d);
			d.text("");
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
