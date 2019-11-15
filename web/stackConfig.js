let ws;

const NO_FILTER = "--ALL--";
const STICKY_HEADER_ID = "stickyHeader";
let symbolFilter;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (m) {
		eval(m);
	};

	const nibblerFilter = document.location.hash;
	let nibblerEnd = nibblerFilter.indexOf(';', 0);
	if (nibblerEnd < 0) {
		nibblerEnd = nibblerFilter.length;
	}
	const nibbler = nibblerFilter.substr(1, nibblerEnd - 1);
	symbolFilter = nibblerFilter.substr(nibblerEnd + 1, nibblerFilter.length).replace(/%20/g, " ").split(",").filter(function (el) {
		return 0 !== el.length;
	});

	setupTable();

	ws.send("subscribe");
	const configTypeCombo = $("#configTypes");
	addConfigTypeOption(configTypeCombo, NO_FILTER);
	configTypeCombo.on("change", function () {
		selectedConfigTypeChanged();
	});

	const massControlRow = setRow("massControl", "ALL", "ALL", "");
	massControlRow.find("input").each(setupCopyToAllRows);

	const massControlSubmitAll = massControlRow.find(".button button");
	massControlSubmitAll.text("Submit all");
	massControlSubmitAll.off("click").click(function () {
		getAllConfigRows().filter(":not(.filtered)").find(".button button").click();
		massControlRow.find("input").each(function (i, input) {
			input = $(input);
			input.val("");
			input.prop("checked", false);
			input.removeClass("notPersisted");
			input.parent().removeClass("notPersisted");
		});
	});

	$("#setRFQConfig").click(function () {
		setAllRFQConfig();
	});
});

function removeAll(nibblerName) {
	getAllConfigRows().filter("[data-nibblerName=\"" + nibblerName + "\"]").remove();
}

function setConfigTypes(configTypes) {

	const configTypeCombo = $("#configTypes");
	$(configTypeCombo).find("option").remove();
	addConfigTypeOption(configTypeCombo, NO_FILTER);
	configTypes.forEach(function (configType) {
		addConfigTypeOption(configTypeCombo, configType);
	});

	if (configTypes.length) {
		selectedConfigTypeChanged();
	}
}

function selectedConfigTypeChanged() {

	const selectedConfigType = getSelectedConfigType();

	const rows = getAllConfigRows();
	rows.each(function (i, row) {
		row = $(row);
		filterRowByConfigType(row, selectedConfigType);
	});
}

function filterRowByConfigType(row, selectedConfigType) {
	row.toggleClass("filtered",
		STICKY_HEADER_ID != row.attr("ID") && NO_FILTER != selectedConfigType && row.attr("configType") != selectedConfigType);
}

function setupCopyToAllRows(i, input) {

	input = $(input);
	const parentDiv = input.parent();
	let findCode = "";
	const divClasses = parentDiv.attr("class");
	divClasses.split(" ").forEach(function (s) {
		findCode += "." + s;
	});
	findCode += " input";
	const inputClasses = input.attr("class");
	if (inputClasses) {
		inputClasses.split(" ").forEach(function (s) {
			findCode += "." + s;
		});
	}

	if (parentDiv.hasClass("boolean")) {
		input.change(function () {
			const copyValue = input.is(":checked");
			const targetInputs = getAllConfigRows().filter(":not(.filtered)").find(findCode);
			targetInputs.each(function (i, targetInput) {
				targetInput = $(targetInput);
				targetInput.prop("checked", copyValue);
				checkInputPersisted(targetInput);
			});
		});
		input.off("dblclick").dblclick(function () {
			const targetInputs = getAllConfigRows().filter(":not(.filtered)").find(findCode);
			targetInputs.each(function (i, targetInput) {
				targetInput = $(targetInput);
				targetInput.prop("checked", "true" === targetInput.attr("data"));
				checkInputPersisted(targetInput);
			});
			input.prop("checked", false);
			checkInputPersisted(input);
		});
	} else {
		input.on("input", function () {

			const copyValue = input.val();
			const targetInputs = getAllConfigRows().filter(":not(.filtered)").find(findCode);
			targetInputs.each(function (i, targetInput) {
				targetInput = $(targetInput);
				if (copyValue) {
					targetInput.val(copyValue);
					targetInput.toggleClass("notPersisted", copyValue !== targetInput.attr("data"));
				} else {
					targetInput.val(targetInput.attr("data"));
					targetInput.removeClass("notPersisted");
				}
			});
		});
	}
}

