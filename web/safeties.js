var ws;

var dogBarkSound;

$(function () {
	ws = connect();
	ws.onmessage = function (data) {
		eval(data);
	};

	ws.send("subscribe");

	dogBarkSound = new Audio("sounds/quack.wav");
	checkWarning();
	setInterval(checkWarning, 60000);

	$("#header").unbind("dblclick").bind("dblclick", showAdmin);
});

function checkWarning() {

	var count = $(".isWarning, .isError .shouldQuack").length;
	if (0 < count) {

		if (!dogBarkSound.readyState) {
			dogBarkSound.load();
		}
		dogBarkSound.play();
	}
}

function setNibblerConnected(source, isConnected) {

	var table = $('#' + source);
	if (table.length < 1) {

		table = $("#nibblerTemplate").clone();
		table.attr("id", source);
		table.removeClass("hidden");

		table.find(".nibblerName").text(source);

		var nibblers = $("#nibblers");
		addSortedDiv(nibblers.find(".nibblerBlock"), table, compareNibblerRow);

		table.find(".enableOMS").off("click").click(function () {
			ws.send("enableOMS," + source);
		});

		table.find(".debugButton").off("click").click(function () {
			var text = table.find(".debugText").val();
			ws.send("sendDebug," + source + "," + text);
		});
	}

	table.find(".nibblerName").toggleClass("connectionDown", !isConnected);
}

function compareNibblerRow(a, b) {

	var aName = a.find(".nibblerName").text();
	var bName = b.find(".nibblerName").text();

	return aName < bName ? -1 : aName == bName ? 0 : 1;
}

function setOMS(id, source, remoteOMSID, omsName, isEnabled, stateText) {

	var row = $("#" + id);

	if (row.length < 1) {

		var table = $('#' + source);

		var header = table.find(".omsRow.headerRow");
		row = header.clone();

		row.attr("id", id);
		row.removeClass("headerRow");
		row.attr("data-remoteOMSID", remoteOMSID);

		row.find(".omsName").text(omsName);
		addSortedDiv(table.find(".omsRow"), row, compareOMSRow);
	}

	row.toggleClass("hidden", isEnabled);
	row.find(".stateText").text(stateText);
}

function setRow(id, source, remoteSafetyID, safetyName, limit, warning, current, lastSymbol, isEditable, isWarning, isError) {

	var row = $("#" + id);

	if (row.length < 1) {

		var table = $('#' + source);

		var header = table.find(".safetyRow.headerRow");
		row = header.clone();

		row.attr("id", id);
		row.removeClass("headerRow");
		row.attr("data-remoteSafetyID", remoteSafetyID);

		row.find(".safetyName").text(safetyName);
		addSortedDiv(table.find(".safetyRow"), row, compareBlotterRow);
	}

	row.toggleClass("isWarning", isWarning);
	row.toggleClass("isError", isError);
	// Temporary way of limiting quacks to OTR ratios
	row.toggleClass("shouldQuack", safetyName.indexOf("Throttle: ") == -1);

	if (isEditable) {
		setEditableCell(row, ".limit", limit);
		row.find(".submit").off("click").click(function () {
			submitRow(row);
		});
	} else {
		row.find(".limit").text(limit);
	}

	row.find(".warning").text(warning);
	row.find(".current").text(current);
	row.find(".submit").toggleClass("hidden", !isEditable);

	if (isWarning || isError) {
		row.find(".lastSymbol").text(lastSymbol);
	} else {
		row.find(".lastSymbol").text("");
	}
}

function setEditableCell(row, selector, value) {

	var d = row.find(selector);
	var input = d.find("input");
	if (!input.length) {
		input = $("<input type=\"text\"/>");
		d.text("");
		d.append(input);

		input.on("input", function () {
			input.toggleClass("notPersisted", input.val() != input.attr("data"));
		});
	}

	if (input.val() != value) {
		input.val(value);
	}
	input.attr("data", value);
	input.toggleClass("notPersisted", input.val() != input.attr("data"));
}

function submitRow(row) {

	if (row.find(".notPersisted").length) {
		var nibblerName = row.parent().attr("id");
		var rowID = row.attr("data-remoteSafetyID");
		var limitStr = row.find(".limit input").val().replace(/,/g, "");

		ws.send("setLimit," + nibblerName + "," + rowID + "," + limitStr);
	}
}

function removeRow(id) {

	var row = $("#" + id);
	row.remove();
}

function compareOMSRow(a, b) {

	var aTime = a.find(".omsName").text();
	var bTime = b.find(".omsName").text();

	return bTime < aTime ? 1 : aTime == bTime ? 0 : -1;
}

function compareBlotterRow(a, b) {

	var aTime = a.find(".safetyName").text();
	var bTime = b.find(".safetyName").text();

	return bTime < aTime ? 1 : aTime == bTime ? 0 : -1;
}

function removeAllRows(source) {

	var table = $('#' + source);
	var omsRows = table.find(".omsRow:not(.headerRow)");
	omsRows.remove();
	var safetyows = table.find(".safetyRow:not(.headerRow)");
	safetyows.remove();
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
