var ws;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (m) {
		eval(m);
	};

	var adminBlock = $("#adminBlock");
	var hash = document.location.hash.substr(1);
	if (hash) {
		adminBlock.toggleClass("isAsylumView", true);
		ws.send("subscribe" + hash);
	} else {
		ws.send("subscribeFamily");
	}

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

	var saveOffsetButton = $("#saveOffsets");
	saveOffsetButton.unbind().bind("click", function () {

		if (!saveOffsetButton.hasClass("locked")) {
			ws.send(command("saveOffsets", []));
		}
	});
	var loadOffsetButton = $("#loadOffsets");
	loadOffsetButton.unbind().bind("click", function () {

		if (!loadOffsetButton.hasClass("locked")) {
			ws.send(command("loadOffsets", []));
		}
	});

	$("#enableSaveLoad").unbind().bind("click", function () {

		saveOffsetButton.toggleClass("locked", !saveOffsetButton.hasClass("locked"));
		loadOffsetButton.toggleClass("locked", !loadOffsetButton.hasClass("locked"));

		setTimeout(function () {
			saveOffsetButton.toggleClass("locked", true);
			loadOffsetButton.toggleClass("locked", true);
		}, 3000);
	});

	$("#globalPriceOffsetUp").unbind().mousedown(globalWidthControls("increaseGlobalPriceOffset"));
	$("#globalPriceOffsetDown").unbind().mousedown(globalWidthControls("decreaseGlobalPriceOffset"));

	var footer = $("#footer");
	var closeFooter = footer.find("#closeFooter");
	closeFooter.unbind().bind("click", function () {
		footer.toggleClass("hidden", true);
	});
	var clearFilters = footer.find("#clearFilters");
	clearFilters.unbind().bind("click", function () {
		$("#filterList").find(".filterName.selected").toggleClass("selected", false);
		updateSelectedFilters();
	});

	var filteringRow = $("#filteringBlock");
	var filterSelection = $("#filterButton");
	filterSelection.unbind().bind("click", function () {
		footer.toggleClass("hidden", !footer.hasClass("hidden"));
	});

	var filteredBidStartButton = filteringRow.find(".bid.runningControls .start");
	filteredBidStartButton.unbind().bind("click", function () {
		ws.send(command("startFiltered", [filterSelection.text(), "BID"]));
	});
	var filteredBidStopButton = filteringRow.find(".bid.runningControls .stop");
	filteredBidStopButton.unbind().bind("click", function () {
		ws.send(command("stopFiltered", [filterSelection.text(), "BID"]));
	});

	var filteredAskStartButton = filteringRow.find(".ask.runningControls .start");
	filteredAskStartButton.unbind().bind("click", function () {
		ws.send(command("startFiltered", [filterSelection.text(), "ASK"]));
	});
	var filteredAskStopButton = filteringRow.find(".ask.runningControls .stop");
	filteredAskStopButton.unbind().bind("click", function () {
		ws.send(command("stopFiltered", [filterSelection.text(), "ASK"]));
	});

	var defaultConfigButton = filteringRow.find(".configControls .default");
	defaultConfigButton.unbind().bind("click", function () {
		ws.send(command("setFilteredSelectedConfig", [filterSelection.text(), "DEFAULT"]));
	});
	var wideConfigButton = filteringRow.find(".configControls .wide");
	wideConfigButton.unbind().bind("click", function () {
		ws.send(command("setFilteredSelectedConfig", [filterSelection.text(), "WIDE"]));
	});
	var obligationConfigButton = filteringRow.find(".configControls .obligation");
	obligationConfigButton.unbind().bind("click", function () {
		ws.send(command("setFilteredSelectedConfig", [filterSelection.text(), "OBLIGATION"]));
	});

	var bidPicardDiv = filteringRow.find(".bid .picardEnabled");
	bidPicardDiv.mousedown(stackEnableFilteredStackChange(filterSelection, "BID", "PICARD"));

	var bidQuoterDiv = filteringRow.find(".bid .quoterEnabled");
	bidQuoterDiv.mousedown(stackEnableFilteredStackChange(filterSelection, "BID", "QUOTER"));

	var askQuoterDiv = filteringRow.find(".ask .quoterEnabled");
	askQuoterDiv.mousedown(stackEnableFilteredStackChange(filterSelection, "ASK", "QUOTER"));

	var askPicardDiv = filteringRow.find(".ask .picardEnabled");
	askPicardDiv.mousedown(stackEnableFilteredStackChange(filterSelection, "ASK", "PICARD"));

	var allEnableDiv = filteringRow.find(".stackControls.allEnabled");
	allEnableDiv.mousedown(stackEnableFilteredAllStackChange(filterSelection));

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

	$("#hideUnregistered").unbind().bind("click", function (cb) {
		adminBlock.toggleClass("hideUnregistered", cb.toElement.checked);
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

	$("#createMonthlyFutures").unbind().bind("click", function () {
		ws.send(command("createMonthlyFutures"));
	});
	$("#rollMonthlyFutures").unbind().bind("click", function () {
		ws.send(command("rollMonthlyFutures"));
	});
	$("#createQuaterlyFutures").unbind().bind("click", function () {
		ws.send(command("createQuaterlyFutures"));
	});
	$("#rollQuaterlyFutures").unbind().bind("click", function () {
		ws.send(command("rollQuaterlyFutures"));
	});
	$("#killExpiredFutures").unbind().bind("click", function () {
		ws.send(command("killExpiredFutures"));
	});
	$("#createAllRFQ").unbind().bind("click", function () {
		ws.send(command("createAllRFQ"));
	});
	$("#adoptAllRFQ").unbind().bind("click", function () {
		ws.send(command("adoptAllRFQ"));
	});

	var copyFromSymbolInput = $("#copyFromSymbol");
	copyFromSymbolInput.on("input", function () {
		var symbol = copyFromSymbolInput.val();
		ws.send(command("checkFamilyInst", [symbol, ".stackCopyFeedback.fromFound"]));
	});
	var copyToSymbolInput = $("#copyToSymbol");
	copyToSymbolInput.on("input", function () {
		var symbol = copyToSymbolInput.val();
		ws.send(command("checkFamilyInst", [symbol, ".stackCopyFeedback.toFound"]));
	});
	$("#copyStackData").unbind().bind("click", function () {
		var fromSymbol = copyFromSymbolInput.val();
		var toSymbol = copyToSymbolInput.val();
		ws.send(command("copyChildSetup", [fromSymbol, toSymbol]));
	});

	$("#header").unbind("dblclick").bind("dblclick", showAdmin);
	footer.unbind("dblclick").bind("dblclick", function (event) {
		event.preventDefault();
	});
});