function addConfigTypeOption(configTypeCombo, configType) {

	const option = $("<option value=\"" + configType + "\">" + configType + "</option>");
	option.addClass(configType);
	option.attr("data", configType);
	configTypeCombo.append(option);
}

function setAllRFQConfig() {

	const massControlRow = $("#massControlALL");

	setRowDetails(massControlRow,
		86400000, true, true, 3000, true, 50, 0, 0, false, 0,
		500, 50,
		500, 50, 1, 3, 1.0,
		0, false, 0, 0, 0,
		1, 10000000, 1, 100, 1, 1, false, false, 1, 5, 100000, 0, 0, 0, 0, 0,
		1, 10000000, 1, 100, 1, 1, false, false, 1, 5, 100000, 0, 0, 0, 0, 0);

	forceRowChanged(massControlRow);
}

function forceRowChanged(row) {

	const changeEvent = new Event('change');
	const inputEvent = new Event("input");
	row.find("input").each(function (i, input) {
		input.dispatchEvent(inputEvent);
		input.dispatchEvent(changeEvent);
	});
}

function setRow(nibblerName, configGroupID, symbol, configType, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction,
	quoteAuctionTheoMaxBPSThrough, quoteIsAllowEmptyBook, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks,
	quoteIsBettermentOppositeSide, quoteOppositeSideBettermentTicks, fxMaxBookAgeMillis, fxMaxJumpBPS, leanMaxBookAgeMillis,
	leanMaxJumpBPS, leanRequiredQty, leanMaxPapaWeight, leanToQuoteRatio, leanPriceAdjustmentRaw, additiveIsEnabled,
	additiveMaxSignalAgeMillis, additiveMinRequiredBPS, additiveMaxBPS, bidPlanMinLevelQty, bidPlanMaxLevelQty, bidPlanLotSize,
	bidPlanMaxLevels, bidMinPicardQty, bidMaxOrdersPerLevel, bidIsOnlySubmitBestLevel, bidIsQuoteBettermentOn, bidModTicks,
	bidQuoteFlickerBuffer, bidQuotePicardMaxBPSThrough, bidPicardMaxPapaWeight, bidPicardMaxPerSec, bidPicardMaxPerMin,
	bidPicardMaxPerHour, bidPicardMaxPerDay, askPlanMinLevelQty, askPlanMaxLevelQty, askPlanLotSize, askPlanMaxLevels,
	askMinPicardQty, askMaxOrdersPerLevel, askIsOnlySubmitBestLevel, askIsQuoteBettermentOn, askModTicks, askQuoteFlickerBuffer,
	askQuotePicardMaxBPSThrough, askPicardMaxPapaWeight, askPicardMaxPerSec, askPicardMaxPerMin, askPicardMaxPerHour,
	askPicardMaxPerDay) {

	const rowID = nibblerName + configGroupID;
	let row = $("#" + rowID);
	if (0 === symbolFilter.length || -1 < symbolFilter.indexOf(symbol) || "ALL" === symbol) {
		if (row.length < 1) {
			row = $("#header").clone();
			row.attr("id", rowID);
			row.attr("data-nibblerName", nibblerName);
			row.attr("data-configGroupID", configGroupID);
			row.removeClass("header");
			$("#table").append(row);

			setupStickyColumns(row);

			row.find("div").each(function (i, d) {

				d = $(d);
				if (!d.hasClass("button") && !d.hasClass("stickyColumn")) {
					d.text("");

					let inputType;
					if (d.hasClass("boolean")) {
						inputType = "checkbox";
					} else {
						inputType = "number";
					}

					if (d.hasClass("sided")) {
						d.append($("<input class=\"bid\" type=\"" + inputType + "\"/>"));
						d.append($("<input class=\"ask\" type=\"" + inputType + "\"/>"));
					} else {
						var input = $("<input type=\"" + inputType + "\"/>");
						d.append(input);
					}

					d.find("input").each(function (i, input) {
						input = $(input);

						if (d.hasClass("boolean")) {
							input.change(function () {
								input.parent().toggleClass("notPersisted", input.is(":checked") != ("true" === input.attr("data")));
							});
						} else {
							input.on("input", function () {
								input.toggleClass("notPersisted", input.val() != input.attr("data"));
							});
						}
					});
				}
			});

			row.find(".button button").click(function () {
				submitRow(row);
			});

			const limitedBufferInput = row.find(".quoteFlickerBuffer input, .maxPapaWeight input");
			limitedBufferInput.attr("min", 0);
			limitedBufferInput.attr("max", 100);
			limitedBufferInput.on('keydown', function (e) {

				if (10 == $(this).val() &&
					((96 < e.keyCode && e.keyCode < 106) || (48 < e.keyCode && e.keyCode < 58))) {

					e.preventDefault();
				} else if (10 < $(this).val() &&
					((95 < e.keyCode && e.keyCode < 106) || (47 < e.keyCode && e.keyCode < 58))) {

					e.preventDefault();
				}
			});
		}
		row.attr("configType", configType);
		const selectedConfigType = getSelectedConfigType();
		filterRowByConfigType(row, selectedConfigType);

		row.find(".symbol").text(symbol);

		const sideCol = row.find(".side");
		sideCol.text("");
		const bidSide = $("<div class=\"bid\">BID</div>");
		const askSide = $("<div class=\"ask\">ASK</div>");
		sideCol.append(bidSide);
		sideCol.append(askSide);

		row.find(".configType").text(configType);

		setRowDetails(row, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction, quoteAuctionTheoMaxBPSThrough,
			quoteIsAllowEmptyBook, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks, quoteIsBettermentOppositeSide,
			quoteOppositeSideBettermentTicks, fxMaxBookAgeMillis, fxMaxJumpBPS, leanMaxBookAgeMillis, leanMaxJumpBPS, leanRequiredQty,
			leanMaxPapaWeight, leanToQuoteRatio, leanPriceAdjustmentRaw, additiveIsEnabled, additiveMaxSignalAgeMillis,
			additiveMinRequiredBPS, additiveMaxBPS, bidPlanMinLevelQty, bidPlanMaxLevelQty, bidPlanLotSize, bidPlanMaxLevels,
			bidMinPicardQty, bidMaxOrdersPerLevel, bidIsOnlySubmitBestLevel, bidIsQuoteBettermentOn, bidModTicks, bidQuoteFlickerBuffer,
			bidQuotePicardMaxBPSThrough, bidPicardMaxPapaWeight, bidPicardMaxPerSec, bidPicardMaxPerMin, bidPicardMaxPerHour,
			bidPicardMaxPerDay, askPlanMinLevelQty, askPlanMaxLevelQty, askPlanLotSize, askPlanMaxLevels, askMinPicardQty,
			askMaxOrdersPerLevel, askIsOnlySubmitBestLevel, askIsQuoteBettermentOn, askModTicks, askQuoteFlickerBuffer,
			askQuotePicardMaxBPSThrough, askPicardMaxPapaWeight, askPicardMaxPerSec, askPicardMaxPerMin, askPicardMaxPerHour,
			askPicardMaxPerDay);

		return row;
	} else {
		row.remove();
	}
}

