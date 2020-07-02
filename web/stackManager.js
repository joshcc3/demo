let ws;

let saveSound;
let loadSound;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (m) {
		eval(m);
	};

	const adminBlock = $("#adminBlock");
	const hash = document.location.hash.substr(1);
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

	const saveOffsetButton = $("#saveOffsets");
	saveOffsetButton.unbind().bind("click", function () {

		if (!saveOffsetButton.hasClass("locked")) {
			ws.send(command("saveOffsets", []));
		}
	});
	const loadOffsetButton = $("#loadOffsets");
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

	const globalBidPicardDiv = $("#globalBidPicard");
	globalBidPicardDiv.mousedown(globalStackEnableStackChange("BID", "PICARD"));

	const globalBidQuoterDiv = $("#globalBidQuote");
	globalBidQuoterDiv.mousedown(globalStackEnableStackChange("BID", "QUOTER"));

	const globalAskQuoterDiv = $("#globalAskQuote");
	globalAskQuoterDiv.mousedown(globalStackEnableStackChange("ASK", "QUOTER"));

	const globalAskPicardDiv = $("#globalAskPicard");
	globalAskPicardDiv.mousedown(globalStackEnableStackChange("ASK", "PICARD"));

	const footer = $("#footer");
	const closeFooter = footer.find("#closeFooter");
	closeFooter.unbind().bind("click", function () {
		footer.toggleClass("hidden", true);
	});
	const clearFilters = footer.find("#clearFilters");
	clearFilters.unbind().bind("click", function () {
		$("#filterList").find(".filterName.selected").toggleClass("selected", false);
		updateSelectedFilters();
	});

	const filteringRow = $("#filteringBlock");
	const filterSelection = $("#filterButton");
	filterSelection.unbind().bind("click", function () {
		footer.toggleClass("hidden", !footer.hasClass("hidden"));
	});

	const filteredBidStartButton = filteringRow.find(".bid.runningControls .start");
	filteredBidStartButton.unbind().bind("click", function () {
		ws.send(command("startFiltered", [filterSelection.text(), "BID"]));
	});
	const filteredBidStopButton = filteringRow.find(".bid.runningControls .stop");
	filteredBidStopButton.unbind().bind("click", function () {
		ws.send(command("stopFiltered", [filterSelection.text(), "BID"]));
	});

	const filteredAskStartButton = filteringRow.find(".ask.runningControls .start");
	filteredAskStartButton.unbind().bind("click", function () {
		ws.send(command("startFiltered", [filterSelection.text(), "ASK"]));
	});
	const filteredAskStopButton = filteringRow.find(".ask.runningControls .stop");
	filteredAskStopButton.unbind().bind("click", function () {
		ws.send(command("stopFiltered", [filterSelection.text(), "ASK"]));
	});

	const defaultConfigButton = filteringRow.find(".configControls .default");
	defaultConfigButton.unbind().bind("click", function () {
		ws.send(command("setFilteredSelectedConfig", [filterSelection.text(), "DEFAULT"]));
	});
	const wideConfigButton = filteringRow.find(".configControls .wide");
	wideConfigButton.unbind().bind("click", function () {
		ws.send(command("setFilteredSelectedConfig", [filterSelection.text(), "WIDE"]));
	});
	const obligationConfigButton = filteringRow.find(".configControls .obligation");
	obligationConfigButton.unbind().bind("click", function () {
		ws.send(command("setFilteredSelectedConfig", [filterSelection.text(), "OBLIGATION"]));
	});
	const configFilterButton = filteringRow.find(".configControls .configWindow");
	configFilterButton.unbind().bind("click", function () {
		ws.send(command("lookupConfigSymbols", [filterSelection.text()]));
	});

	const bidPicardDiv = filteringRow.find(".bid .picardEnabled");
	bidPicardDiv.mousedown(stackEnableFilteredStackChange(filterSelection, "BID", "PICARD"));

	const bidQuoterDiv = filteringRow.find(".bid .quoterEnabled");
	bidQuoterDiv.mousedown(stackEnableFilteredStackChange(filterSelection, "BID", "QUOTER"));

	const askQuoterDiv = filteringRow.find(".ask .quoterEnabled");
	askQuoterDiv.mousedown(stackEnableFilteredStackChange(filterSelection, "ASK", "QUOTER"));

	const askPicardDiv = filteringRow.find(".ask .picardEnabled");
	askPicardDiv.mousedown(stackEnableFilteredStackChange(filterSelection, "ASK", "PICARD"));

	const allEnableDiv = filteringRow.find(".stackControls.allEnabled");
	allEnableDiv.mousedown(stackEnableFilteredAllStackChange(filterSelection));

	const childSymbolSearchInput = $("#symbolLookup");
	childSymbolSearchInput.unbind("input").bind("input", function () {
		const symbol = childSymbolSearchInput.val();
		const child = findChild(symbol);
		childSymbolSearchInput.toggleClass("childAvailable", 1 == child.length);
	});

	childSymbolSearchInput.bind("keypress", function (e) {
		if (e.keyCode == 13) {
			const symbol = childSymbolSearchInput.val();
			showChild(symbol);
		}
	});
	$("#symbolLookupGo").unbind().bind("click", function () {
		const symbol = childSymbolSearchInput.val();
		showChild(symbol);
	});

	$("#hideUnregistered").unbind().bind("click", function (cb) {
		adminBlock.toggleClass("hideUnregistered", cb.toElement.checked);
	});

	const familyIsinInput = $("#familySymbolLookup");
	const isADRCheckBox = $("#isADR");
	const isADRCheck = isADRCheckBox[0];
	const familyNameFeedback = $("#foundFamilyName");
	familyIsinInput.on("input", function () {
		const symbol = familyIsinInput.val();
		ws.send(command("checkFamilyInst", [symbol, "#creationInfoRow"]));
		familyNameFeedback.text("");
	});
	isADRCheckBox.change(function () {
		familyNameFeedback.text("");
	});
	$("#findFamilyMembers").unbind().bind("click", function () {
		const symbol = familyIsinInput.val();
		const isADR = isADRCheck.checked;
		$(".childCreationRow:not(.headerRow)").remove();
		ws.send(command("findFamilyMembers", [symbol, isADR]));
	});

	const familyNameInput = $("#quoteSymbol");
	familyNameInput.on("input", function () {
		familyNameFeedback.text("");
	});
	const isAsylumCheck = $("#isAsylum")[0];
	$("#createFamily").unbind().bind("click", function () {
		const symbol = familyNameInput.val();
		const isAsylum = isAsylumCheck.checked;
		const isADR = isADRCheck.checked;
		ws.send(command("createFamily", [symbol, isAsylum, isADR]));
	});
	$("#createAllChildren").unbind().bind("click", function () {

		const childCreationRows = $("#createChildrenTable").find(".childCreationRow:not(.headerRow)");
		childCreationRows.each(function () {
			const childCreationRow = $(this);
			if (!childCreationRow.find(".hostNibblers").hasClass("notPersisted")) {
				createStackForChildRow(childCreationRow);
			}
		});
	});
	$("#adoptAllChildren").unbind().bind("click", function () {

		const childCreationRows = $("#createChildrenTable").find(".childCreationRow:not(.headerRow)");
		childCreationRows.each(function () {
			const childCreationRow = $(this);
			if (!childCreationRow.find(".hostNibblers").hasClass("notPersisted")) {
				adoptStackForChildRow(childCreationRow);
			}
		});
	});

	$("#resetOffsets").unbind().bind("click", function () {
		const symbol = familyNameInput.val();
		const isADR = isADRCheck.checked;
		ws.send(command("resetOffsetsForFamily", [symbol, isADR]));
	});

	const familyInput = $("#familySymbol");
	familyInput.on("input", function () {
		const family = familyInput.val();
		ws.send(command("checkFamilyExists", [family, "#adoptionInfoRow .familyFound"]));
	});
	const childInput = $("#childSymbol");
	childInput.on("input", function () {
		const child = childInput.val();
		ws.send(command("checkChildExists", [child, "#adoptionInfoRow .childFound"]));
	});
	$("#adoptChild").unbind().bind("click", function () {
		const family = familyInput.val();
		const child = childInput.val();
		const isADR = isADRCheck.checked;
		ws.send(command("adoptChild", [family, child, isADR]));
	});

	$("#createMissingChildren").unbind().bind("click", function () {
		ws.send(command("createMissingChildren"));
	});
	$("#correctChildAdoptions").unbind().bind("click", function () {
		ws.send(command("correctAdoptionsForChildren"));
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
	$("#killExpiringFutures").unbind().bind("click", function () {
		ws.send(command("killExpiringFutures"));
	});
	$("#createAllRFQ").unbind().bind("click", function () {
		ws.send(command("createAllRFQ"));
	});
	$("#adoptAllRFQ").unbind().bind("click", function () {
		ws.send(command("adoptAllRFQ"));
	});

	const copyFromSymbolInput = $("#copyFromSymbol");
	copyFromSymbolInput.on("input", function () {
		const symbol = copyFromSymbolInput.val();
		ws.send(command("checkFamilyInst", [symbol, ".stackCopyFeedback.fromFound"]));
	});
	const copyToSymbolInput = $("#copyToSymbol");
	copyToSymbolInput.on("input", function () {
		const symbol = copyToSymbolInput.val();
		ws.send(command("checkFamilyInst", [symbol, ".stackCopyFeedback.toFound"]));
	});
	$("#copyStackData").unbind().bind("click", function () {
		const fromSymbol = copyFromSymbolInput.val();
		const toSymbol = copyToSymbolInput.val();
		ws.send(command("copyChildSetup", [fromSymbol, toSymbol]));
	});

	$("#header").unbind("dblclick").bind("dblclick", showAdmin);
	footer.unbind("dblclick").bind("dblclick", function (event) {
		event.preventDefault();
	});

	saveSound = new Audio("stacksSounds/saveOffsets.wav");
	loadSound = new Audio("stacksSounds/loadOffsets.wav");
});

