var ws;

var NO_FILTER = "--ALL--";
var STICKY_HEADER_ID = "stickyHeader";
var symbolFilter;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (m) {
		eval(m);
	};

	var nibblerFilter = document.location.hash;
	var nibblerEnd = nibblerFilter.indexOf(';', 0);
	if (nibblerEnd < 0) {
		nibblerEnd = nibblerFilter.length;
	}
	var nibbler = nibblerFilter.substr(1, nibblerEnd - 1);
	symbolFilter = nibblerFilter.substr(nibblerEnd + 1, nibblerFilter.length).split(",").filter(function (el) {
		return 0 !== el.length;
	});

	setupTable();

	ws.send("subscribe");
	var configTypeCombo = $("#configTypes");
	addConfigTypeOption(configTypeCombo, NO_FILTER);
	configTypeCombo.on("change", function () {
		selectedConfigTypeChanged();
	});

	var massControlRow = setRow("massControl", "ALL", "ALL", "");
	massControlRow.find("input").each(setupCopyToAllRows);

	var massControlSubmitAll = massControlRow.find(".button button");
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
});

function removeAll(nibblerName) {
	getAllConfigRows().filter("[data-nibblerName=\"" + nibblerName + "\"]").remove();
}

function setConfigTypes(configTypes) {

	var configTypeCombo = $("#configTypes");
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

	var selectedConfigType = getSelectedConfigType();

	var rows = getAllConfigRows();
	rows.each(function (i, row) {
		row = $(row);
		filterRowByConfigType(row, selectedConfigType);
	});
}

function filterRowByConfigType(row, selectedConfigType) {
	row.toggleClass("filtered", STICKY_HEADER_ID != row.attr("ID") && NO_FILTER != selectedConfigType && row.attr("configType") != selectedConfigType);
}

function setupCopyToAllRows(i, input) {

	input = $(input);
	var parentDiv = input.parent();
	var findCode = "";
	var divClasses = parentDiv.attr("class");
	divClasses.split(" ").forEach(function (s) {
		findCode += "." + s;
	});
	findCode += " input";
	var inputClasses = input.attr("class");
	if (inputClasses) {
		inputClasses.split(" ").forEach(function (s) {
			findCode += "." + s;
		});
	}

	if (parentDiv.hasClass("boolean")) {
		input.change(function () {
			var copyValue = input.is(":checked");
			var targetInputs = getAllConfigRows().filter(":not(.filtered)").find(findCode);
			targetInputs.each(function (i, targetInput) {
				targetInput = $(targetInput);
				targetInput.prop("checked", copyValue);
				checkInputPersisted(targetInput);
			});
		});
		input.off("dblclick").dblclick(function () {
			var targetInputs = getAllConfigRows().filter(":not(.filtered)").find(findCode);
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

			var copyValue = input.val();
			var targetInputs = getAllConfigRows().filter(":not(.filtered)").find(findCode);
			targetInputs.each(function (i, targetInput) {
				targetInput = $(targetInput);
				if (copyValue) {
					targetInput.val(copyValue);
					targetInput.toggleClass("notPersisted", copyValue != targetInput.attr("data"));
				} else {
					targetInput.val(targetInput.attr("data"));
					targetInput.removeClass("notPersisted");
				}
			});
		});
	}
}

function addConfigTypeOption(configTypeCombo, configType) {

	var option = $("<option value=\"" + configType + "\">" + configType + "</option>");
	option.addClass(configType);
	option.attr("data", configType);
	configTypeCombo.append(option);
}