function setRowDetails(row, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction, quoteAuctionTheoMaxBPSThrough,
	quoteIsAllowEmptyBook, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks, quoteIsBettermentOppositeSide,
	quoteOppositeSideBettermentTicks, fxMaxBookAgeMillis, fxMaxJumpBPS, leanMaxBookAgeMillis, leanMaxJumpBPS, leanRequiredQty,
	leanMaxPapaWeight, leanToQuoteRatio, leanPriceAdjustmentRaw, additiveIsEnabled, additiveMaxSignalAgeMillis, additiveMinRequiredBPS,
	additiveMaxBPS, bidPlanMinLevelQty, bidPlanMaxLevelQty, bidPlanLotSize, bidPlanMaxLevels, bidMinPicardQty, bidMaxOrdersPerLevel,
	bidIsOnlySubmitBestLevel, bidIsQuoteBettermentOn, bidModTicks, bidQuoteFlickerBuffer, bidQuotePicardMaxBPSThrough,
	bidPicardMaxPapaWeight, bidPicardMaxPerSec, bidPicardMaxPerMin, bidPicardMaxPerHour, bidPicardMaxPerDay, askPlanMinLevelQty,
	askPlanMaxLevelQty, askPlanLotSize, askPlanMaxLevels, askMinPicardQty, askMaxOrdersPerLevel, askIsOnlySubmitBestLevel,
	askIsQuoteBettermentOn, askModTicks, askQuoteFlickerBuffer, askQuotePicardMaxBPSThrough, askPicardMaxPapaWeight, askPicardMaxPerSec,
	askPicardMaxPerMin, askPicardMaxPerHour, askPicardMaxPerDay) {

	setCellData(row, ".quoteInst.maxBookAgeMillis input", quoteMaxBookAgeMillis);

	setBoolData(row, ".quoteInst.isAuctionQuotingEnabled input", quoteIsAuctionQuotingEnabled);
	setBoolData(row, ".quoteInst.isOnlyAuction input", quoteIsOnlyAuction);
	setCellData(row, ".quoteInst.quoteAuctionTheoMaxBPSThrough input", quoteAuctionTheoMaxBPSThrough);
	setBoolData(row, ".quoteInst.isAllowEmptyBook input", quoteIsAllowEmptyBook);
	setCellData(row, ".quoteInst.maxJumpBPS input", quoteMaxJumpBPS);
	setCellData(row, ".quoteInst.maxJumpBPS input", quoteMaxJumpBPS);
	setCellData(row, ".quoteInst.bettermentQty input", quoteBettermentQty);
	setCellData(row, ".quoteInst.bettermentTicks input", quoteBettermentTicks);
	setBoolData(row, ".quoteInst.isBettermentOppositeSide input", quoteIsBettermentOppositeSide);
	setCellData(row, ".quoteInst.oppositeSideBettermentTicks input", quoteOppositeSideBettermentTicks);

	setCellData(row, ".fx.maxBookAgeMillis input", fxMaxBookAgeMillis);
	setCellData(row, ".fx.maxJumpBPS input", fxMaxJumpBPS);

	setCellData(row, ".leanInst.maxBookAgeMillis input", leanMaxBookAgeMillis);
	setCellData(row, ".leanInst.maxJumpBPS input", leanMaxJumpBPS);
	setCellData(row, ".leanInst.requiredQty input", leanRequiredQty);
	setCellData(row, ".leanInst.maxPapaWeight input", leanMaxPapaWeight);
	setDoubleData(row, ".leanInst.leanToQuoteRatio input", leanToQuoteRatio);
	setDoubleData(row, ".leanInst.leanPriceAdjustment input", leanPriceAdjustmentRaw);

	setBoolData(row, ".additiveInst.isEnabled input", additiveIsEnabled);
	setCellData(row, ".additiveInst.maxSignalAgeMillis input", additiveMaxSignalAgeMillis);
	setCellData(row, ".additiveInst.minRequiredBPS input", additiveMinRequiredBPS);
	setCellData(row, ".additiveInst.maxBPS input", additiveMaxBPS);

	setCellData(row, ".plan.minLevelQty .bid", bidPlanMinLevelQty);
	setCellData(row, ".plan.maxLevelQty .bid", bidPlanMaxLevelQty);
	setCellData(row, ".plan.lotSize .bid", bidPlanLotSize);
	setCellData(row, ".plan.maxLevels .bid", bidPlanMaxLevels);
	setCellData(row, ".plan.minPicardQty .bid", bidMinPicardQty);

	setCellData(row, ".strategy.maxOrdersPerLevel .bid", bidMaxOrdersPerLevel);
	setBoolData(row, ".strategy.isOnlySubmitBestLevel .bid", bidIsOnlySubmitBestLevel);
	setBoolData(row, ".strategy.isQuoteBettermentOn .bid", bidIsQuoteBettermentOn);
	setCellData(row, ".strategy.quoteModTicks .bid", bidModTicks);
	setCellData(row, ".strategy.quoteFlickerBuffer .bid", bidQuoteFlickerBuffer);
	setCellData(row, ".strategy.quotePicardMaxBPSThrough .bid", bidQuotePicardMaxBPSThrough);
	setCellData(row, ".strategy.picardMaxPapaWeight .bid", bidPicardMaxPapaWeight);
	setCellData(row, ".strategy.picardMaxPerSec .bid", bidPicardMaxPerSec);
	setCellData(row, ".strategy.picardMaxPerMin .bid", bidPicardMaxPerMin);
	setCellData(row, ".strategy.picardMaxPerHour .bid", bidPicardMaxPerHour);
	setCellData(row, ".strategy.picardMaxPerDay .bid", bidPicardMaxPerDay);

	setCellData(row, ".plan.minLevelQty .ask", askPlanMinLevelQty);
	setCellData(row, ".plan.maxLevelQty .ask", askPlanMaxLevelQty);
	setCellData(row, ".plan.lotSize .ask", askPlanLotSize);
	setCellData(row, ".plan.maxLevels .ask", askPlanMaxLevels);
	setCellData(row, ".plan.minPicardQty .ask", askMinPicardQty);

	setCellData(row, ".strategy.maxOrdersPerLevel .ask", askMaxOrdersPerLevel);
	setBoolData(row, ".strategy.isOnlySubmitBestLevel .ask", askIsOnlySubmitBestLevel);
	setBoolData(row, ".strategy.isQuoteBettermentOn .ask", askIsQuoteBettermentOn);
	setCellData(row, ".strategy.quoteModTicks .ask", askModTicks);
	setCellData(row, ".strategy.quoteFlickerBuffer .ask", askQuoteFlickerBuffer);
	setCellData(row, ".strategy.quotePicardMaxBPSThrough .ask", askQuotePicardMaxBPSThrough);
	setCellData(row, ".strategy.picardMaxPapaWeight .ask", askPicardMaxPapaWeight);
	setCellData(row, ".strategy.picardMaxPerSec .ask", askPicardMaxPerSec);
	setCellData(row, ".strategy.picardMaxPerMin .ask", askPicardMaxPerMin);
	setCellData(row, ".strategy.picardMaxPerHour .ask", askPicardMaxPerHour);
	setCellData(row, ".strategy.picardMaxPerDay .ask", askPicardMaxPerDay);
}

