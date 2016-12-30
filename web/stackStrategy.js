var ws;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (m) {
		eval(m);
	};

	var nibblerFilter = document.location.hash;
	var nibbler = nibblerFilter.substr(1, nibblerFilter.length);
	ws.send("subscribe-nibbler," + nibbler);

	var creationRow = $("#creationRow");
	creationRow.find("input").each(setupSymbolInput);

	var submit = creationRow.find(".button button");
	submit.off("click").click(function () {

		var quoteSymbol = creationRow.find("input[name=quote]").val();
		var leanInstType = creationRow.find("#leanInstID").find("option:selected").text();
		var leanSymbol = creationRow.find("input[name=lean]").val();
		ws.send(command("submitSymbol", [quoteSymbol, leanInstType, leanSymbol]));
	});
});

function setupSymbolInput(i, input) {

	input = $(input);
	var type = input.attr("name");

	input.on("input", function () {
		var symbol = input.val();
		ws.send(command("checkInst", [type, symbol]));
	});
}

function noInstID(type) {

	var infoRow = $("#" + type + "Info");
	infoRow.toggleClass("unknown", true);
	infoRow.find("div").text("");
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

function setInstID(type, isin, ccy, mic) {

	var infoRow = $("#" + type + "Info");
	infoRow.toggleClass("unknown", false);
	infoRow.find(".isin").text(isin);
	infoRow.find(".ccy").text(ccy);
	infoRow.find(".mic").text(mic);
}

function removeAll() {
	$(".dataRow").remove();
}

function setRow(id, quoteSymbol, quoteISIN, quoteCCY, quoteMIC, leanSymbol, leanISIN, leanCCY, leanMIC, isQuoteInstDefEventAvailable,
				isQuoteBookAvailable, isLeanBookAvailable, isFXAvailable, selectedConfigType) {

	var row = $("#" + id);
	if (row.length < 1) {
		row = $("#header").clone();
		row.attr("id", id);
		row.addClass("dataRow");
		$("#createdTable").append(row);

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
	setCellData(row, ".lean.isin", leanISIN);
	setCellData(row, ".lean.ccy", leanCCY);
	setCellData(row, ".lean.mic", leanMIC);

	setBoolData(row, ".isQuoteAvailable", isQuoteInstDefEventAvailable);
	setBoolData(row, ".isQuoteBookAvailable", isQuoteBookAvailable);
	setBoolData(row, ".isLeanBookAvailable", isLeanBookAvailable);
	setBoolData(row, ".isFXAvailable", isFXAvailable);

	setCellData(row, ".selectedConfigType", selectedConfigType);
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