function updateOffsets(familySymbol, bpsWider, skipNonDefaults) {
	ws.send(command("updateOffsets", [familySymbol, bpsWider, skipNonDefaults]));
}

function killFamily(symbol) {
	ws.send(command("killFamily", [symbol]))
}

function popUp(url, name, width, height) {

	window.open(url, name, 'width=' + width + ',height=' + height);
	window.focus();
}

function setGlobalOffset(globalOffset) {
	$("#globalPriceOffset").text(globalOffset);
}

function setGlobalStackEnabled(isBidPicardEnabled, isBidQuoterEnabled, isAskQuoterEnabled, isAskPicardEnabled) {
	$("#globalBidPicard").toggleClass("enabled", isBidPicardEnabled);
	$("#globalBidQuote").toggleClass("enabled", isBidQuoterEnabled);
	$("#globalAskQuote").toggleClass("enabled", isAskQuoterEnabled);
	$("#globalAskPicard").toggleClass("enabled", isAskPicardEnabled);
}

function setFilters(filters) {

	const filterList = $("#filterList");
	filterList.find(".filterGroup:not(.template)").remove();

	const templateFilter = filterList.find(".filterGroup.template");

	for (let filterName in filters) {

		if (filters.hasOwnProperty(filterName)) {

			const filterGroupName = filters[filterName];

			const filterGroupID = cleanID("FILTER_GROUP_" + filterGroupName);
			let filterGroup = findChild(filterGroupID);

			if (filterGroup.length < 1) {
				filterGroup = templateFilter.clone();
				filterGroup.attr("id", filterGroupID);
				filterGroup.removeClass("template");
				filterGroup.find(".filterGroupName").text(filterGroupName);

				addSortedDiv(filterList.find(".filterGroup"), filterGroup, filterComparator);
			}

			const filterNameCell = filterGroup.find(".template.filterName").clone();

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

	const filterList = $("#filterList");
	const selectedFilters = filterList.find(".filterName.selected");

	const filterButton = $("#filterButton");

	if (selectedFilters.length < 1) {
		filterButton.text("None");
	} else {
		let selectedFilterNames = "";
		selectedFilters.each(function () {
			selectedFilterNames = selectedFilterNames + " | " + $(this).text();
		});
		selectedFilterNames = selectedFilterNames.substr(3, selectedFilterNames.length - 3);
		filterButton.text(selectedFilterNames);
	}
}

function filterComparator(a, b) {

	const aSymbol = a.find(".orderingName").text();
	const bSymbol = b.find(".orderingName").text();

	return aSymbol < bSymbol ? -1 : aSymbol == bSymbol ? 0 : 1;
}

function clearFieldData(fieldID) {

	const infoRow = $(fieldID);
	infoRow.toggleClass("unknown", true);
	infoRow.find("div").text("");
}

function setInstID(infoRowSelector, isin, ccy, mic, instType) {

	const infoRow = $(infoRowSelector);
	infoRow.toggleClass("unknown", false);
	infoRow.find(".isin").text(isin);
	infoRow.find(".ccy").text(ccy);
	infoRow.find(".mic").text(mic);
	infoRow.find(".instType").text(instType);
}

function setFieldData(fieldID, text) {

	const infoRow = $(fieldID);
	infoRow.toggleClass("unknown", false);
	infoRow.find("div").text(text);
}

function setCreateFamilyRow(symbol, isFamilyExists, familyName) {

	const familyNameInput = $("#quoteSymbol");
	familyNameInput.val(symbol);
	familyNameInput.toggleClass("childAvailable", isFamilyExists);

	const familyNameFeedback = $("#foundFamilyName");
	familyNameFeedback.text(familyName);
}

function addCreateChildRow(childSymbol, isChildAlreadyCreated, nibblers, tradableNibbler, instTypes, leanInstType, leanSymbol) {

	const childTable = $("#createChildrenTable");
	const childCreationRow = $(".childCreationRow.headerRow").clone();

	childCreationRow.removeClass("headerRow");

	const childQuoteSymbolInput = childCreationRow.find(".childQuoteSymbol");
	childQuoteSymbolInput.val(childSymbol);
	childQuoteSymbolInput.toggleClass("childAvailable", isChildAlreadyCreated);

	const nibblersCombo = childCreationRow.find(".hostNibblers");
	nibblersCombo.removeAttr('id');
	nibblers.forEach(function (nibbler) {

		const option = $("<option value=\"" + nibbler + "\">" + nibbler + "</option>");
		option.addClass(nibbler);
		option.attr("data", nibbler);
		nibblersCombo.append(option);
	});
	nibblersCombo.toggleClass("notPersisted", true);
	nibblersCombo.off("focus").focus(function () {
		nibblersCombo.toggleClass("notPersisted", false);
	});

	if (tradableNibbler) {
		nibblersCombo.val(tradableNibbler);
		nibblersCombo.change();
		nibblersCombo.toggleClass("notPersisted", false);
	}

	const instTypeCombo = childCreationRow.find(".leanInstID");
	instTypes.forEach(function (instType) {

		const option = $("<option value=\"" + instType + "\">" + instType + "</option>");
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
		createStackForChildRow(childCreationRow);
	});

	childCreationRow.find("button.adoptButton").unbind().bind("click", function () {
		adoptStackForChildRow(childCreationRow);
	});

	addSortedDiv(childTable.find(".row"), childCreationRow, childCreationComparator);
}

function createStackForChildRow(childCreationRow) {

	const quoteSymbol = childCreationRow.find("input[name=quote]").val();
	const forNibbler = childCreationRow.find(".hostNibblers").find("option:selected").text();
	const leanInstType = childCreationRow.find(".leanInstID").find("option:selected").text();
	const leanSymbol = childCreationRow.find("input[name=lean]").val();
	const additiveSymbol = childCreationRow.find("input[name=additive]").val();
	ws.send(command("createChildStack", [forNibbler, quoteSymbol, leanInstType, leanSymbol, additiveSymbol]));
}

function adoptStackForChildRow(childCreationRow) {

	const family = $("#quoteSymbol").val();
	const child = childCreationRow.find("input[name=quote]").val();
	const isADRCheck = $("#isADR")[0];
	const isADR = isADRCheck.checked;
	ws.send(command("adoptChild", [family, child, isADR]));
}

function childCreationComparator(a, b) {

	const aSymbol = a.find(".childQuoteSymbol").val();
	const bSymbol = b.find(".childQuoteSymbol").val();

	return aSymbol < bSymbol ? -1 : aSymbol == bSymbol ? 0 : 1;
}

function removeAll(nibblerName) {
	const table = getExchangeTable(nibblerName);
	table.find(".dataRow").remove();
	setOrderCount(nibblerName);
}

function findChild(symbol) {

	const rowID = cleanID(symbol);
	return $("#" + rowID);
}

function cleanID(symbol) {
	return symbol.replace(/ |\/|\.|:|\(|\)/g, "_");
}

function showChild(symbol) {
	const child = findChild(symbol);
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

function addFamily(familyName, isAsylum) {

	const familyID = "family_" + cleanID(familyName);
	let family = findChild(familyID);
	if (family.length < 1) {

		family = $("#templateFamily").clone();
		family.attr("id", familyID);
		family.removeClass("template");

		family.toggleClass("isAsylum", isAsylum);

		const familyDetails = family.find(".familyDetails");
		const familyBlock = family.find(".children");
		familyDetails.find(".openStack").unbind().bind("click", function () {
			launchLadder(familyName + ";S");
		});
		familyDetails.find(".refreshParent").unbind().bind("click", function () {
			ws.send(command("refreshParent", [familyName]));
		});

		const bidStartButton = family.find(".familyDetails .bid .start");
		bidStartButton.unbind().bind("click", function () {
			ws.send(command("startFamily", [familyName, "BID"]));
		});

		const bidStopButton = family.find(".familyDetails .bid .stop");
		bidStopButton.unbind().bind("click", function () {
			ws.send(command("stopFamily", [familyName, "BID"]));
		});

		const askStartButton = family.find(".familyDetails .ask .start");
		askStartButton.unbind().bind("click", function () {
			ws.send(command("startFamily", [familyName, "ASK"]));
		});

		const askStopButton = family.find(".familyDetails .ask .stop");
		askStopButton.unbind().bind("click", function () {
			ws.send(command("stopFamily", [familyName, "ASK"]));
		});

		const familyNameDiv = family.find(".familyName");
		familyNameDiv.text(familyName);
		familyNameDiv.unbind().bind("click", function () {
			familyBlock.toggleClass("hidden", !familyBlock.hasClass("hidden"));
		});

		const bidOffsetUpButton = family.find(".bid .priceOffsetUp");
		bidOffsetUpButton.mousedown(priceOffsetChange("increaseOffset", familyName, "BID", 1));

		const bidOffsetDownButton = family.find(".bid .priceOffsetDown");
		bidOffsetDownButton.mousedown(priceOffsetChange("increaseOffset", familyName, "BID", -1));

		const askOffsetUpButton = family.find(".ask .priceOffsetUp");
		askOffsetUpButton.mousedown(priceOffsetChange("increaseOffset", familyName, "ASK", 1));

		const askOffsetDownButton = family.find(".ask .priceOffsetDown");
		askOffsetDownButton.mousedown(priceOffsetChange("increaseOffset", familyName, "ASK", -1));

		const selectDefaultConfigDiv = family.find(".configControls .default");
		selectDefaultConfigDiv.unbind().bind("click", function () {
			ws.send(command("selectConfig", [familyName, "DEFAULT"]));
		});

		const selectWideConfigDiv = family.find(".configControls .wide");
		selectWideConfigDiv.unbind().bind("click", function () {
			ws.send(command("selectConfig", [familyName, "WIDE"]));
		});

		const selectObligationConfigDiv = family.find(".configControls .obligation");
		selectObligationConfigDiv.unbind().bind("click", function () {
			ws.send(command("selectConfig", [familyName, "OBLIGATION"]));
		});

		const openConfigWindowDiv = family.find(".configControls .configWindow");
		openConfigWindowDiv.unbind().bind("click", function () {
			let configs = familyName;
			family.find(".children .row:not(.header)").each(function () {
				configs = configs + "," + $(this).attr("id").replace("_", " ");
			});
			popUp("/stackConfig#;" + configs, "Configs", 2200, 400);
		});

		openConfigWindowDiv.mousedown(function (e) {
			if (e.button == 2) {
				const children = family.find(".children .row:not(.header)");
				if (children.length) {
					let configs = "";
					children.each(function () {
						configs = configs + $(this).attr("id").replace("_", " ") + ",";
					});
					configs = configs.substr(0, configs.length - 1);
					popUp("/stackConfig#;" + configs, "Configs", 2200, 400);
				}
			}
		});

		const bidPicardDiv = family.find(".bid .picardEnabled");
		bidPicardDiv.mousedown(stackEnableStackChange(familyName, "BID", "PICARD"));

		const bidQuoterDiv = family.find(".bid .quoterEnabled");
		bidQuoterDiv.mousedown(stackEnableStackChange(familyName, "BID", "QUOTER"));

		const askQuoterDiv = family.find(".ask .quoterEnabled");
		askQuoterDiv.mousedown(stackEnableStackChange(familyName, "ASK", "QUOTER"));

		const askPicardDiv = family.find(".ask .picardEnabled");
		askPicardDiv.mousedown(stackEnableStackChange(familyName, "ASK", "PICARD"));

		const allEnableDiv = family.find(".stackControls.allEnabled");
		allEnableDiv.mousedown(stackEnableAllStackChange(familyName));

		const cleanFamilyDiv = family.find(".stackControls.cleanParent");
		cleanFamilyDiv.mousedown(function () {
			ws.send(command("cleanParent", [familyName]));
		});

		addSortedDiv($("#families").find(".family"), family, tableComparator);

		setChildCount(familyName);
	}
	return family;
}

function tableComparator(a, b) {

	const aIsAsylum = a.hasClass("isAsylum");
	const bIsAsylum = b.hasClass("isAsylum");

	if (aIsAsylum === bIsAsylum) {

		const aSymbol = a.find(".familyDetails .familyName").text();
		const bSymbol = b.find(".familyDetails .familyName").text();

		return aSymbol < bSymbol ? -1 : aSymbol === bSymbol ? 0 : 1;

	} else if (aIsAsylum){
		return -1;
	} else {
		return 1;
	}
}

function globalStackEnableStackChange(side, stack) {
	return function (event) {
		event.preventDefault();
		switch (event.which) {
			case 1: {
				ws.send(command("globalStackEnabled", [side, stack, true]));
				break;
			}
			case 3: {
				ws.send(command("globalStackEnabled", [side, stack, false]));
				break;
			}
		}
	}
}

function priceOffsetChange(cmd, familyName, side, direction) {
	return function (event) {
		event.preventDefault();
		switch (event.which) {
			case 1: {
				ws.send(command(cmd, [familyName, side, direction]));
				break;
			}
			case 3: {
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
			case 1: {
				ws.send(command("setFilteredStackEnabled", [filters.text(), side, stack, true]));
				break;
			}
			case 3: {
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
			case 1: {
				ws.send(command("setStackEnabled", [familyName, side, stack, true]));
				break;
			}
			case 3: {
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
			case 1: {
				ws.send(command("setChildStackEnabled", [familyName, childSymbol, side, stack, true]));
				break;
			}
			case 3: {
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
			case 1: {
				ws.send(command("setFilteredAllStacksEnabled", [filters.text(), true]));
				break;
			}
			case 3: {
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
			case 1: {
				ws.send(command("setAllStacksEnabled", [familyName, true]));
				break;
			}
			case 3: {
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
			case 1: {
				ws.send(command(instruction, [1]));
				break;
			}
			case 3: {
				ws.send(command(instruction, [5]));
				break;
			}
		}
	}
}

function setParentData(familyName, bidPriceOffset, askPriceOffset, selectedConfigType, bidPicardEnabled, bidQuoterEnabled, askPicardEnabled,
					   askQuoterEnabled) {

	const family = addFamily(familyName);

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

	const rowID = cleanID(childSymbol);
	const row = $("#" + rowID);

	const family = row.parent().parent();
	row.remove();
	setActiveChildCounts(family);
	setChildCount(familyName);
}

function setChild(familyName, childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier, familyToChildRatio) {

	const rowID = cleanID(childSymbol);
	let row = $("#" + rowID);

	const exchangeTable = addFamily(familyName).find(".children");
	let quoteSymbolCell;
	if (row.length < 1) {

		row = exchangeTable.find(".header").clone();
		row.removeClass("header");
		row.attr("id", rowID);
		row.addClass("dataRow");
		row.toggleClass("unregistered", true);

		quoteSymbolCell = setCellData(row.find(".symbol"), childSymbol);

		addSortedDiv(exchangeTable.find(".row"), row, rowComparator);

	} else if (row.parent().get(0) != exchangeTable.get(0)) {

		const oldFamily = row.parent().parent().find(".familyName").text();
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

	const bidPriceOffsetDiv = addNumberBox(row, ".bid.priceOffset");
	const bidQtyMultiplierDiv = addNumberBox(row, ".bid.qtyMultiplier");

	const askPriceOffsetDiv = addNumberBox(row, ".ask.priceOffset");
	const askQtyMultiplierDiv = addNumberBox(row, ".ask.qtyMultiplier");

	const familyToChildRatioDiv = addNumberBox(row, ".familyToChildRatio");

	setDoubleData(bidPriceOffsetDiv, bidPriceOffset);
	setDoubleData(bidQtyMultiplierDiv, bidQtyMultiplier);
	setDoubleData(askPriceOffsetDiv, askPriceOffset);
	setDoubleData(askQtyMultiplierDiv, askQtyMultiplier);

	setDoubleData(familyToChildRatioDiv, familyToChildRatio);

	const submitButton = row.find("input[type=Submit]");
	submitButton.removeClass("hidden");
	submitButton.unbind().bind("click", function () {

		const bidOffset = bidPriceOffsetDiv.val();
		const bidMultiplier = bidQtyMultiplierDiv.val();

		const askOffset = askPriceOffsetDiv.val();
		const askMultiplier = askQtyMultiplierDiv.val();

		const ratio = familyToChildRatioDiv.val();

		ws.send(command("setRelationship", [childSymbol, bidOffset, bidMultiplier, askOffset, askMultiplier, ratio]));
	});

	const orphanButton = row.find(".orphanButton");
	orphanButton.removeClass("hidden");
	orphanButton.unbind().bind("click", function () {
		ws.send(command("orphanChild", [childSymbol]));
	});

	const killSymbolButton = row.find(".killSymbolButton");
	killSymbolButton.removeClass("hidden");
	killSymbolButton.unbind().bind("click", function () {
		ws.send(command("killChild", [childSymbol]));
	});

	const childControls = row.find(".childControls");

	const defaultConfigButton = childControls.find(".default");
	defaultConfigButton.unbind().bind("click", function () {
		ws.send(command("setChildSelectedConfig", [familyName, childSymbol, "DEFAULT"]));
	});

	const wideConfigButton = childControls.find(".wide");
	wideConfigButton.unbind().bind("click", function () {
		ws.send(command("setChildSelectedConfig", [familyName, childSymbol, "WIDE"]));
	});

	const obligationConfigButton = childControls.find(".obligation");
	obligationConfigButton.unbind().bind("click", function () {
		ws.send(command("setChildSelectedConfig", [familyName, childSymbol, "OBLIGATION"]));
	});

	const openConfigWindowDiv = childControls.find(".configWindow");
	openConfigWindowDiv.unbind().bind("click", function () {
		popUp("/stackConfig#;" + childSymbol, "Configs", 2200, 400);
	});

	const bidControls = childControls.find(".bid");
	const askControls = childControls.find(".ask");

	const bidPicardButton = bidControls.find(".picardEnabled");
	const bidQuoterButton = bidControls.find(".quoterEnabled");
	const askQuoterButton = askControls.find(".quoterEnabled");
	const askPicardButton = askControls.find(".picardEnabled");
	bidPicardButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "BID", "PICARD"));
	bidQuoterButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "BID", "QUOTER"));
	askQuoterButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "ASK", "QUOTER"));
	askPicardButton.mousedown(stackChildEnableStackChange(familyName, childSymbol, "ASK", "PICARD"));

	const bidStartConfigWindowDiv = bidControls.find(".start");
	bidStartConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("startChild", [familyName, childSymbol, "BID"]));
	});

	const bidStopConfigWindowDiv = bidControls.find(".stop");
	bidStopConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("stopChild", [familyName, childSymbol, "BID"]));
	});

	const askStartConfigWindowDiv = askControls.find(".start");
	askStartConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("startChild", [familyName, childSymbol, "ASK"]));
	});

	const askStopConfigWindowDiv = askControls.find(".stop");
	askStopConfigWindowDiv.unbind().bind("click", function () {
		ws.send(command("stopChild", [familyName, childSymbol, "ASK"]));
	});

	setChildCount(familyName);
}