function setCellData(row, cellID, value) {

	const input = row.find(cellID);
	if (typeof value != "undefined") {
		input.attr("data", value);
		input.val(value);
	} else {
		input.attr("data", "");
		input.val("");
	}
	input.removeClass("notPersisted");
}

function setDoubleData(row, cellID, value) {

	const input = row.find(cellID);
	if (typeof value != "undefined") {
		input.attr("data", value);
		input.val(parseFloat(value));
	} else {
		input.attr("data", "");
		input.val("");
	}
	input.removeClass("notPersisted");
}

function setBoolData(row, cellID, value) {

	const input = row.find(cellID);
	if (typeof value != "undefined") {
		input.attr("data", value);
		input.prop("checked", value);
	} else {
		input.attr("data", "");
		input.prop("checked", false);
	}
	checkInputPersisted(input);
}

function submitRow(row) {

	const nibblerName = row.attr("data-nibblerName");
	const configGroupID = row.attr("data-configGroupID");

	const quoteMaxBookAgeMillis = getCellData(row, ".quoteInst.maxBookAgeMillis input");
	const quoteIsAuctionQuotingEnabled = getCellBool(row, ".quoteInst.isAuctionQuotingEnabled input");
	const quoteIsOnlyAuction = getCellBool(row, ".quoteInst.isOnlyAuction input");
	const quoteAuctionTheoMaxBPSThrough = getCellData(row, ".quoteInst.quoteAuctionTheoMaxBPSThrough input");
	const quoteIsAllowEmptyBook = getCellBool(row, ".quoteInst.isAllowEmptyBook input");
	const quoteMaxJumpBPS = getCellData(row, ".quoteInst.maxJumpBPS input");
	const quoteBettermentQty = getCellData(row, ".quoteInst.bettermentQty input");
	const quoteBettermentTicks = getCellData(row, ".quoteInst.bettermentTicks input");
	const quoteIsBettermentOppositeSide = getCellBool(row, ".quoteInst.isBettermentOppositeSide input");
	const quoteOppositeSideBettermentTicks = getCellData(row, ".quoteInst.oppositeSideBettermentTicks input");

	const fxMaxBookAgeMillis = getCellData(row, ".fx.maxBookAgeMillis input");
	const fxMaxJumpBPS = getCellData(row, ".fx.maxJumpBPS input");

	const leanMaxBookAgeMillis = getCellData(row, ".leanInst.maxBookAgeMillis input");
	const leanMaxJumpBPS = getCellData(row, ".leanInst.maxJumpBPS input");
	const leanRequiredQty = getCellData(row, ".leanInst.requiredQty input");
	const leanMaxPapaWeight = getCellData(row, ".leanInst.maxPapaWeight input");
	const leanToQuoteRatio = getCellFloat(row, ".leanInst.leanToQuoteRatio input");
	const leanPriceAdjustment = getCellFloat(row, ".leanInst.leanPriceAdjustment input");

	const additiveIsEnabled = getCellBool(row, ".additiveInst.isEnabled input");
	const additiveMaxSignalAgeMillis = getCellData(row, ".additiveInst.maxSignalAgeMillis input");
	const additiveMinRequiredBPS = getCellData(row, ".additiveInst.minRequiredBPS input");
	const additiveMaxBPS = getCellData(row, ".additiveInst.maxBPS input");

	const bidPlanMinLevelQty = getCellData(row, ".plan.minLevelQty .bid");
	const bidPlanMaxLevelQty = getCellData(row, ".plan.maxLevelQty .bid");
	const bidPlanLotSize = getCellData(row, ".plan.lotSize .bid");
	const bidPlanMaxLevels = getCellData(row, ".plan.maxLevels .bid");
	const bidMinPicardQty = getCellData(row, ".plan.minPicardQty .bid");

	const bidMaxOrdersPerLevel = getCellData(row, ".strategy.maxOrdersPerLevel .bid");
	const bidIsOnlySubmitBestLevel = getCellBool(row, ".strategy.isOnlySubmitBestLevel .bid");
	const bidIsQuoteBettermentOn = getCellBool(row, ".strategy.isQuoteBettermentOn .bid");
	const bidModTicks = getCellData(row, ".strategy.quoteModTicks .bid");
	const bidQuoteFlickerBuffer = getCellData(row, ".strategy.quoteFlickerBuffer .bid");
	const bidQuotePicardMaxBPSThrough = getCellData(row, ".strategy.quotePicardMaxBPSThrough .bid");
	const bidPicardMaxPapaWeight = getCellData(row, ".strategy.picardMaxPapaWeight .bid");
	const bidPicardMaxPerSec = getCellData(row, ".strategy.picardMaxPerSec .bid");
	const bidPicardMaxPerMin = getCellData(row, ".strategy.picardMaxPerMin .bid");
	const bidPicardMaxPerHour = getCellData(row, ".strategy.picardMaxPerHour .bid");
	const bidPicardMaxPerDay = getCellData(row, ".strategy.picardMaxPerDay .bid");

	const askPlanMinLevelQty = getCellData(row, ".plan.minLevelQty .ask");
	const askPlanMaxLevelQty = getCellData(row, ".plan.maxLevelQty .ask");
	const askPlanLotSize = getCellData(row, ".plan.lotSize .ask");
	const askPlanMaxLevels = getCellData(row, ".plan.maxLevels .ask");
	const askMinPicardQty = getCellData(row, ".plan.minPicardQty .ask");

	const askMaxOrdersPerLevel = getCellData(row, ".strategy.maxOrdersPerLevel .ask");
	const askIsOnlySubmitBestLevel = getCellBool(row, ".strategy.isOnlySubmitBestLevel .ask");
	const askIsQuoteBettermentOn = getCellBool(row, ".strategy.isQuoteBettermentOn .ask");
	const askModTicks = getCellData(row, ".strategy.quoteModTicks .ask");
	const askQuoteFlickerBuffer = getCellData(row, ".strategy.quoteFlickerBuffer .ask");
	const askQuotePicardMaxBPSThrough = getCellData(row, ".strategy.quotePicardMaxBPSThrough .ask");
	const askPicardMaxPapaWeight = getCellData(row, ".strategy.picardMaxPapaWeight .ask");
	const askPicardMaxPerSec = getCellData(row, ".strategy.picardMaxPerSec .ask");
	const askPicardMaxPerMin = getCellData(row, ".strategy.picardMaxPerMin .ask");
	const askPicardMaxPerHour = getCellData(row, ".strategy.picardMaxPerHour .ask");
	const askPicardMaxPerDay = getCellData(row, ".strategy.picardMaxPerDay .ask");

	ws.send(command("submitChange", [nibblerName, configGroupID, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction,
		quoteAuctionTheoMaxBPSThrough, quoteIsAllowEmptyBook, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks,
		quoteIsBettermentOppositeSide, quoteOppositeSideBettermentTicks, fxMaxBookAgeMillis, fxMaxJumpBPS, leanMaxBookAgeMillis,
		leanMaxJumpBPS, leanRequiredQty, leanMaxPapaWeight, leanToQuoteRatio, leanPriceAdjustment, additiveIsEnabled,
		additiveMaxSignalAgeMillis, additiveMinRequiredBPS, additiveMaxBPS, bidPlanMinLevelQty, bidPlanMaxLevelQty, bidPlanLotSize,
		bidPlanMaxLevels, bidMinPicardQty, bidMaxOrdersPerLevel, bidIsOnlySubmitBestLevel, bidIsQuoteBettermentOn, bidModTicks,
		bidQuoteFlickerBuffer, bidQuotePicardMaxBPSThrough, bidPicardMaxPapaWeight, bidPicardMaxPerSec, bidPicardMaxPerMin,
		bidPicardMaxPerHour, bidPicardMaxPerDay, askPlanMinLevelQty, askPlanMaxLevelQty, askPlanLotSize, askPlanMaxLevels, askMinPicardQty,
		askMaxOrdersPerLevel, askIsOnlySubmitBestLevel, askIsQuoteBettermentOn, askModTicks, askQuoteFlickerBuffer,
		askQuotePicardMaxBPSThrough, askPicardMaxPapaWeight, askPicardMaxPerSec, askPicardMaxPerMin, askPicardMaxPerHour,
		askPicardMaxPerDay]));
}

