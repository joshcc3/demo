var ws;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (m) {
		eval(m);
	};

	ws.send("subscribe");
	document.addEventListener('contextmenu', function (event) {
		event.preventDefault();
	});
	$("#stacksCreation").unbind().bind("click", function () {
		popUp("/stackStrategy", "Stacks", 1800, 800);
	});
	$("#stackConfig").unbind().bind("click", function () {
		popUp("/stackConfig", "Configs", 2200, 800);
	});

	$("#stackParentalRule").unbind().bind("click", function () {
		ws.send(command("refreshAllParents", []));
	});
	$("#allStart").unbind().bind("click", function () {
		ws.send(command("startAll", []));
	});
	$("#allStop").unbind().bind("click", function () {
		ws.send(command("stopAll", []));
	});
	$("#cleanAllParents").unbind().bind("click", function () {
		ws.send(command("cleanAllParents", []));
	});
	$("#minimiseFamilies").unbind().bind("click", function () {
		minimiseAll();
	});

	var childSymbolSearchInput = $("#symbolLookup");
	childSymbolSearchInput.unbind("input").bind("input", function () {
		var symbol = childSymbolSearchInput.val();
		var child = findChild(symbol);
		childSymbolSearchInput.toggleClass("childAvailable", 1 == child.length);
	});

	childSymbolSearchInput.bind("keypress", function (e) {
		if (e.keyCode == 13) {
			var symbol = childSymbolSearchInput.val();
			showChild(symbol);
		}
	});
	$("#symbolLookupGo").unbind().bind("click", function () {
		var symbol = childSymbolSearchInput.val();
		showChild(symbol);
	});

	var familyNameInput = $("#quoteSymbol");
	familyNameInput.on("input", function () {
		var symbol = familyNameInput.val();
		ws.send(command("checkFamilyInst", [symbol, "#creationInfoRow"]));
	});
	$("#createFamily").unbind().bind("click", function () {
		var symbol = familyNameInput.val();
		ws.send(command("createFamily", [symbol]));
	});

	var familyInput = $("#familySymbol");
	familyInput.on("input", function () {
		var family = familyInput.val();
		ws.send(command("checkFamilyExists", [family, "#adoptionInfoRow .familyFound"]));
	});
	var childInput = $("#childSymbol");
	childInput.on("input", function () {
		var child = childInput.val();
		ws.send(command("checkChildExists", [child, "#adoptionInfoRow .childFound"]));
	});
	$("#adoptChild").unbind().bind("click", function () {
		var family = familyInput.val();
		var child = childInput.val();
		ws.send(command("adoptChild", [family, child]));
	});

	$("#header").unbind("dblclick").bind("dblclick", showAdmin);
});

function popUp(url, name, width, height) {

	window.open(url, name, 'width=' + width + ',height=' + height);
	window.focus();
}

function clearFieldData(fieldID) {

	var infoRow = $(fieldID);
	infoRow.toggleClass("unknown", true);
	infoRow.find("div").text("");
}

function setInstID(isin, ccy, mic, instType) {

	var infoRow = $("#creationInfoRow");
	infoRow.toggleClass("unknown", false);
	infoRow.find(".isin").text(isin);
	infoRow.find(".ccy").text(ccy);
	infoRow.find(".mic").text(mic);
	infoRow.find(".instType").text(instType);
}

function setFieldData(fieldID, text) {

	var infoRow = $(fieldID);
	infoRow.toggleClass("unknown", false);
	infoRow.find("div").text(text);
}

function removeAll(nibblerName) {
	var table = getExchangeTable(nibblerName);
	table.find(".dataRow").remove();
	setOrderCount(nibblerName);
}

function findChild(symbol) {

	var rowID = symbol.replace(/ |\/|\.|:/g, "_");
	return $("#" + rowID);
}

function showChild(symbol) {
	var child = findChild(symbol);
	if (1 == child.length) {
		minimiseAll();
		child.parent().toggleClass("hidden", false);

		$(".childSearchResult").toggleClass("childSearchResult", false);
		child.toggleClass("childSearchResult", true);
		window.setTimeout(function () {
			child.toggleClass("childSearchResult", false);
		}, 2500);
	}
}

function minimiseAll() {
	$(".family .children").toggleClass("hidden", true);
}