function popUp(url, name, width, height) {

	window.open(url, name, 'width=' + width + ',height=' + height);
	window.focus();
}

function setGlobalOffset(globalOffset) {
	$("#globalPriceOffset").text(globalOffset);
}

function setFilters(filters) {

	var filterList = $("#filterList");
	filterList.find(".filterGroup:not(.template)").remove();

	var templateFilter = filterList.find(".filterGroup.template");

	for (var filterName in filters) {

		if (filters.hasOwnProperty(filterName)) {

			var filterGroupName = filters[filterName];

			var filterGroupID = cleanID("FILTER_GROUP_" + filterGroupName);
			var filterGroup = findChild(filterGroupID);

			if (filterGroup.length < 1) {
				filterGroup = templateFilter.clone();
				filterGroup.attr("id", filterGroupID);
				filterGroup.removeClass("template");
				filterGroup.find(".filterGroupName").text(filterGroupName);

				addSortedDiv(filterList.find(".filterGroup"), filterGroup, filterComparator);
			}

			var filterNameCell = filterGroup.find(".template.filterName").clone();

			filterNameCell.removeClass("template");
			filterNameCell.text(filterName);

			filterNameCell.unbind().bind("click", getFilterSelectionFunction(filterNameCell));

			addSortedDiv(filterGroup.find(".filterName"), filterNameCell, filterComparator);
		}
	}
}

function getFilterSelectionFunction(filterNameCell) {
	return function () {
		filterNameCell.toggleClass("selected", !filterNameCell.hasClass("selected"));
		updateSelectedFilters();
	};
}

function updateSelectedFilters() {

	var filterList = $("#filterList");
	var selectedFilters = filterList.find(".filterName.selected");

	var filterButton = $("#filterButton");

	if (selectedFilters.length < 1) {
		filterButton.text("None");
	} else {
		var selectedFilterNames = "";
		selectedFilters.each(function () {
			selectedFilterNames = selectedFilterNames + " | " + $(this).text();
		});
		selectedFilterNames = selectedFilterNames.substr(3, selectedFilterNames.length - 3);
		filterButton.text(selectedFilterNames);
	}
}

function filterComparator(a, b) {

	var aSymbol = a.find(".orderingName").text();
	var bSymbol = b.find(".orderingName").text();

	return aSymbol < bSymbol ? -1 : aSymbol == bSymbol ? 0 : 1;
}