function getCellData(row, cellID) {
	return parseInt(row.find(cellID).val(), 10);
}

function getCellFloat(row, cellID) {
	return row.find(cellID).val();
}

function getCellBool(row, cellID) {
	return row.find(cellID).is(":checked");
}

function getAllConfigRows() {
	return $("#table").find(".row:not(#header):not(#massControlAll)");
}

function getSelectedConfigType() {
	return $("#configTypes").find("option:selected").text();
}

function checkInputPersisted(checkbox) {

	const parentDiv = checkbox.parent();
	const allInputs = parentDiv.find("input");
	let allPersisted = true;
	allInputs.each(function (i, input) {
		input = $(input);
		allPersisted &= input.prop("checked") == ("true" === input.attr("data"));
	});
	parentDiv.toggleClass("notPersisted", !allPersisted);
}

function setupTable() {

	document.oncontextmenu = function () {
		return false;
	};

	const header = $("#header");
	const stickyHeader = header.clone();
	stickyHeader.appendTo(header.parent());
	stickyHeader.attr("id", STICKY_HEADER_ID);
	const wind = $(window);
	const headerYPos = header.offset().top;
	const headerXPos = header.offset().left;

	wind.scroll(function () {

		if (headerYPos < wind.scrollTop()) {
			stickyHeader.toggleClass('sticky', true);

			const x = headerXPos - $(this).scrollLeft();
			stickyHeader.css('left', x);
		} else {
			stickyHeader.toggleClass('sticky', false);
		}
	});
}

function setupStickyColumns(row) {

	row.find(".stickyColumn").remove();
	const columns = row.find(".configID");

	const stickyColumns = $("<div class=\"stickyColumn\"></div>");

	stickyColumns.appendTo(row);
	columns.clone().appendTo(stickyColumns);
	stickyColumns.removeAttr("id");

	const referenceColumn = columns.filter(".leftMost");

	const wind = $(window);
	const columnXPos = referenceColumn.offset().left;

	wind.scroll(function () {

		if (columnXPos < wind.scrollLeft()) {
			stickyColumns.toggleClass('sticky', true);

			const columnYPos = referenceColumn.offset().top;
			const y = columnYPos - $(this).scrollTop();
			stickyColumns.css("top", y);
			stickyColumns.css("height", referenceColumn.outerHeight());
		} else {
			stickyColumns.toggleClass('sticky', false);
		}
	});
}