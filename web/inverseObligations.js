let handler;
const Contracts = {};
const Meeting = {};
let HideOK = false;

let $okButton = $("button.toggleOK");
$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (x) {
		eval(x);
	};
	$okButton.unbind('click').bind('click', toggleHideOK);

	HideOK = localStorage.hideOK === "true";
	HideFake = localStorage.hideFake === "true";

	$okButton.text(HideOK ? "No OK" : "OK");

});

function setObligation(symbol, isFailing, percentage) {

	let row = Contracts[symbol];

	if (!Contracts.hasOwnProperty(symbol)) {
		const spreadTable = $("#spreadTable");
		row = spreadTable.find('tr.template').clone().removeClass('template');
		Contracts[symbol] = row;
		const symbolCell = row.find('.symbol');
		symbolCell.text(symbol);
		symbolCell.bind('click', function () {
			launchLadder(symbol)
		});
		row.data('symbol', symbol);
		spreadTable.addSorted(row, function (a, b) {
			a = $(a);
			b = $(b);
			return a.data('symbol') < b.data('symbol') ? -1 : a.data('symbol') === b.data('symbol') ? 0 : 1;
		});
	}

	row.find(".percentage").text(percentage);
	row.toggleClass("obligationMet", !isFailing);
	row.toggleClass("obligationNotMet", isFailing);

	row.toggleClass("hidden", !isFailing && HideOK);

	Meeting[symbol] = !isFailing;
	updateCounter();
}

function updateCounter() {
	let count = 0;
	let total = 0;
	for (let symbol in Meeting) {
		if (Meeting[symbol]) {
			count += 1;
		}
		total += 1;
	}
	$(".met").text(count);
	$(".total").text(total);
}

function toggleHideOK() {
	HideOK = !HideOK;
	localStorage.hideOK = "" + HideOK;
	$okButton.text(HideOK ? "No OK" : "OK");
	updateHidden();
}

function updateHidden() {
	for (let symbol in Contracts) {
		const row = Contracts[symbol];
		const obligationMet = row.hasClass("obligationMet");
		row.toggleClass("hidden", obligationMet && HideOK);
	}
}