function setRow(nibblerName, configGroupID, symbol, configType, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction,
				quoteAuctionTheoMaxTicksThrough, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks, fxMaxBookAgeMillis,
				fxMaxJumpBPS, leanMaxBookAgeMillis, leanMaxJumpBPS, leanRequiredQty, leanMaxPapaWeight, leanToQuoteRatio,
				leanPriceAdjustmentRaw, additiveIsEnabled, additiveMaxSignalAgeMillis, additiveMinRequiredBPS, additiveMaxBPS,
				bidPlanMinLevelQty, bidPlanMaxLevelQty, bidPlanLotSize, bidPlanMaxLevels, bidMinPicardQty, bidMaxOrdersPerLevel,
				bidIsOnlySubmitBestLevel, bidIsQuoteBettermentOn, bidModTicks, bidQuoteFlickerBuffer, bidQuotePicardMaxTicksThrough,
				bidPicardMaxPerSec, bidPicardMaxPerMin, bidPicardMaxPerHour, bidPicardMaxPerDay, askPlanMinLevelQty, askPlanMaxLevelQty,
				askPlanLotSize, askPlanMaxLevels, askMinPicardQty, askMaxOrdersPerLevel, askIsOnlySubmitBestLevel, askIsQuoteBettermentOn,
				askModTicks, askQuoteFlickerBuffer, askQuotePicardMaxTicksThrough, askPicardMaxPerSec, askPicardMaxPerMin,
				askPicardMaxPerHour, askPicardMaxPerDay) {

	var rowID = nibblerName + configGroupID;
	var row = $("#" + rowID);
	if (0 === symbolFilter.length || -1 < symbolFilter.indexOf(symbol) || "ALL" == symbol) {
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

					var inputType;
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

			var limitedBufferInput = row.find(".quoteFlickerBuffer input, .maxPapaWeight input");
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
		var selectedConfigType = getSelectedConfigType();
		filterRowByConfigType(row, selectedConfigType);

		row.find(".symbol").text(symbol);

		var sideCol = row.find(".side");
		sideCol.text("");
		var bidSide = $("<div class=\"bid\">BID</div>");
		var askSide = $("<div class=\"ask\">ASK</div>");
		sideCol.append(bidSide);
		sideCol.append(askSide);

		row.find(".configType").text(configType);

		setCellData(row, ".quoteInst.maxBookAgeMillis input", quoteMaxBookAgeMillis);

		setBoolData(row, ".quoteInst.isAuctionQuotingEnabled input", quoteIsAuctionQuotingEnabled);
		setBoolData(row, ".quoteInst.isOnlyAuction input", quoteIsOnlyAuction);
		setCellData(row, ".quoteInst.auctionTheoMaxTicksThrough input", quoteAuctionTheoMaxTicksThrough);
		setCellData(row, ".quoteInst.maxJumpBPS input", quoteMaxJumpBPS);
		setCellData(row, ".quoteInst.bettermentQty input", quoteBettermentQty);
		setCellData(row, ".quoteInst.bettermentTicks input", quoteBettermentTicks);

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
		setCellData(row, ".strategy.quotePicardMaxTicksThrough .bid", bidQuotePicardMaxTicksThrough);
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
		setCellData(row, ".strategy.quotePicardMaxTicksThrough .ask", askQuotePicardMaxTicksThrough);
		setCellData(row, ".strategy.picardMaxPerSec .ask", askPicardMaxPerSec);
		setCellData(row, ".strategy.picardMaxPerMin .ask", askPicardMaxPerMin);
		setCellData(row, ".strategy.picardMaxPerHour .ask", askPicardMaxPerHour);
		setCellData(row, ".strategy.picardMaxPerDay .ask", askPicardMaxPerDay);

		return row;
	} else {
		row.remove();
	}
}