function addFamily(familyName) {

	var familyID = "family_" + familyName;
	var family = $("#" + familyID);
	if (family.length < 1) {

		family = $("#templateFamily").clone();
		family.attr("id", familyID);
		family.removeClass("template");

		var familyDetails = family.find(".familyDetails");
		var familyBlock = family.find(".children");
		familyDetails.find(".openStack").unbind().bind("click", function () {
			launchLadder(familyName + ";S");
		});
		familyDetails.find(".refreshParent").unbind().bind("click", function () {
			ws.send(command("refreshParent", [familyName]));
		});

		var bidStartButton = family.find(".bid .start");
		bidStartButton.unbind().bind("click", function () {
			ws.send(command("startFamily", [familyName, "BID"]));
		});

		var bidStopButton = family.find(".bid .stop");
		bidStopButton.unbind().bind("click", function () {
			ws.send(command("stopFamily", [familyName, "BID"]));
		});

		var askStartButton = family.find(".ask .start");
		askStartButton.unbind().bind("click", function () {
			ws.send(command("startFamily", [familyName, "ASK"]));
		});

		var askStopButton = family.find(".ask .stop");
		askStopButton.unbind().bind("click", function () {
			ws.send(command("stopFamily", [familyName, "ASK"]));
		});

		var familyNameDiv = family.find(".familyName");
		familyNameDiv.text(familyName);
		familyNameDiv.unbind().bind("click", function () {
			familyBlock.toggleClass("hidden", !familyBlock.hasClass("hidden"));
		});

		var bidOffsetUpButton = family.find(".bid .priceOffsetUp");
		bidOffsetUpButton.mousedown(priceOffsetChange("increaseOffset", familyName, "BID", 1));

		var bidOffsetDownButton = family.find(".bid .priceOffsetDown");
		bidOffsetDownButton.mousedown(priceOffsetChange("increaseOffset", familyName, "BID", -1));

		var askOffsetUpButton = family.find(".ask .priceOffsetUp");
		askOffsetUpButton.mousedown(priceOffsetChange("increaseOffset", familyName, "ASK", 1));

		var askOffsetDownButton = family.find(".ask .priceOffsetDown");
		askOffsetDownButton.mousedown(priceOffsetChange("increaseOffset", familyName, "ASK", -1));

		var selectDefaultConfigDiv = family.find(".configControls .default");
		selectDefaultConfigDiv.unbind().bind("click", function () {
			ws.send(command("selectConfig", [familyName, "DEFAULT"]));
		});

		var selectWideConfigDiv = family.find(".configControls .wide");
		selectWideConfigDiv.unbind().bind("click", function () {
			ws.send(command("selectConfig", [familyName, "WIDE"]));
		});

		var selectObligationConfigDiv = family.find(".configControls .obligation");
		selectObligationConfigDiv.unbind().bind("click", function () {
			ws.send(command("selectConfig", [familyName, "OBLIGATION"]));
		});

		var openConfigWindowDiv = family.find(".configControls .configWindow");
		openConfigWindowDiv.unbind().bind("click", function () {
			popUp("/stackConfig#;" + familyName, "Configs", 2200, 400);
		});

		var bidPicardDiv = family.find(".bid .picardEnabled");
		bidPicardDiv.mousedown(stackEnableStackChange(familyName, "BID", "PICARD"));

		var bidQuoterDiv = family.find(".bid .quoterEnabled");
		bidQuoterDiv.mousedown(stackEnableStackChange(familyName, "BID", "QUOTER"));

		var askQuoterDiv = family.find(".ask .quoterEnabled");
		askQuoterDiv.mousedown(stackEnableStackChange(familyName, "ASK", "QUOTER"));

		var askPicardDiv = family.find(".ask .picardEnabled");
		askPicardDiv.mousedown(stackEnableStackChange(familyName, "ASK", "PICARD"));

		var allEnableDiv = family.find(".stackControls.allEnabled");
		allEnableDiv.mousedown(stackEnableAllStackChange(familyName));

		var cleanFamilyDiv = family.find(".stackControls.cleanParent");
		cleanFamilyDiv.mousedown(function () {
			ws.send(command("cleanParent", [familyName]));
		});

		addSortedDiv($("#families").find(".family"), family, tableComparator);

		setChildCount(familyName);
	}
	return family;
}

function tableComparator(a, b) {

	var aSymbol = $(a).find(".familyDetails .familyName").text();
	var bSymbol = $(b).find(".familyDetails .familyName").text();

	return aSymbol < bSymbol ? -1 : aSymbol == bSymbol ? 0 : 1;
}

function priceOffsetChange(cmd, familyName, side, direction) {
	return function (event) {
		event.preventDefault();
		switch (event.which) {
			case 1:
			{
				ws.send(command(cmd, [familyName, side, direction]));
				break;
			}
			case 3:
			{
				ws.send(command(cmd, [familyName, side, 10 * direction]));
				break;
			}
		}
	}
}

function stackEnableStackChange(familyName, side, stack) {
	return function (event) {
		event.preventDefault();
		switch (event.which) {
			case 1:
			{
				ws.send(command("setStackEnabled", [familyName, side, stack, true]));
				break;
			}
			case 3:
			{
				ws.send(command("setStackEnabled", [familyName, side, stack, false]));
				break;
			}
		}
	}
}

function stackEnableAllStackChange(familyName) {
	return function (event) {
		event.preventDefault();
		switch (event.which) {
			case 1:
			{
				ws.send(command("setAllStacksEnabled", [familyName, true]));
				break;
			}
			case 3:
			{
				ws.send(command("setAllStacksEnabled", [familyName, false]));
				break;
			}
		}
	}
}

