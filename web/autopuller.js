let ws;
const Rows = {};
let WorkingPx = {};
let MdPx = {};
let Symbols = [];
let Sound;

$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (x) {
		eval(x)
	};
	setupInputBehavior();
	Sound = new Audio("sounds/firealarm.wav");
	Sound.load();
});

function setupInputBehavior() {
	const inputRow = $("#rules").find("tr.ruleInput");
	const symbolSelect = inputRow.find(".symbolSelect");
	symbolSelect.unbind('change').bind('change', function () {
		const symbol = symbolSelect.val();
		populateRowPrices(inputRow, symbol);
	});
	inputRow.find("button.update").unbind('click').bind('click', function () {
		sendWriteRule(inputRow);
	});
	inputRow.find(".start").toggleClass("hidden", false).unbind('click').bind('click', sendStartAll);
	inputRow.find(".stop").toggleClass("hidden", false).unbind('click').bind('click', sendStopAll);
}

function populateSelect(select, values, defaultVal) {

	if (defaultVal && values.indexOf(defaultVal) === -1) {
		values.push(defaultVal);
	}

	select.find("option").remove();
	values.forEach(function (s) {
		select.append($("<option value='" + s + "'>" + s + "</option>"));
	});
	if (select.data('value')) {
		select.val(select.data('value'));
	} else if (defaultVal) {
		select.val(defaultVal);
	}
}

function populateRowPrices(inputRow, symbol) {

	const fromPrice = inputRow.find(".fromPrice");
	const toPrice = inputRow.find(".toPrice");
	if (WorkingPx[symbol]) {
		populateSelect(fromPrice, WorkingPx[symbol], WorkingPx[symbol][WorkingPx[symbol].length - 1]);
		populateSelect(toPrice, WorkingPx[symbol], WorkingPx[symbol][0]);
	} else {
		populateSelect(fromPrice, []);
		populateSelect(toPrice, []);
	}

	const priceCondition = inputRow.find(".priceCondition");
	if (MdPx[symbol]) {

		populateSelect(priceCondition, MdPx[symbol], MdPx[symbol][MdPx[symbol].length / 2]);
	} else {
		populateSelect(priceCondition, []);

	}
}

function populateRuleRow(row, side, symbol, orderPriceFrom, orderPriceTo, conditionPrice, conditionSide, qtyCondition, qtyThreshold) {
	row.find(".side").val(side);
	populateSelect(row.find(".fromPrice"), WorkingPx[symbol], orderPriceFrom);
	populateSelect(row.find(".toPrice"), WorkingPx[symbol], orderPriceTo);
	populateSelect(row.find(".priceCondition"), MdPx[symbol], conditionPrice);
	row.find(".conditionSide").val(conditionSide);
	row.find(".qtyCondition").val(qtyCondition);
	row.find(".qtyThreshold").val(qtyThreshold);
}

function playSound() {
	Sound.play();
}

// Commands from server

//noinspection JSUnusedLocalSymbols
function updateGlobals(symbols, symbolToWorkingPrice, symbolToMdPrice) {
	Symbols = symbols;
	WorkingPx = symbolToWorkingPrice;
	MdPx = symbolToMdPrice;

	const currSymbol = Symbols[0];
	const inputRow = $("#rules").find("tr.ruleInput");
	const symbolSelect = inputRow.find(".symbolSelect");
	populateSelect(symbolSelect, Symbols, currSymbol);
	populateRowPrices(inputRow, currSymbol);
}

//noinspection JSUnusedLocalSymbols
function ruleFired(key) {

	playSound();

	let row = Rows[key];
	if (row) {
		row.toggleClass("fired", true);
	}
}

