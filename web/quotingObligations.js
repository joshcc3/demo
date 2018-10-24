let handler;

let meowSound;
const LastMeowTime = new Date().getTime();

$(function () {

	ws = connect();
	handler = new Handler(ws);
	ws.logToConsole = false;
	ws.onmessage = handler.msg;

	$(window).trigger("startup-done");

	meowSound = new Audio("sounds/meow.wav");

	const strategies = $("#strategies");
	$("#showAll").change(function () {
		strategies.toggleClass("hideMetObligations", !this.checked);
	});
});

function checkWarning() {

	if (!meowSound.readyState) {
		meowSound.load();
	}

	let isMeowTime = false;
	$(".row:not(.headerRow)").each(function () {
		const row = $(this);

		if (row.hasClass("strategyOn")) {
			row.toggleClass("offWarningGiven", false);
		} else if (!row.hasClass("offWarningGiven")) {
			row.toggleClass("offWarningGiven", true);
			isMeowTime = true;
		}
	});

	if (isMeowTime) {
		meowSound.play();
	}
}

function setRow(rowID, symbol, percentageOn, isStrategyOn, isObligationFail) {

	let row = $("#" + rowID);
	if (row.size() < 1) {

		const table = $("#strategies");
		row = $("#strategyTemplate").clone();

		row.removeClass("headerRow");

		row.attr("id", rowID);
		row.find(".symbol").text(symbol);

		addSortedDiv(table.find(".row"), row, function (a, b) {
			const aID = a.attr("id");
			const bID = b.attr("id");
			return aID < bID ? -1 : aID == bID ? 0 : 1;
		});
	}

	row.toggleClass("obligationFail", isObligationFail);
	row.toggleClass("strategyOn", isStrategyOn);
	row.find(".percentageOn").text(percentageOn);

	row.toggleClass("hidden", false);
}

function addSortedDiv(tableRows, row, comparator) {

	let bottom = 0;
	let top = tableRows.length;
	while (bottom < top - 1) {

		const mid = Math.floor((bottom + top) / 2);

		if (0 < comparator($(row), $(tableRows[mid]))) {
			bottom = mid;
		} else {
			top = mid;
		}
	}
	row.insertAfter($(tableRows[bottom]));
}

function deleteRow(id) {
	$("#" + id).toggleClass("hidden", true);
}