function setChildData(childSymbol, leanSymbol, nibblerName, selectedConfigType, isBidStrategyOn, bidInfo, bidPicardEnabled,
					  bidQuoterEnabled, isAskStrategyOn, askInfo, askPicardEnabled, askQuoterEnabled) {

	const rowID = cleanID(childSymbol);
	const row = $("#" + rowID);

	if (row) {

		row.removeClass("unregistered");

		const leanSymbolCell = row.find(".leanSymbol");
		leanSymbolCell.text(leanSymbol);
		leanSymbolCell.unbind().bind("click", function () {
			launchLadder(leanSymbol);
		});

		const childControls = row.find(".childControls");
		childControls.find(".configControls button").removeClass("enabled");
		childControls.find(".configControls ." + selectedConfigType.toLowerCase()).addClass("enabled");
		childControls.find(".nibblerName").text(nibblerName);

		const bidControls = childControls.find(".bid");
		bidControls.filter(".runningControls").toggleClass("enabled", isBidStrategyOn);
		bidControls.find(".picardEnabled").toggleClass("enabled", bidPicardEnabled);
		bidControls.find(".quoterEnabled").toggleClass("enabled", bidQuoterEnabled);
		bidControls.filter(".strategyInfo").text(bidInfo);
		childControls.toggleClass("bidChildRunning", isBidStrategyOn && (bidPicardEnabled || bidQuoterEnabled));

		const askControls = childControls.find(".ask");
		askControls.filter(".runningControls").toggleClass("enabled", isAskStrategyOn);
		askControls.find(".picardEnabled").toggleClass("enabled", askPicardEnabled);
		askControls.find(".quoterEnabled").toggleClass("enabled", askQuoterEnabled);
		askControls.filter(".strategyInfo").text(askInfo);
		childControls.toggleClass("askChildRunning", isAskStrategyOn && (askPicardEnabled || askQuoterEnabled));

		const family = row.parent().parent();
		setActiveChildCounts(family);
	}
}

