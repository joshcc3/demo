// WARNING - the layout of the html is depended upon

let ws;

let saveSound;
let loadSound;

let isFutures;

let isLazy = false;
const searchedForSymbols = new Set()
const subscribedToSymbols = new Set()

const UI_VERSION_NUM = "1";
const DELIMITER = "\u0000";

const emojiSymbols = {
	"omega": "\u03A9",
	"smile": String.fromCodePoint(0x1F600),
	"smiley": String.fromCodePoint(0x1F600),
	"poop": String.fromCodePoint(0x1F4A9),
	"middle-finger": String.fromCodePoint(0x1F595),
	"cloud-sun": String.fromCodePoint(0x26C5),
	"money-mouth-face": String.fromCodePoint(0x1F911),
	"hundred-points": String.fromCodePoint(0x1F4AF),
	"australia": String.fromCodePoint(0x1F1FA),
	"brazil": String.fromCodePoint(0x1F1F7),
	"china": String.fromCodePoint(0x1F1F3),
	"germany": String.fromCodePoint(0x1F1EC),
	"spain": String.fromCodePoint(0x1F1F8),
	"european-union": String.fromCodePoint(0x1F1FA),
	"france": String.fromCodePoint(0x1F1F7),
	"united-kingdom": String.fromCodePoint(0x1F1E7),
	"hong-kong": String.fromCodePoint(0x1F1F0),
	"netherlands": String.fromCodePoint(0x1F1F1),
	"norway": String.fromCodePoint(0x1F1F4),
	"russia": String.fromCodePoint(0x1F1FA),
	"united-states": String.fromCodePoint(0x1F1F2)
}

let templateFamilyElement;
let familiesDiv;

