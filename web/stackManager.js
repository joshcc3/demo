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

	var familyIsinInput = $("#familySymbolLookup");
	familyIsinInput.on("input", function () {
		var symbol = familyIsinInput.val();
		ws.send(command("checkFamilyInst", [symbol, "#creationInfoRow"]));
	});
	$("#findFamilyMembers").unbind().bind("click", function () {
		var symbol = familyIsinInput.val();
		$(".childCreationRow:not(.headerRow)").remove();
		ws.send(command("findFamilyMembers", [symbol]));
	});

	var familyNameInput = $("#quoteSymbol");
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

function setCreateFamilyRow(symbol, isFamilyExists) {

	var familyNameInput = $("#quoteSymbol");
	familyNameInput.val(symbol);
	familyNameInput.toggleClass("childAvailable", isFamilyExists);
}

function addCreateChildRow(childSymbol, isChildAlreadyCreated, nibblers, instTypes, leanInstType, leanSymbol) {

	var childTable = $("#createChildrenTable");
	var childCreationRow = $(".childCreationRow.headerRow").clone();

	childCreationRow.removeClass("headerRow");

	var childQuoteSymbolInput = childCreationRow.find(".childQuoteSymbol");
	childQuoteSymbolInput.val(childSymbol);
	childQuoteSymbolInput.toggleClass("childAvailable", isChildAlreadyCreated);

	var nibblersCombo = childCreationRow.find(".hostNibblers");
	nibblers.forEach(function (nibbler) {

		var option = $("<option value=\"" + nibbler + "\">" + nibbler + "</option>");
		option.addClass(nibbler);
		option.attr("data", nibbler);
		nibblersCombo.append(option);
	});
	nibblersCombo.toggleClass("notPersisted", true);
	nibblersCombo.off("focus").focus(function () {
		nibblersCombo.toggleClass("notPersisted", false);
	});

	var instTypeCombo = childCreationRow.find(".leanInstID");
	instTypes.forEach(function (instType) {

		var option = $("<option value=\"" + instType + "\">" + instType + "</option>");
		option.addClass(instType);
		option.attr("data", instType);
		instTypeCombo.append(option);
	});
	instTypeCombo.val(leanInstType);

	childCreationRow.find(".childLeanSymbol").val(leanSymbol);

	childCreationRow.find(".viewLadder").unbind().bind("click", function () {
		launchLadder(childSymbol);
	});

	childCreationRow.find("button.createButton").off("click").click(function () {

		var quoteSymbol = childCreationRow.find("input[name=quote]").val();
		var forNibbler = childCreationRow.find(".hostNibblers").find("option:selected").text();
		var leanInstType = childCreationRow.find(".leanInstID").find("option:selected").text();
		var leanSymbol = childCreationRow.find("input[name=lean]").val();
		ws.send(command("createChildStack", [forNibbler, quoteSymbol, leanInstType, leanSymbol]));
	});

	childCreationRow.find("button.adoptButton").unbind().bind("click", function () {
		var family = $("#quoteSymbol").val();
		var child = childCreationRow.find("input[name=quote]").val();
		ws.send(command("adoptChild", [family, child]));
	});

	addSortedDiv(childTable.find(".row"), childCreationRow, childCreationComparator);
}