function rowComparator(a, b) {

	const aSymbol = a.find(".symbol").text();
	const bSymbol = b.find(".symbol").text();

	return aSymbol < bSymbol ? -1 : aSymbol == bSymbol ? 0 : 1;
}

function addNumberBox(row, selector) {

	const div = row.find(selector);
	let input = div.find("input");

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

	const family = addFamily(familyName);
	const orderCountDiv = family.find(".childCount");
	const orders = family.find(".children .row").length - 1;
	orderCountDiv.text(orders);
}

function setActiveChildCounts(family) {

	const childControls = family.find(".childControls");

	const onBids = childControls.filter(".bidChildRunning").length;
	const offBids = childControls.length - onBids - 1;

	const onAsks = childControls.filter(".askChildRunning").length;
	const offAsks = childControls.length - onAsks - 1;

	const familyDetails = family.find(".activeCount");
	const bidCounts = familyDetails.filter(".bid");
	bidCounts.find(".on").text(onBids);
	bidCounts.find(".off").text(offBids);
	bidCounts.toggleClass("somethingOff", 0 < offBids);

	const askCounts = familyDetails.filter(".ask");
	askCounts.find(".on").text(onAsks);
	askCounts.find(".off").text(offAsks);
	askCounts.toggleClass("somethingOff", 0 < offAsks);
}

function displayErrorMsg(text) {

	const errorDiv = $("#errorMsg");
	errorDiv.text(text);
	console.log("ERROR", text, errorDiv);
	setTimeout(hideErrorMsg, 5000);
}

function hideErrorMsg() {
	const errorDiv = $("#errorMsg");
	errorDiv.text("");
}

function showAdmin(event) {
	const adminDiv = $("#adminBlock");
	if (event.ctrlKey) {
		adminDiv.toggleClass("hideAdmin", !adminDiv.hasClass("hideAdmin"));
	} else {
		adminDiv.toggleClass("hideAdmin", true);
	}
	window.getSelection().removeAllRanges();
}

function openConfig(symbolList) {
	popUp("/stackConfig#;" + symbolList, "Configs", 2200, 400);
}

function offsetsSaved() {
	playSound(saveSound);
}

function offsetsLoaded() {
	playSound(loadSound);
}

function playSound(sound) {

	if (!sound.readyState) {
		sound.load();
	}
	sound.play();
}