$(function () {

	templateFamilyElement = $("#templateFamily");
	familiesDiv = $("#families");

	ws = connect();
	ws.logToConsole = true;
	ws.onmessage = function (m) {
		eval(m);
	};

	isFutures = document.location.href.includes("prod-futures-ladder.eeif.drw")

	const adminBlock = $("#adminBlock");
	const hash = document.location.hash.substr(1);

	isLazy = hash.endsWith(",Lazy");
	const suffix = isLazy ? ",Lazy" : "";

	let subscriptionString;
	if (hash.startsWith("Asylum")) {
		adminBlock.toggleClass("isAsylumView", true);
		subscriptionString = "subscribeNew" + hash.replace("Asylum", "Family");
	} else if (hash) {
		subscriptionString = "subscribeNew" + hash;
	} else {
		subscriptionString = "subscribeNewFamily,DEFAULT" + suffix;
	}

	ws.send(subscriptionString);

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
		childSymbolSearchInput.toggleClass("childAvailable", 1 === child.length);
		const isSearching = isLazy && 1 !== child.length && !searchedForSymbols.has(symbol);
		childSymbolSearchInput.toggleClass("childSearching", isSearching);
		searchedForSymbols.add(symbol)
	});

	childSymbolSearchInput.bind("keypress", function (e) {
		if (e.keyCode === 13) {
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

function createAllFamiliesFromFile() {
	ws.send(command("createFamiliesFromFile", []));
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

	return aSymbol < bSymbol ? -1 : aSymbol === bSymbol ? 0 : 1;
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

	return aSymbol < bSymbol ? -1 : aSymbol === bSymbol ? 0 : 1;
}

function removeAll(nibblerName) {
	const table = getExchangeTable(nibblerName);
	table.find(".dataRow").remove();
	setOrderCount(nibblerName);
}

function findChild(symbol) {

	const rowID = cleanID(symbol);
	const element = $("#" + rowID);
	if (isLazy && 1 > element.length && 3 <= symbol.length) {
		ws.send("lazySubscribe," + symbol);
	}

	return element;
}

function cleanID(symbol) {
	return symbol.replace(/ |\/|\.|:|\(|\)/g, "_");
}

function showChild(symbol) {
	const child = findChild(symbol);
	if (1 === child.length) {
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

function addFamily(familyName, isAsylum, _uiName) {

	const uiName = _uiName ? _uiName : familyName;
	const familyID = "family_" + cleanID(familyName);
	let family = findChild(familyID);
	if (family.length < 1 && (!isLazy || subscribedToSymbols.has(familyName))) {

		family = templateFamilyElement.clone();
		family.attr("id", familyID);
		family.removeClass("template");
		family.toggleClass("isAsylum", isAsylum);

		let childElems = family.children();

		const familyDetails = childElems[0];
		const familyBlock = childElems[1];

		let openStackButton = familyDetails.children[0];
		openStackButton.onclick = () => {
			launchLadder(familyName + ";S", undefined, isFutures);
		};
		let refreshParentButton = familyDetails.children[1];
		refreshParentButton.onclick = () => {
			ws.send(command("refreshParent", [familyName]));
		};

		const bidControlsDiv = familyDetails.children[4];
		const bidStartButton = bidControlsDiv.children[0];
		bidStartButton.onclick = () => {
			ws.send(command("startFamily", [familyName, "BID"]));
		};

		const bidStopButton = bidControlsDiv.children[1];
		bidStopButton.onclick = () => {
			ws.send(command("stopFamily", [familyName, "BID"]));
		};

		const askControlsDiv = familyDetails.children[5];
		const askStartButton = askControlsDiv.children[0];
		askStartButton.onclick = () => {
			ws.send(command("startFamily", [familyName, "ASK"]));
		};

		const askStopButton = askControlsDiv.children[1];
		askStopButton.onclick = () => {
			ws.send(command("stopFamily", [familyName, "ASK"]));
		};

		const familyNameDiv = familyDetails.children[7];
		familyNameDiv.innerText = familyName;

		let editable = false

		let lastUIName = familyName;

		const uiNameDiv = familyDetails.children[8];
		uiNameDiv.innerText = uiName;
		uiNameDiv.onclick = () => {
			if (!editable) {

				if (familyBlock.classList.contains("hidden")) {
					familyBlock.classList.remove("hidden");
				} else {
					familyBlock.classList.add("hidden");
				}
			}
		};

		uiNameDiv.onkeypress = function (e) {
			if (editable) {
				if (e.keyCode === 13) {
					const currentUIName = uiNameDiv.innerText;
					const regexMatch = /^[a-z0-9_][a-z0-9_-]+$/i;
					const lengthCheck = 3 <= currentUIName.length && 30 >= currentUIName.length;
					const charCheck = currentUIName.match(regexMatch) !== null;
					if (!lengthCheck) {
						displayErrorMsg("Family name must be between 3 and 10 characters long")
					} else if (!charCheck) {
						displayErrorMsg("Family name can only have the following characters [a-z0-9 _-]")
					} else {
						ws.send(`renameFamily,${familyName},${currentUIName}`)
						uiNameDiv.innerText = lastUIName;
						uiNameDiv.setAttribute('contenteditable', 'false');
						editable = false;
					}
					return false;
				}
			}
		};

		const uiNameEdit = familyDetails.children[9];
		uiNameEdit.onclick = () => {
			lastUIName = uiNameDiv.innerText
			uiNameDiv.setAttribute('contenteditable', 'true');
			editable = true;
		};

		let bidOffsetDiv = familyDetails.children[11];
		const bidOffsetUpButton = bidOffsetDiv.children[0]; //.find(".bid .priceOffsetUp");
		bidOffsetUpButton.onmousedown = priceOffsetChange("increaseOffset", familyName, "BID", 1);

		const bidOffsetDownButton = bidOffsetDiv.children[1];
		bidOffsetDownButton.onmousedown = priceOffsetChange("increaseOffset", familyName, "BID", -1);

		let askOffsetDiv = familyDetails.children[13];
		const askOffsetUpButton = askOffsetDiv.children[0]; // find(".ask .priceOffsetUp");
		askOffsetUpButton.onmousedown = priceOffsetChange("increaseOffset", familyName, "ASK", 1);

		const askOffsetDownButton = askOffsetDiv.children[1];
		askOffsetDownButton.onmousedown = priceOffsetChange("increaseOffset", familyName, "ASK", -1);

		const openConfigWindowDiv = familyDetails.children[14].children[0]; // family.find(".configControls .configWindow");
		openConfigWindowDiv.onclick = function () {
			let configs = familyName;
			family.find(".children .row:not(.header)").each(function () {
				configs = configs + "," + $(this).attr("id").replace("_", " ");
			});
			popUp("/stackConfig#;" + configs, "Configs", 2200, 400);
		};

		openConfigWindowDiv.onmousedown = function (e) {
			if (e.button === 2) {
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
		};

		let bidStackControlsDiv = familyDetails.children[15];
		const bidPicardDiv = bidStackControlsDiv.children[0];// find(".bid .picardEnabled");
		bidPicardDiv.onmousedown = stackEnableStackChange(familyName, "BID", "PICARD");

		const bidQuoterDiv = bidStackControlsDiv.children[1];
		bidQuoterDiv.onmousedown = stackEnableStackChange(familyName, "BID", "QUOTER");

		let askStackControlsDiv = familyDetails.children[16];
		const askQuoterDiv = askStackControlsDiv.children[0];
		askQuoterDiv.onmousedown = stackEnableStackChange(familyName, "ASK", "QUOTER");

		const askPicardDiv = askStackControlsDiv.children[1];
		askPicardDiv.onmousedown = stackEnableStackChange(familyName, "ASK", "PICARD");

		const allEnableDiv = familyDetails.children[17]; // family.find(".stackControls.allEnabled");
		allEnableDiv.onmousedown = stackEnableAllStackChange(familyName);

		const cleanFamilyDiv = familyDetails.children[18]; // family.find(".stackControls.cleanParent");
		cleanFamilyDiv.onmousedown = function () {
			ws.send(command("cleanParent", [familyName]));
		};

		familiesDiv.append(family);

		// addSortedDiv($("#families").find(".family"), family, tableComparator);

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

	} else if (aIsAsylum) {
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

function lazySymbolSubscribe(familyName) {
	subscribedToSymbols.add(familyName)
}

function setParentData(familyName, uiName, bidPriceOffset, askPriceOffset, bidPicardEnabled, bidQuoterEnabled, askPicardEnabled,
	askQuoterEnabled) {

	const familyDetailElems = addFamily(familyName, false, uiName).children()[0].children;

	familyDetailElems[10].innerText = bidPriceOffset;
	familyDetailElems[12].innerText = askPriceOffset;

	const bidStackControls = familyDetailElems[15];
	if (bidPicardEnabled) {
		bidStackControls.children[0].classList.add("enabled")
	} else {
		bidStackControls.children[0].classList.remove("enabled")
	}

	if (bidQuoterEnabled) {
		bidStackControls.children[1].classList.add("enabled")
	} else {
		bidStackControls.children[0].classList.add("enabled")
	}

	const askStackControls = familyDetailElems[16];
	if (askPicardEnabled) {
		askStackControls.children[0].classList.add("enabled")
	} else {
		askStackControls.children[0].classList.remove("enabled")
	}

	if (askQuoterEnabled) {
		askStackControls.children[1].classList.add("enabled")
	} else {
		askStackControls.children[0].classList.add("enabled")
	}
}

function removeChild(familyName, childSymbol) {

	const rowID = cleanID(childSymbol);
	const row = $("#" + rowID);

	const family = row.parent().parent();
	row.remove();
	setActiveChildCounts(family);

	const familyID = "family_" + cleanID(familyName);
	let familyElem = findChild(familyID);
	if (familyElem.length >= 1) {
		setChildCount(familyName);
	}
}

function setChild(familyName, childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier, familyToChildRatio) {

	const rowID = cleanID(childSymbol);
	let row = document.getElementById("#" + rowID);

	let familyElems = addFamily(familyName).children();
	const exchangeTable = familyElems[1]
	let quoteSymbolCell;
	if (!row) {

		row = exchangeTable.children[1].cloneNode(true); // find(".header").clone();
		row.classList.remove("header");
		row.classList.remove("hidden");
		row.setAttribute("id", rowID);
		row.classList.add("dataRow");
		row.classList.add("unregistered");

		const symbolElem = row.children[0].children[0];
		quoteSymbolCell = setCellData(symbolElem, childSymbol); // row.find(".symbol"), childSymbol);

		// TODO - fix this here
		// addSortedDiv(exchangeTable.find(".row"), row, rowComparator);
		exchangeTable.appendChild(row);

		// TODO - fix this here

		// } else if (row.parent().get(0) !== exchangeTable.get(0)) {
	} else if (row.parentElement !== exchangeTable) {

		const oldFamily = row.parentElement.parentElement.children[0].children[7].innerText; //.find(".familyName").text();
		row.remove();
		setChildCount(oldFamily);

		quoteSymbolCell = row.children[0].children[0]; //find(".symbol");
		exchangeTable.appendChild(row);
	} else {
		quoteSymbolCell = row.children[0].children[0];
	}

	quoteSymbolCell.onclick = function () {
		launchLadder(childSymbol);
	};

	const rowChildren = row.children[0];
	const bidQtyMultiplierDiv = addNumberBox(rowChildren.children[1].children[0]); // , ".bid.qtyMultiplier");
	const bidPriceOffsetDiv = addNumberBox(rowChildren.children[2].children[0]); // row, ".bid.priceOffset");

	const familyToChildRatioDiv = addNumberBox(rowChildren.children[3].children[0]); // row, ".familyToChildRatio");

	const askPriceOffsetDiv = addNumberBox(rowChildren.children[4].children[0]); //row, ".ask.priceOffset");
	const askQtyMultiplierDiv = addNumberBox(rowChildren.children[5].children[0]); // row, ".ask.qtyMultiplier");

	setDoubleData(bidPriceOffsetDiv, bidPriceOffset);
	setDoubleData(bidQtyMultiplierDiv, bidQtyMultiplier);
	setDoubleData(askPriceOffsetDiv, askPriceOffset);
	setDoubleData(askQtyMultiplierDiv, askQtyMultiplier);

	setDoubleData(familyToChildRatioDiv, familyToChildRatio);

	const submitButton = row.children[0].children[6]; // find("input[type=Submit]");
	submitButton.classList.remove("hidden");
	submitButton.onclick = function () {

		const bidOffset = bidPriceOffsetDiv.value;
		const bidMultiplier = bidQtyMultiplierDiv.value;

		const askOffset = askPriceOffsetDiv.value;
		const askMultiplier = askQtyMultiplierDiv.value;

		const ratio = familyToChildRatioDiv.value;

		ws.send(command("setRelationship", [childSymbol, bidOffset, bidMultiplier, askOffset, askMultiplier, ratio]));
	};

	const orphanButton = row.children[0].children[7]; // find(".orphanButton");
	orphanButton.classList.remove("hidden");
	orphanButton.onclick = function () {
		ws.send(command("orphanChild", [childSymbol]));
	};

	const killSymbolButton = row.children[0].children[8]; //(".killSymbolButton");
	killSymbolButton.classList.remove("hidden");
	killSymbolButton.onclick = function () {
		ws.send(command("killChild", [childSymbol]));
	};

	const childControls = row.children[2]; // find(".childControls");

	const openConfigWindowDiv = childControls.children[0].children[0];//(".configWindow");
	openConfigWindowDiv.onclick = function () {
		popUp("/stackConfig#;" + childSymbol, "Configs", 2200, 400);
	};

	// const bidControls = childControls.find(".bid");
	// const askControls = childControls.find(".ask");

	const bidPicardButton = childControls.children[1].children[0]; // .find(".picardEnabled");
	const bidQuoterButton = childControls.children[1].children[1]; // .find(".quoterEnabled");
	const askQuoterButton = childControls.children[3].children[1]; // .find(".quoterEnabled");
	const askPicardButton = childControls.children[3].children[0]; // .find(".picardEnabled");
	bidPicardButton.onmousedown = stackChildEnableStackChange(familyName, childSymbol, "BID", "PICARD");
	bidQuoterButton.onmousedown = stackChildEnableStackChange(familyName, childSymbol, "BID", "QUOTER");
	askQuoterButton.onmousedown = stackChildEnableStackChange(familyName, childSymbol, "ASK", "QUOTER");
	askPicardButton.onmousedown = stackChildEnableStackChange(familyName, childSymbol, "ASK", "PICARD");

	const bidStartConfigWindowDiv = childControls.children[2].children[0]; // .find(".start");
	bidStartConfigWindowDiv.onclick = function () {
		ws.send(command("startChild", [familyName, childSymbol, "BID"]));
	};

	const bidStopConfigWindowDiv = childControls.children[2].children[1];
	bidStopConfigWindowDiv.onclick = function () {
		ws.send(command("stopChild", [familyName, childSymbol, "BID"]));
	};

	const askStartConfigWindowDiv = childControls.children[4].children[0];// askControls.find(".start");
	askStartConfigWindowDiv.onclick = function () {
		ws.send(command("startChild", [familyName, childSymbol, "ASK"]));
	};

	const askStopConfigWindowDiv = childControls.children[4].children[1];
	askStopConfigWindowDiv.onclick = function () {
		ws.send(command("stopChild", [familyName, childSymbol, "ASK"]));
	};

	setChildCount(familyName);
}

function setChildData(childSymbol, leanSymbol, nibblerName, isBidStrategyOn, bidInfo, bidPicardEnabled, bidQuoterEnabled, isAskStrategyOn,
	askInfo, askPicardEnabled, askQuoterEnabled) {

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

	return aSymbol < bSymbol ? -1 : aSymbol === bSymbol ? 0 : 1;
}

function addNumberBox(inputElem) {

	//
	// if (!input.length) {
	// 	input = $("<input type=\"number\"/>");
	// 	div.text("");
	// 	div.append(input);
	// }
	inputElem.onkeypress = function () {
		if (inputElem.value !== inputElem.getAttribute("data")) {
			inputElem.classList.add("notPersisted")
		} else {
			inputElem.classList.remove("notPersisted")
		}
	};
	inputElem.classList.remove("hidden");
	// input.off("input").on("input", function () {
	// 	input.toggleClass("notPersisted", input.val() !== input.attr("data"));
	// });

	return inputElem;

}

function setCellData(input, value) {

	if (typeof value != "undefined") {
		input.setAttribute("data", value);
		input.innerText = value;
	} else {
		input.setAttribute("data", "");
		input.innerText = "";
	}
	return input
}

function setDoubleData(input, value) {

	if (typeof value != "undefined") {
		input.setAttribute("data", value);
		input.value = parseFloat(value);
	} else {
		input.setAttribute("data", "");
		input.value = "";
	}
	input.classList.remove("notPersisted");
}

function setChildCount(familyName) {

	const family = addFamily(familyName);
	const orderCountDiv = family.children()[0].children[2]; // find(".childCount");
	const orders = family.children()[1].children.length - 1;
	orderCountDiv.innerText = orders;
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

function displayInfoMsg(text) {
	console.log(text);
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

function setFamilyName(parentSymbol, uiName) {
	const familyID = "family_" + cleanID(parentSymbol);
	let family = findChild(familyID);
	if (family.length >= 1) {
		family.find('.uiName').text(uiName);
	}
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

function sendUIVersionFormat(uiVersionNum) {
	if (UI_VERSION_NUM !== uiVersionNum) {
		throw `Version num mismatch: expected ${UI_VERSION_NUM}, got: ${uiVersionNum}`;
	}
}

function sendInitializationParentData(cachedFamilyData) {
	const parsed = cachedFamilyData.split(DELIMITER);
	let i = 0;
	while (i < parsed.length) {
		const funcName = parsed[i];
		i += 1;
		let numArgs;
		if ("addFamily" === funcName) {
			numArgs = 3;
			const familyName = parsed[i];
			const isAsylum = parsed[i + 1] === "1";
			const uiName = parsed[i + 2];
			addFamily(familyName, isAsylum, uiName);
		} else if ("setParentData" === funcName) {
			numArgs = 5;

			const familyName = parsed[i];
			const uiName = parsed[i + 1];
			const bidPriceOffset = parsed[i + 2];
			const askPriceOffset = parsed[i + 3];
			const bitSet = parseInt(parsed[i + 4]);
			const bidPicardEnabled = bitSet & 1;
			const bidQuoterEnabled = bitSet & 2;
			const askPicardEnabled = bitSet & 4;
			const askQuoterEnabled = bitSet & 8;
			setParentData(familyName, uiName, bidPriceOffset, askPriceOffset, bidPicardEnabled, bidQuoterEnabled, askPicardEnabled,
				askQuoterEnabled)
		} else {
			throw `Invalid function called ${funcName}`;
		}
		i += numArgs;
	}
}

function sendInitializationChildData(cachedFamilyData) {
	setTimeout(() => {
		const parsed = cachedFamilyData.split(DELIMITER);
		let i = 0;
		while (i < parsed.length) {
			const funcName = parsed[i];
			i += 1;
			let numArgs;
			if ("setChild" === funcName) {
				numArgs = 7;
				const familyName = parsed[i];
				const childSymbol = parsed[i + 1];
				const bidPriceOffset = parsed[i + 2];
				const bidQtyMultiplier = parsed[i + 3];
				const askPriceOffset = parsed[i + 4];
				const askQtyMultiplier = parsed[i + 5];
				const familyToChildRatio = parsed[i + 6];
				setChild(familyName, childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier, familyToChildRatio);
			} else if ("setChildData" === funcName) {
				numArgs = 6;

				const symbol = parsed[i];
				const leanSymbol = parsed[i + 1];
				const nibblerName = parsed[i + 2];
				const bidInfo = parsed[i + 3];
				const askInfo = parsed[i + 4];
				const bitSet = parseInt(parsed[i + 5]);
				const isBidStrategyOn = bitSet & 1;
				const isBidPicardEnabled = bitSet & 2;
				const isBidQuoterEnabled = bitSet & 4;
				const isAskStrategyOn = bitSet & 8;
				const isAskPicardEnabled = bitSet & 16;
				const isAskQuoterEnabled = bitSet & 32;
				setChildData(symbol, leanSymbol, nibblerName, isBidStrategyOn, bidInfo, isBidPicardEnabled, isBidQuoterEnabled,
					isAskStrategyOn,
					askInfo, isAskPicardEnabled, isAskQuoterEnabled);
			} else {
				throw `Invalid function called ${funcName}`;
			}
			i += numArgs;
		}
	}, 200);
}