function clearFieldData(fieldID) {

	var infoRow = $(fieldID);
	infoRow.toggleClass("unknown", true);
	infoRow.find("div").text("");
}

function setInstID(infoRowSelector, isin, ccy, mic, instType) {

	var infoRow = $(infoRowSelector);
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

	var rowID = cleanID(symbol);
	return $("#" + rowID);
}

function cleanID(symbol) {
	return symbol.replace(/ |\/|\.|:/g, "_");
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

	var familyID = "family_" + cleanID(familyName);
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
				var children = family.find(".children .row:not(.header)");
				if (children.length) {
					var configs = "";
					children.each(function () {
						configs = configs + $(this).attr("id").replace("_", " ") + ",";
					});
					configs = configs.substr(0, configs.length - 1);
					popUp("/stackConfig#;" + configs, "Configs", 2200, 400);
				}
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

function stackEnableFilteredStackChange(filters, side, stack) {
	return function (event) {
		event.preventDefault();
		switch (event.which) {
			case 1:
			{
				ws.send(command("setFilteredStackEnabled", [filters.text(), side, stack, true]));
				break;
			}
			case 3:
			{
				ws.send(command("setFilteredStackEnabled", [filters.text(), side, stack, false]));
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

function stackEnableFilteredAllStackChange(filters) {
	return function (event) {
		event.preventDefault();
		switch (event.which) {
			case 1:
			{
				ws.send(command("setFilteredAllStacksEnabled", [filters.text(), true]));
				break;
			}
			case 3:
			{
				ws.send(command("setFilteredAllStacksEnabled", [filters.text(), false]));
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

function globalWidthControls(instruction) {
	return function (event) {
		event.preventDefault();
		switch (event.which) {
			case 1:
			{
				ws.send(command(instruction, [1]));
				break;
			}
			case 3:
			{
				ws.send(command(instruction, [5]));
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

function removeChild(familyName, childSymbol) {

	var rowID = cleanID(childSymbol);
	var row = $("#" + rowID);

	var family = row.parent().parent();
	row.remove();
	setActiveChildCounts(family);
	setChildCount(familyName);
}

function setChild(familyName, childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier, familyToChildRatio) {

	var rowID = cleanID(childSymbol);
	var row = $("#" + rowID);

	var exchangeTable = addFamily(familyName).find(".children");
	var quoteSymbolCell;
	if (row.length < 1) {

		row = exchangeTable.find(".header").clone();
		row.removeClass("header");
		row.attr("id", rowID);
		row.addClass("dataRow");
		row.toggleClass("unregistered", true);

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

	var familyToChildRatioDiv = addNumberBox(row, ".familyToChildRatio");

	setDoubleData(bidPriceOffsetDiv, bidPriceOffset);
	setDoubleData(bidQtyMultiplierDiv, bidQtyMultiplier);
	setDoubleData(askPriceOffsetDiv, askPriceOffset);
	setDoubleData(askQtyMultiplierDiv, askQtyMultiplier);

	setDoubleData(familyToChildRatioDiv, familyToChildRatio);

	var submitButton = row.find("input[type=Submit]");
	submitButton.removeClass("hidden");
	submitButton.unbind().bind("click", function () {

		var bidOffset = bidPriceOffsetDiv.val();
		var bidMultiplier = bidQtyMultiplierDiv.val();

		var askOffset = askPriceOffsetDiv.val();
		var askMultiplier = askQtyMultiplierDiv.val();

		var ratio = familyToChildRatioDiv.val();

		ws.send(command("setRelationship", [childSymbol, bidOffset, bidMultiplier, askOffset, askMultiplier, ratio]));
	});

	var orphanButton = row.find(".orphanButton");
	orphanButton.removeClass("hidden");
	orphanButton.unbind().bind("click", function () {
		ws.send(command("orphanChild", [childSymbol]));
	});

	var killSymbolButton = row.find(".killSymbolButton");
	killSymbolButton.removeClass("hidden");
	killSymbolButton.unbind().bind("click", function () {
		ws.send(command("killChild", [childSymbol]));
	});

	var childControls = row.find(".childControls");

	var defaultConfigButton = childControls.find(".default");
	defaultConfigButton.unbind().bind("click", function () {
		ws.send(command("setChildSelectedConfig", [familyName, childSymbol, "DEFAULT"]));
	});

	var wideConfigButton = childControls.find(".wide");
	wideConfigButton.unbind().bind("click", function () {
		ws.send(command("setChildSelectedConfig", [familyName, childSymbol, "WIDE"]));
	});

	var obligationConfigButton = childControls.find(".obligation");
	obligationConfigButton.unbind().bind("click", function () {
		ws.send(command("setChildSelectedConfig", [familyName, childSymbol, "OBLIGATION"]));
	});

	var openConfigWindowDiv = childControls.find(".configWindow");
	openConfigWindowDiv.unbind().bind("click", function () {
		popUp("/stackConfig#;" + childSymbol, "Configs", 2200, 400);
	});

	var bidControls = childControls.find(".bid");
	var askControls = childControls.find(".ask");

	var bidPicardButton = bidControls.find(".picardEnabled");
	var bidQuoterButton = bidControls.find(".quoterEnabled");
	var askQuoterButton = askControls.find(".quoterEnabled");
	var askPicardButton = askControls.find(".picardEnabled");
	bidPicardButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "BID", "PICARD"));
	bidQuoterButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "BID", "QUOTER"));
	askQuoterButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "ASK", "QUOTER"));
	askPicardButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "ASK", "PICARD"));

	var bidStartConfigWindowDiv = bidControls.find(".start");
	bidStartConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("startChild", [familyName, childSymbol, "BID"]));
	});

	var bidStopConfigWindowDiv = bidControls.find(".stop");
	bidStopConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("stopChild", [familyName, childSymbol, "BID"]));
	});

	var askStartConfigWindowDiv = askControls.find(".start");
	askStartConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("startChild", [familyName, childSymbol, "ASK"]));
	});

	var askStopConfigWindowDiv = askControls.find(".stop");
	askStopConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("stopChild", [familyName, childSymbol, "ASK"]));
	});

	setChildCount(familyName);
}