function setParentData(familyName, bidPriceOffset, askPriceOffset, selectedConfigType, bidPicardEnabled, bidQuoterEnabled, askPicardEnabled,
					   askQuoterEnabled) {

	var family = addFamily(familyName);

	family.find(".familyDetails .bid.priceOffset").text(bidPriceOffset);
	family.find(".familyDetails .ask.priceOffset").text(askPriceOffset);

	family.find(".familyDetails .configControls button").removeClass("enabled");
	family.find(".familyDetails .configControls ." + selectedConfigType.toLowerCase()).addClass("enabled");

	family.find(".familyDetails .bid .picardEnabled").toggleClass("enabled", bidPicardEnabled);
	family.find(".familyDetails .bid .quoterEnabled").toggleClass("enabled", bidQuoterEnabled);

	family.find(".familyDetails .ask .picardEnabled").toggleClass("enabled", askPicardEnabled);
	family.find(".familyDetails .ask .quoterEnabled").toggleClass("enabled", askQuoterEnabled);
}

function setChild(familyName, childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier) {

	var rowID = childSymbol.replace(/ |\/|\.|:/g, "_");
	var row = $("#" + rowID);

	var exchangeTable = addFamily(familyName).find(".children");
	if (row.length < 1) {

		row = exchangeTable.find(".header").clone();
		row.removeClass("header");
		row.attr("id", rowID);
		row.addClass("dataRow");

		var quoteSymbolCell = setCellData(row.find(".symbol"), childSymbol);

		addSortedDiv(exchangeTable.find(".row"), row, rowComparator);

	} else if (row.parent().get(0) != exchangeTable.get(0)) {

		var oldFamily = row.parent().parent().find(".familyName").text();
		row.remove();
		setChildCount(oldFamily);

		addSortedDiv(exchangeTable.find(".row"), row, rowComparator);
	}

	quoteSymbolCell.unbind().bind("click", function () {
		launchLadder(childSymbol);
	});

	var bidPriceOffsetDiv = addNumberBox(row, ".bid.priceOffset");
	var bidQtyMultiplierDiv = addNumberBox(row, ".bid.qtyMultiplier", bidQtyMultiplier);

	var askPriceOffsetDiv = addNumberBox(row, ".ask.priceOffset", askPriceOffset);
	var askQtyMultiplierDiv = addNumberBox(row, ".ask.qtyMultiplier", askQtyMultiplier);

	setDoubleData(bidPriceOffsetDiv, bidPriceOffset);
	setDoubleData(bidQtyMultiplierDiv, bidQtyMultiplier);
	setDoubleData(askPriceOffsetDiv, askPriceOffset);
	setDoubleData(askQtyMultiplierDiv, askQtyMultiplier);

	var submitButton = row.find("input[type=Submit]");
	submitButton.removeClass("hidden");
	submitButton.unbind().bind("click", function () {

		var bidOffset = bidPriceOffsetDiv.val();
		var bidMultiplier = bidQtyMultiplierDiv.val();

		var askOffset = askPriceOffsetDiv.val();
		var askMultiplier = askQtyMultiplierDiv.val();

		ws.send(command("setRelationship", [childSymbol, bidOffset, bidMultiplier, askOffset, askMultiplier]));
	});

	var orphanButton = row.find(".orphanButton");
	orphanButton.removeClass("hidden");

	orphanButton.unbind().bind("click", function () {
		ws.send(command("orphanChild", [childSymbol]));
	});

	setChildCount(familyName);
}

function rowComparator(a, b) {

	var aSymbol = $(a).find(".symbol").text();
	var bSymbol = $(b).find(".symbol").text();

	return aSymbol < bSymbol ? -1 : aSymbol == bSymbol ? 0 : 1;
}

function addNumberBox(row, selector) {

	var div = row.find(selector);
	var input = div.find("input");

	if (!input.length) {
		input = $("<input type=\"number\"/>");
		div.text("");
		div.append(input);
	}
	input.off("input").on("input", function () {
		input.toggleClass("notPersisted", input.val() != input.attr("data"));
	});

	return input;
}

function setCellData(input, value) {

	if (typeof value != "undefined") {
		input.attr("data", value);
		input.text(value);
	} else {
		input.attr("data", "");
		input.text("");
	}
	return input
}

function setDoubleData(input, value) {

	if (typeof value != "undefined") {
		input.attr("data", value);
		input.val(parseFloat(value));
	} else {
		input.attr("data", "");
		input.val("");
	}
	input.removeClass("notPersisted");
}

function setChildCount(familyName) {

	var nibbler = addFamily(familyName);
	var orderCountDiv = nibbler.find(".childCount");
	var orders = nibbler.find(".children .row").length - 1;
	orderCountDiv.text(orders);
}

function displayErrorMsg(text) {

	var errorDiv = $("#footer");
	errorDiv.text(text);
	console.log("ERROR", text, errorDiv);
	setTimeout(hideErrorMsg, 5000);
}

function hideErrorMsg() {
	var errorDiv = $("#footer");
	errorDiv.text("");
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