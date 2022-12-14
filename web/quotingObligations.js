let meowSound;
new Date().getTime();
let communityName;

$(function () {

	ws = connect();
	ws.logToConsole = false;

	ws.onmessage = function (m) {
		eval(m);
	};

	const hash = document.location.hash.substr(1);
	if (0 === hash.length) {
		communityName = "DEFAULT";
	} else {
		communityName = hash;
	}

	ws.send("subscribeToCommunity," + communityName)

	$(window).trigger("startup-done");

	meowSound = new Audio("sounds/meow.wav");

	const strategies = $("#strategies");
	$("#showAll").change(function () {
		strategies.toggleClass("hideMetObligations", !this.checked);
	});

	$("#showOff").change(function () {
		strategies.toggleClass("hideOnStrategies", this.checked);
	});

	$("#everythingOn").click(function () {
		ws.send(command("everythingOn", [communityName]));
	});
	$("#everythingOff").click(function () {
		ws.send(command("everythingOff", [communityName]));
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

function setRow(rowID, symbol, sourceNibbler, percentageOn, isEnabled, isStrategyOn, isStrategyQuoting, stateDescription,
	isObligationFail) {

	let row = $("#" + rowID);
	if (row.size() < 1) {

		const table = $("#strategies");
		row = $("#strategyTemplate").clone();

		row.removeClass("headerRow");

		row.attr("id", rowID);
		const symbolCell = row.find(".symbol");
		symbolCell.text(symbol);
		row.find(".nibblerName").text(sourceNibbler);

		symbolCell.unbind().bind("click", function () {
			launchLadder(symbol);
		});
		row.find(".smallButton.strategyOn").click(function () {
			ws.send(command("startStrategy", [communityName, symbol]));
		});
		row.find(".smallButton.strategyOff").click(function () {
			ws.send(command("stopStrategy", [communityName, symbol]));
		});
		row.find(".enabled").change(function () {
			ws.send(command("setEnabledState", [communityName, symbol]))
		});

		addSortedDiv(table.find(".row"), row, function (a, b) {
			const aID = a.attr("id");
			const bID = b.attr("id");
			return aID < bID ? -1 : aID === bID ? 0 : 1;
		});
	}

	const wasOn = row.hasClass("strategyOn");
	row.toggleClass("wasOn", wasOn);

	row.toggleClass("obligationFail", isObligationFail);
	row.toggleClass("strategyOn", isStrategyOn);
	row.toggleClass("strategyQuoting", isStrategyQuoting);
	row.find(".enabled").attr('checked', isEnabled);
	row.find(".percentageOn").text(percentageOn);
	row.find(".description").text(stateDescription);

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