//noinspection JSUnusedLocalSymbols
function displayRule(key, symbol, side, orderPriceFrom, orderPriceTo, conditionPrice, conditionSide, qtyCondition, qtyThreshold,
					 enabled, enabledByUser, instantPullCount) {

	let row = Rows[key];
	if (!row) {

		const rules = $("#rules");

		row = rules.find("tr.ruleInput").clone().removeClass("ruleInput");
		row.attr('id', key);
		row.data('ruleID', key);

		rules.append(row);

		Rows[key] = row;

		const symbolSelect = row.find(".symbolSelect");
		populateSelect(symbolSelect, [symbol], symbol);
		symbolSelect.attr('disabled', 'disabled');

		symbolSelect.toggleClass("hidden", true);
		row.find(".symbolDisplay").text(symbol).toggleClass("hidden", false);

		row.find("button.delete").toggleClass("hidden", false);
		row.find("button.copy").toggleClass("hidden", false);
		row.find("button.start").toggleClass("hidden", false);
		row.find("button.stop").toggleClass("hidden", false);

		row.find("button.update").unbind('click').bind('click', function () {
			sendWriteRule(row);
			row.toggleClass("fired", false);
		});

		row.find("button.delete").unbind('click').bind('click', function () {
			sendDeleteRule(row);
		});

		row.find("button.copy").unbind('click').bind('click', function () {
			const inputRow = rules.find("tr.ruleInput");
			const symbolSelect = inputRow.find(".symbolSelect");
			populateSelect(symbolSelect, Symbols, symbol);
			populateRuleRow(inputRow, side, symbol, orderPriceFrom, orderPriceTo, conditionPrice, conditionSide, qtyCondition, qtyThreshold);
		});

		row.find("button.start").unbind('click').bind('click', function () {
			sendStartRule(row);
			row.toggleClass("fired", false);
		});
		row.find("button.stop").unbind('click').bind('click', function () {
			sendStopRule(row);
			row.toggleClass("fired", false);
		});
	}

	populateRuleRow(row, side, symbol, orderPriceFrom, orderPriceTo, conditionPrice, conditionSide, qtyCondition, qtyThreshold);
	row.toggleClass("enabled", enabled);
	row.find(".enabledByUser").text(enabledByUser);

	if (0 < instantPullCount) {
		row.find(".instantPull").text("Instantly pull " + instantPullCount + " orders!");
		row.toggleClass("alert", true);
	} else {
		row.find(".instantPull").text("");
		row.toggleClass("alert", false);
	}
}

//noinspection JSUnusedLocalSymbols
function showMessage(message) {
	const div = $("#message");
	if (message != div.text()) {
		playSound();
	}
	div.text(message).toggleClass("hidden", false);
}

function removeRule(key) {
	const row = Rows[key];
	row.remove();
	delete Rows[key];
}

// Send commands to server

function sendWriteRule(inputRow) {
	let ruleID = inputRow.data('ruleID');
	if (!ruleID) {
		ruleID = "NEW";
	}
	const symbol = inputRow.find(".symbolSelect").val();
	const side = inputRow.find(".side").val();
	const fromPrice = inputRow.find(".fromPrice").val();
	const toPrice = inputRow.find(".toPrice").val();
	const priceCondition = inputRow.find(".priceCondition").val();
	const conditionSide = inputRow.find(".conditionSide").val();
	const qtyCondition = inputRow.find(".qtyCondition").val();
	const qtyThreshold = inputRow.find(".qtyThreshold").val();
	ws.send(command("writeRule", [ruleID,
		symbol,
		side,
		fromPrice,
		toPrice,
		priceCondition,
		conditionSide,
		qtyCondition,
		qtyThreshold]));
}

function sendDeleteRule(row) {
	ws.send(command("deleteRule", [row.data('ruleID')]));
}

function sendStartRule(row) {
	ws.send(command("startRule", [row.data('ruleID')]));
}

function sendStopRule(row) {
	ws.send(command("stopRule", [row.data('ruleID')]));
}

function sendStartAll() {
	ws.send(command("startAllRules", []));
}

function sendStopAll() {
	ws.send(command("stopAllRules", []));
}
