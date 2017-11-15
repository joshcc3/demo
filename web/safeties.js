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

	$("#minimiseAll").unbind("click").bind("click", function () {
		$(".nibblerBlock").toggleClass("minimised", true);
	});
	$("#maximiseAll").unbind("click").bind("click", function () {
		$(".nibblerBlock").toggleClass("minimised", false);
	});
});

function checkWarning() {

	$(".pinned").toggleClass("pinned", false);

	if (checkWarnedForAWhile()) {

		if (!dogBarkSound.readyState) {
			dogBarkSound.load();
		}
		dogBarkSound.play();
	}
}

function checkWarnedForAWhile() {

	var isLongEnough = false;
	var minTime = new Date().getTime() - 5000;
	$(".isWarning, .isError").each(function () {
		var warningTime =  parseInt($(this).attr("data-warningTime"), 10);
		if (warningTime < minTime) {
			$(this).toggleClass("pinned", true);
			return isLongEnough = true;
		}
	});
	return isLongEnough;
}

function setNibblerConnected(source, isConnected) {

	var table = $('#' + source);
	if (table.length < 1) {

		table = $("#nibblerTemplate").clone();
		table.attr("id", source);
		table.removeClass("hidden");

		var nibblerName = table.find(".nibblerName");
		nibblerName.text(source);
		nibblerName.unbind("click").bind("click", function () {
			table.toggleClass("minimised", !table.hasClass("minimised"));
		});

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
	row.toggleClass("isWarning", !isEnabled);
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

	if (isWarning && !row.hasClass("isWarning")) {
		row.attr("data-warningTime", new Date().getTime());
	}
	row.toggleClass("isWarning", isWarning);
	row.toggleClass("isError", isError);

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

	if (input.val() != value && (!input.hasClass("notPersisted") || input.val().replace(/,| /g, "") == value)) {
		input.val(value);
	}
	input.attr("data", value);
	input.toggleClass("notPersisted", input.val() != input.attr("data"));
}

function submitRow(row) {

	if (row.find(".notPersisted").length) {
		var nibblerName = row.parent().attr("id");
		var rowID = row.attr("data-remoteSafetyID");
		var limitStr = row.find(".limit input").val().replace(/,| /g, "");

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