function setCellData(row, cellID, value) {

	var input = row.find(cellID);
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

	var input = row.find(cellID);
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

	var input = row.find(cellID);
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

	var nibblerName = row.attr("data-nibblerName");
	var configGroupID = row.attr("data-configGroupID");

	var quoteMaxBookAgeMillis = getCellData(row, ".quoteInst.maxBookAgeMillis input");
	var quoteIsAuctionQuotingEnabled = getCellBool(row, ".quoteInst.isAuctionQuotingEnabled input");
	var quoteIsOnlyAuction = getCellBool(row, ".quoteInst.isOnlyAuction input");
	var quoteAuctionTheoMaxTicksThrough = getCellData(row, ".quoteInst.auctionTheoMaxTicksThrough input");
	var quoteMaxJumpBPS = getCellData(row, ".quoteInst.maxJumpBPS input");
	var quoteBettermentQty = getCellData(row, ".quoteInst.bettermentQty input");
	var quoteBettermentTicks = getCellData(row, ".quoteInst.bettermentTicks input");

	var fxMaxBookAgeMillis = getCellData(row, ".fx.maxBookAgeMillis input");
	var fxMaxJumpBPS = getCellData(row, ".fx.maxJumpBPS input");

	var leanMaxBookAgeMillis = getCellData(row, ".leanInst.maxBookAgeMillis input");
	var leanMaxJumpBPS = getCellData(row, ".leanInst.maxJumpBPS input");
	var leanRequiredQty = getCellData(row, ".leanInst.requiredQty input");
	var leanMaxPapaWeight = getCellData(row, ".leanInst.maxPapaWeight input");
	var leanToQuoteRatio = getCellFloat(row, ".leanInst.leanToQuoteRatio input");
	var leanPriceAdjustment = getCellFloat(row, ".leanInst.leanPriceAdjustment input");

	var additiveIsEnabled = getCellBool(row, ".additiveInst.isEnabled input");
	var additiveMaxSignalAgeMillis = getCellData(row, ".additiveInst.maxSignalAgeMillis input");
	var additiveMinRequiredBPS = getCellData(row, ".additiveInst.minRequiredBPS input");
	var additiveMaxBPS = getCellData(row, ".additiveInst.maxBPS input");

	var bidPlanMinLevelQty = getCellData(row, ".plan.minLevelQty .bid");
	var bidPlanMaxLevelQty = getCellData(row, ".plan.maxLevelQty .bid");
	var bidPlanLotSize = getCellData(row, ".plan.lotSize .bid");
	var bidPlanMaxLevels = getCellData(row, ".plan.maxLevels .bid");
	var bidMinPicardQty = getCellData(row, ".plan.minPicardQty .bid");

	var bidMaxOrdersPerLevel = getCellData(row, ".strategy.maxOrdersPerLevel .bid");
	var bidIsOnlySubmitBestLevel = getCellBool(row, ".strategy.isOnlySubmitBestLevel .bid");
	var bidIsQuoteBettermentOn = getCellBool(row, ".strategy.isQuoteBettermentOn .bid");
	var bidModTicks = getCellData(row, ".strategy.quoteModTicks .bid");
	var bidQuoteFlickerBuffer = getCellData(row, ".strategy.quoteFlickerBuffer .bid");
	var bidQuotePicardMaxTicksThrough = getCellData(row, ".strategy.quotePicardMaxTicksThrough .bid");
	var bidPicardMaxPerSec = getCellData(row, ".strategy.picardMaxPerSec .bid");
	var bidPicardMaxPerMin = getCellData(row, ".strategy.picardMaxPerMin .bid");
	var bidPicardMaxPerHour = getCellData(row, ".strategy.picardMaxPerHour .bid");
	var bidPicardMaxPerDay = getCellData(row, ".strategy.picardMaxPerDay .bid");

	var askPlanMinLevelQty = getCellData(row, ".plan.minLevelQty .ask");
	var askPlanMaxLevelQty = getCellData(row, ".plan.maxLevelQty .ask");
	var askPlanLotSize = getCellData(row, ".plan.lotSize .ask");
	var askPlanMaxLevels = getCellData(row, ".plan.maxLevels .ask");
	var askMinPicardQty = getCellData(row, ".plan.minPicardQty .ask");

	var askMaxOrdersPerLevel = getCellData(row, ".strategy.maxOrdersPerLevel .ask");
	var askIsOnlySubmitBestLevel = getCellBool(row, ".strategy.isOnlySubmitBestLevel .ask");
	var askIsQuoteBettermentOn = getCellBool(row, ".strategy.isQuoteBettermentOn .ask");
	var askModTicks = getCellData(row, ".strategy.quoteModTicks .ask");
	var askQuoteFlickerBuffer = getCellData(row, ".strategy.quoteFlickerBuffer .ask");
	var askQuotePicardMaxTicksThrough = getCellData(row, ".strategy.quotePicardMaxTicksThrough .ask");
	var askPicardMaxPerSec = getCellData(row, ".strategy.picardMaxPerSec .ask");
	var askPicardMaxPerMin = getCellData(row, ".strategy.picardMaxPerMin .ask");
	var askPicardMaxPerHour = getCellData(row, ".strategy.picardMaxPerHour .ask");
	var askPicardMaxPerDay = getCellData(row, ".strategy.picardMaxPerDay .ask");

	ws.send(command("submitChange", [nibblerName, configGroupID, quoteMaxBookAgeMillis, quoteIsAuctionQuotingEnabled, quoteIsOnlyAuction,
		quoteAuctionTheoMaxTicksThrough, quoteMaxJumpBPS, quoteBettermentQty, quoteBettermentTicks, fxMaxBookAgeMillis, fxMaxJumpBPS,
		leanMaxBookAgeMillis, leanMaxJumpBPS, leanRequiredQty, leanMaxPapaWeight, leanToQuoteRatio, leanPriceAdjustment,
		additiveIsEnabled, additiveMaxSignalAgeMillis, additiveMinRequiredBPS, additiveMaxBPS, bidPlanMinLevelQty, bidPlanMaxLevelQty,
		bidPlanLotSize, bidPlanMaxLevels, bidMinPicardQty, bidMaxOrdersPerLevel, bidIsOnlySubmitBestLevel, bidIsQuoteBettermentOn,
		bidModTicks, bidQuoteFlickerBuffer, bidQuotePicardMaxTicksThrough, bidPicardMaxPerSec, bidPicardMaxPerMin, bidPicardMaxPerHour,
		bidPicardMaxPerDay, askPlanMinLevelQty, askPlanMaxLevelQty, askPlanLotSize, askPlanMaxLevels, askMinPicardQty, askMaxOrdersPerLevel,
		askIsOnlySubmitBestLevel, askIsQuoteBettermentOn, askModTicks, askQuoteFlickerBuffer, askQuotePicardMaxTicksThrough,
		askPicardMaxPerSec, askPicardMaxPerMin, askPicardMaxPerHour, askPicardMaxPerDay]));
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

	var parentDiv = checkbox.parent();
	var allInputs = parentDiv.find("input");
	var allPersisted = true;
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

	var header = $("#header");
	var stickyHeader = header.clone();
	stickyHeader.appendTo(header.parent());
	stickyHeader.attr("id", STICKY_HEADER_ID);
	var wind = $(window);
	var headerYPos = header.offset().top;
	var headerXPos = header.offset().left;

	wind.scroll(function () {

		if (headerYPos < wind.scrollTop()) {
			stickyHeader.toggleClass('sticky', true);

			var x = headerXPos - $(this).scrollLeft();
			stickyHeader.css('left', x);
		} else {
			stickyHeader.toggleClass('sticky', false);
		}
	});
}

function setupStickyColumns(rows) {

	rows.each(function () {

		var row = $(this);
		row.find(".stickyColumn").remove();
		var columns = row.find(".configID");

		var stickyColumns = $("<div class=\"stickyColumn\"></div>");

		stickyColumns.appendTo(row);
		columns.clone().appendTo(stickyColumns);
		stickyColumns.removeAttr("id");

		var referenceColumn = columns.filter(".leftMost");

		var wind = $(window);
		var columnXPos = referenceColumn.offset().left;

		wind.scroll(function () {

			if (columnXPos < wind.scrollLeft()) {
				stickyColumns.toggleClass('sticky', true);

				var columnYPos = referenceColumn.offset().top;
				var y = columnYPos - $(this).scrollTop();
				stickyColumns.css("top", y);
				stickyColumns.css("height", referenceColumn.outerHeight());
			} else {
				stickyColumns.toggleClass('sticky', false);
			}
		});
	});
}