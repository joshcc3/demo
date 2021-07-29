let handler;
const Contracts = {};
const Meeting = {};
let HideOK = false;
let HideFake = false;

$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (x) {
		eval(x);
	};
	$("button.toggleOK").unbind('click').bind('click', toggleHideOK);
	$("button.toggleFake").unbind('click').bind('click', toggleHideFake);

	HideOK = localStorage.hideOK == "true";
	HideFake = localStorage.hideFake == "true";

	$("button.toggleOK").text(HideOK ? "No OK" : "OK");
	$("button.toggleFake").text(HideFake ? "No Fake" : "Fake");

});

function setObligation(symbol, type, bpsObligation, qtyObligation, obligationMet, bpsWide, qtyShowing) {

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
			return a.data('symbol') < b.data('symbol') ? -1 : a.data('symbol') == b.data('symbol') ? 0 : 1;
		});
	}

	row.find('.symbol').toggleClass('widthInIndexPoints', type === 'INDEX_POINTS');
	row.find('.bpsObligation').text(bpsObligation);
	row.find('.qtyObligation').text(qtyObligation);
	row.find('.bpsWide').text(bpsWide);
	row.find('.qtyShowing').text(qtyShowing);
	row.toggleClass("obligationMet", obligationMet);
	row.toggleClass("obligationNotMet", !obligationMet);

	let fakeObligation = "1" === qtyObligation;
	row.toggleClass("fakeObligation", fakeObligation);
	row.toggleClass("realObligation", !fakeObligation);
	row.toggleClass("hidden", (obligationMet && HideOK) || (fakeObligation && HideFake));

	Meeting[symbol] = obligationMet;
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
	$("button.toggleOK").text(HideOK ? "No OK" : "OK");
	updateHidden();
}

function toggleHideFake() {
	HideFake = !HideFake;
	localStorage.hideFake = "" + HideFake;
	$("button.toggleFake").text(HideFake ? "No Fake" : "Fake");
	updateHidden();
}

function updateHidden() {
	for (let symbol in Contracts) {
		const row = Contracts[symbol];
		const fakeObligation = row.hasClass("fakeObligation");
		const obligationMet = row.hasClass("obligationMet");
		row.toggleClass("hidden", (fakeObligation && HideFake) || (obligationMet && HideOK));
	}
}