function childCreationComparator(a, b) {

	var aSymbol = a.find(".childQuoteSymbol").val();
	var bSymbol = b.find(".childQuoteSymbol").val();

	return aSymbol < bSymbol ? -1 : aSymbol == bSymbol ? 0 : 1;
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

	var familyID = "family_" + familyName.replace(/ |\/|\.|:/g, "_");
	var family = findChild(familyID);
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

		var bidStartButton = family.find(".familyDetails .bid .start");
		bidStartButton.unbind().bind("click", function () {
			ws.send(command("startFamily", [familyName, "BID"]));
		});

		var bidStopButton = family.find(".familyDetails .bid .stop");
		bidStopButton.unbind().bind("click", function () {
			ws.send(command("stopFamily", [familyName, "BID"]));
		});

		var askStartButton = family.find(".familyDetails .ask .start");
		askStartButton.unbind().bind("click", function () {
			ws.send(command("startFamily", [familyName, "ASK"]));
		});

		var askStopButton = family.find(".familyDetails .ask .stop");
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
			var configs = familyName;
			family.find(".children .row:not(.header)").each(function () {
				configs = configs + "," + $(this).attr("id").replace("_", " ");
			});
			popUp("/stackConfig#;" + configs, "Configs", 2200, 400);
		});

		openConfigWindowDiv.mousedown(function (e) {
			if (e.button == 2) {
				popUp("/stackConfig#;" + familyName, "Configs", 2200, 400);
			}
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

	var aSymbol = a.find(".familyDetails .familyName").text();
	var bSymbol = b.find(".familyDetails .familyName").text();

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

function stackChildEnableStackChange(familyName, childSymbol, side, stack) {
	return function (event) {
		event.preventDefault();
		switch (event.which) {
			case 1:
			{
				ws.send(command("setChildStackEnabled", [familyName, childSymbol, side, stack, true]));
				break;
			}
			case 3:
			{
				ws.send(command("setChildStackEnabled", [familyName, childSymbol, side, stack, false]));
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
	var quoteSymbolCell;
	if (row.length < 1) {

		row = exchangeTable.find(".header").clone();
		row.removeClass("header");
		row.attr("id", rowID);
		row.addClass("dataRow");

		quoteSymbolCell = setCellData(row.find(".symbol"), childSymbol);

		addSortedDiv(exchangeTable.find(".row"), row, rowComparator);

	} else if (row.parent().get(0) != exchangeTable.get(0)) {

		var oldFamily = row.parent().parent().find(".familyName").text();
		row.remove();
		setChildCount(oldFamily);

		quoteSymbolCell = row.find(".symbol");

		addSortedDiv(exchangeTable.find(".row"), row, rowComparator);
	} else {
		quoteSymbolCell = row.find(".symbol");
	}

	quoteSymbolCell.unbind().bind("click", function () {
		launchLadder(childSymbol);
	});

	var bidPriceOffsetDiv = addNumberBox(row, ".bid.priceOffset");
	var bidQtyMultiplierDiv = addNumberBox(row, ".bid.qtyMultiplier");

	var askPriceOffsetDiv = addNumberBox(row, ".ask.priceOffset");
	var askQtyMultiplierDiv = addNumberBox(row, ".ask.qtyMultiplier");

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

	var defaultConfigButton = row.find(".childControls .default");
	defaultConfigButton.unbind().bind("click", function () {
		ws.send(command("setChildSelectedConfig", [familyName, childSymbol, "DEFAULT"]));
	});

	var wideConfigButton = row.find(".childControls .wide");
	wideConfigButton.unbind().bind("click", function () {
		ws.send(command("setChildSelectedConfig", [familyName, childSymbol, "WIDE"]));
	});

	var obligationConfigButton = row.find(".childControls .obligation");
	obligationConfigButton.unbind().bind("click", function () {
		ws.send(command("setChildSelectedConfig", [familyName, childSymbol, "OBLIGATION"]));
	});

	var openConfigWindowDiv = row.find(".childControls .configWindow");
	openConfigWindowDiv.unbind().bind("click", function () {
		popUp("/stackConfig#;" + childSymbol, "Configs", 2200, 400);
	});

	var bidPicardButton = row.find(".childControls .bid .picardEnabled");
	var bidQuoterButton = row.find(".childControls .bid .quoterEnabled");
	var askQuoterButton = row.find(".childControls .ask .quoterEnabled");
	var askPicardButton = row.find(".childControls .ask .picardEnabled");
	bidPicardButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "BID", "PICARD"));
	bidQuoterButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "BID", "QUOTER"));
	askQuoterButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "ASK", "QUOTER"));
	askPicardButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "ASK", "PICARD"));

	var bidStartConfigWindowDiv = row.find(".childControls .bid .start");
	bidStartConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("startChild", [familyName, childSymbol, "BID"]));
	});

	var bidStopConfigWindowDiv = row.find(".childControls .bid .stop");
	bidStopConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("stopChild", [familyName, childSymbol, "BID"]));
	});

	var askStartConfigWindowDiv = row.find(".childControls .ask .start");
	askStartConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("startChild", [familyName, childSymbol, "ASK"]));
	});

	var askStopConfigWindowDiv = row.find(".childControls .ask .stop");
	askStopConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("stopChild", [familyName, childSymbol, "ASK"]));
	});

	setChildCount(familyName);
}

function setChildData(childSymbol, nibblerName, selectedConfigType, isBidStrategyOn, bidInfo, bidPicardEnabled, bidQuoterEnabled, isAskStrategyOn,
					  askInfo, askPicardEnabled, askQuoterEnabled) {

	var rowID = childSymbol.replace(/ |\/|\.|:/g, "_");
	var row = $("#" + rowID);

	if (row) {

		row.find(".childControls .configControls button").removeClass("enabled");
		row.find(".childControls .configControls ." + selectedConfigType.toLowerCase()).addClass("enabled");

		row.find(".childControls .nibblerName").text(nibblerName);

		row.find(".childControls .bid.runningControls").toggleClass("enabled", isBidStrategyOn);
		row.find(".childControls .bid .picardEnabled").toggleClass("enabled", bidPicardEnabled);
		row.find(".childControls .bid .quoterEnabled").toggleClass("enabled", bidQuoterEnabled);
		row.find(".childControls .bid.strategyInfo").text(bidInfo);

		row.find(".childControls .ask.runningControls").toggleClass("enabled", isAskStrategyOn);
		row.find(".childControls .ask .picardEnabled").toggleClass("enabled", askPicardEnabled);
		row.find(".childControls .ask .quoterEnabled").toggleClass("enabled", askQuoterEnabled);
		row.find(".childControls .ask.strategyInfo").text(askInfo);
	}
}

function rowComparator(a, b) {

	var aSymbol = a.find(".symbol").text();
	var bSymbol = b.find(".symbol").text();

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