function setChildData(childSymbol, leanSymbol, nibblerName, selectedConfigType, isBidStrategyOn, bidInfo, bidPicardEnabled, bidQuoterEnabled, isAskStrategyOn,
					  askInfo, askPicardEnabled, askQuoterEnabled) {

	var rowID = cleanID(childSymbol);
	var row = $("#" + rowID);

	if (row) {

		row.removeClass("unregistered");
		row.find(".leanSymbol").text(leanSymbol);

		var childControls = row.find(".childControls");
		childControls.find(".configControls button").removeClass("enabled");
		childControls.find(".configControls ." + selectedConfigType.toLowerCase()).addClass("enabled");
		childControls.find(".nibblerName").text(nibblerName);

		var bidControls = childControls.find(".bid");
		bidControls.filter(".runningControls").toggleClass("enabled", isBidStrategyOn);
		bidControls.find(".picardEnabled").toggleClass("enabled", bidPicardEnabled);
		bidControls.find(".quoterEnabled").toggleClass("enabled", bidQuoterEnabled);
		bidControls.filter(".strategyInfo").text(bidInfo);

		var askControls = childControls.find(".ask");
		askControls.filter(".runningControls").toggleClass("enabled", isAskStrategyOn);
		askControls.find(".picardEnabled").toggleClass("enabled", askPicardEnabled);
		askControls.find(".quoterEnabled").toggleClass("enabled", askQuoterEnabled);
		askControls.filter(".strategyInfo").text(askInfo);

		var family = row.parent().parent();
		setActiveChildCounts(family);
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

	var family = addFamily(familyName);
	var orderCountDiv = family.find(".childCount");
	var orders = family.find(".children .row").length - 1;
	orderCountDiv.text(orders);
}

function setActiveChildCounts(family) {

	var childControls = family.find(".childControls");

	var runningControls = childControls.find(".runningControls");
	var bidControls = runningControls.filter(".bid");
	var onBids = bidControls.filter(".enabled").length;
	var offBids = bidControls.length - onBids - 1;

	var askControls = runningControls.filter(".ask");
	var onAsks = askControls.filter(".enabled").length;
	var offAsks = askControls.length - onAsks - 1;

	var familyDetails = family.find(".activeCount");
	var bidCounts = familyDetails.filter(".bid");
	bidCounts.find(".on").text(onBids);
	bidCounts.find(".off").text(offBids);
	bidCounts.toggleClass("somethingOff", 0 < offBids);

	var askCounts = familyDetails.filter(".ask");
	askCounts.find(".on").text(onAsks);
	askCounts.find(".off").text(offAsks);
	askCounts.toggleClass("somethingOff", 0 < offAsks);
}

function displayErrorMsg(text) {

	var errorDiv = $("#errorMsg");
	errorDiv.text(text);
	console.log("ERROR", text, errorDiv);
	setTimeout(hideErrorMsg, 5000);
}

function hideErrorMsg() {
	var errorDiv = $("#errorMsg");
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