const DISPLAY_THRESHOLD = "display-threshold"

let ws;

let isSoundsOn = true;
let checkCrossed = true;

const RUSSIA_SSF = /^(SR|SP|GZ|LK|RN|TT|MT|NK|VB|SG|HY|FS|UK|CH|TN|SN|GM|RT|ME|MN)([A-Z])([0-9])$/;

const picards = {};
let queued;

let lastSound = new Date().getTime();

let picardSound;
let sortByValue = "opportunitySize";
let displayThreshold = 2;
let hideRfq = false;

$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (msg) {
		eval(msg);
	};

	$("#sound").bind('click', function () {
		if (isSoundsOn) {
			isSoundsOn = false;
			$("#sound").val('sound off');
		} else {
			isSoundsOn = true;
			$("#sound").val('sound on');
			playSound();
		}
	});

	$("#all").bind('click', function () {
		checkCrossed = !checkCrossed;
		$(this).val(checkCrossed ? 'all crosses' : 'picards only');
	});

	$("#sortBy").click(() => {
		if (sortByValue === "opportunitySize") {
			sortByValue = "bps";
			$("#sortBy").val("bps");
		} else {
			sortByValue = "opportunitySize";
			$("#sortBy").val("notional");
		}
	});

	$("#filterValue").change(() => {
		displayThreshold = parseFloat($("#filterValue").val());
		localStorage[DISPLAY_THRESHOLD] = displayThreshold;
		$(".picard").each((index, row) => {
			let isPicardRow = $(row).hasClass("rfqPicard");
			$(row).toggleClass("hidden", (isPicardRow && hideRfq) || $(row).data("opportunitySize") < displayThreshold);
		});
	});

	$("#showRfq").change(() => {
		hideRfq = !$("#showRfq").attr('checked');
		$(".rfqPicard").each((index, row) => {
			$(row).toggleClass("hidden", hideRfq || $(row).data("opportunitySize") < displayThreshold);
		});
	});

	const picardsDiv = $("#picards");
	$("#hideOPXLFilterList").change(() => {
		const isHideOPXLFilterList = $("#hideOPXLFilterList").attr('checked');
		picardsDiv.toggleClass("hideOPXLFilterList", isHideOPXLFilterList);
	});

	setInterval(sortPicards, 1000);

	if (localStorage[DISPLAY_THRESHOLD]) {
		displayThreshold = parseFloat(localStorage[DISPLAY_THRESHOLD]);
		$("#filterValue").val(displayThreshold);
	}
});

function setSound(filename) {
	picardSound = new Audio(filename);
}

function playSound() {

	const now = new Date().getTime();
	if (isSoundsOn && picardSound && 1000 < now - lastSound) {
		lastSound = now;

		if (!picardSound.readyState) {
			picardSound.load();
		}
		picardSound.play();
	}
}

function displaySymbol(symbol, listing) {
	if (symbol.indexOf(listing) === -1) {
		return listing + " (" + symbol + ")";
	}
	return symbol;
}

function picard(symbol, listing, side, bpsThrough, opportunitySize, ccy, price, description, state, inAuction, isPlaySound,
	isOnOPXLFilterList, isRunnable) {

	if (!RUSSIA_SSF.test(symbol)) {

		const key = toId(symbol);
		let picard;
		let isRfq = symbol.endsWith(" RFQ");

		if (picards.hasOwnProperty(key)) {

			picard = picards[key];

		} else if (state !== "DEAD") {

			picard = $('tr.picard.template').clone().removeClass('template');
			picard.click(function () {
				let priceText = picard.find('.price').text();
				launchLadderAtPrice(symbol, priceText);
				ws.send('recenter' + "," + symbol + ",\"" + Math.round(parseFloat(priceText) * 1e9) + "\"");
				if (isRfq) {
					let origSymbol = symbol.split(" ")[0];
					let origSuffix = origSymbol.slice(origSymbol.length - 2, origSymbol.length);
					let origPrefix = origSymbol.slice(0, origSymbol.length - 2);
					launchLadderAtPrice(origPrefix + " " + origSuffix, priceText);
				}
			});
			picards[key] = picard;
			$('#picards').append(picard);
			queueSort();
		} else {
			return;
		}

		if (state === "DEAD") {
			delete picards[key];
			picard.remove();
		} else {

			let opportunity = parseFloat(opportunitySize.replace(",", ""));
			picard.attr('id', key);
			picard.data('bps', parseFloat(bpsThrough));
			picard.data('opportunitySize', opportunity);
			picard.find('.symbol').text(displaySymbol(symbol, listing));
			picard.find('.bpsThrough').text(bpsThrough + ' bps');
			picard.find('.price').text(price);
			picard.find('.side').text(side);
			picard.find('.opportunitySize').text(opportunitySize + " " + ccy);
			picard.find('.description').text(description);

			picard.toggleClass("live", state === "LIVE");
			picard.toggleClass("fade", state === "FADE");
			picard.toggleClass("BID", !inAuction && side === "BID");
			picard.toggleClass("ASK", !inAuction && side === "ASK");
			picard.toggleClass("BID_AUCTION", inAuction && side === "BID");
			picard.toggleClass("ASK_AUCTION", inAuction && side === "ASK");
			picard.toggleClass("rfqPicard", isRfq);

			let bigEnough = opportunity > displayThreshold;
			picard.toggleClass("hidden", !bigEnough || (hideRfq && isRfq));

			if (bigEnough && isPlaySound && checkCrossed) {
				playSound();
			}

			picard.toggleClass("isOnOPXLFilterList", isOnOPXLFilterList);
			picard.toggleClass("isNotRunnable", !isRunnable);
		}
	}
}

function queueSort() {
	if (!queued) {
		queued = setTimeout(sortPicards, 300);
	}
}

function sortPicards() {
	let sorted = $('#picards').find('tr.picard:not(.template)');
	sorted.sort(function (a, b) {
		return $(b).data(sortByValue) - $(a).data(sortByValue);
	});
	sorted.detach().appendTo('#picards');
	queued = null;
}

function toId(symbol) {
	return symbol.replace(/\W/g, '');
}

function test() {
	picard("sym1", "CHIX", "BID", "20.3", "400", "EUR", "1000.0", "Description", "live", false, false, false);
	picard("sym2", "CHIX", "ASK", "0.3", "350", "EUR", "1000.0", "Description", "live", false, false, true);
	picard("sym3", "CHIX", "BID", "30.3", "0", "EUR", "1000.0", "Description", "live", false, false, true);
	picard("sym4", "CHIX", "BID", "1.65", "400", "EUR", "233.0", "Description", "fade", false, false, true);
	picard("sym4", "CHIX", "BID", "1.65", "700", "EUR", "233.0", "Description", "dead", false, false, true);
}
