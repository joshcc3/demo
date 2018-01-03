var ws;

var isSoundsOn = true;
var checkCrossed = false;

var RUSSIA_SSF = /^(RI|SR|SP|GZ|LK|RN|TT|MT|NK|VB|SG|HY|FS|UK|CH|TN|SN|GM|RT|ME|MN)([A-Z])([0-9])$/;

var picards = {};
var queued;

var lastSound = new Date().getTime();

var picardSound;

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
		ws.send('setCheckCrossed,' + (!checkCrossed));
	});

	setInterval(sortPicards, 1000);

});

function setSound(filename) {
	picardSound = new Audio(filename);
}

function playSound() {

	var now = new Date().getTime();
	if (isSoundsOn && picardSound && 1000 < now - lastSound) {
		lastSound = now;

		if (!picardSound.readyState) {
			picardSound.load();
		}
		picardSound.play();
	}
}

function displaySymbol(symbol, listing) {
	if (symbol.indexOf(listing) == -1) {
		return listing + " (" + symbol + ")";
	}
	return symbol;
}

function picard(symbol, listing, side, bpsThrough, price, description, state, inAuction, longPrice) {

	if (!RUSSIA_SSF.test(symbol)) {

		var key = toId(symbol);
		var picard;

		if (picards.hasOwnProperty(key)) {
			picard = picards[key];
		} else {
			if (state == "DEAD") {
				// Don't create a picard just to kill it
				return;
			}
			picard = $('tr.picard.template').clone().removeClass('template');
			picard.click(function () {
				console.log("HERE");
				launchLadderAtPrice(symbol, price);
				ws.send('recenter' + "," + symbol + ",\"" + longPrice + "\"");
			});
			picards[key] = picard;
			$('#picards').append(picard);
			queueSort();
		}

		if (state == "DEAD") {
			delete picards[key];
			picard.remove();
		} else {

			picard.attr('id', key);
			picard.data('bps', parseFloat(bpsThrough));
			picard.find('.symbol').text(displaySymbol(symbol, listing));
			picard.find('.bpsThrough').text(bpsThrough + ' bps');
			picard.find('.price').text(price);
			picard.find('.side').text(side);
			picard.find('.description').text(description);

			picard.toggleClass("live", state == "LIVE");
			picard.toggleClass("fade", state == "FADE");
			picard.toggleClass("BID", !inAuction && side == "BID");
			picard.toggleClass("ASK", !inAuction && side == "ASK");
			picard.toggleClass("BID_AUCTION", inAuction && side == "BID");
			picard.toggleClass("ASK_AUCTION", inAuction && side == "ASK");
		}
	}
}

function setCheckCrossed(check) {
	checkCrossed = check;
	$("#all").val(check ? 'all crosses' : 'picards only');
}

function queueSort() {
	if (!queued) {
		queued = setTimeout(sortPicards, 300);
	}
}

function sortPicards() {
	var sorted = $('#picards').find('tr.picard:not(.template)');
	sorted.sort(function (a, b) {
		return parseInt(10000 * ($(b).data('bps') - $(a).data('bps')));
	});
	sorted.detach().appendTo('#picards');
	queued = null;
}

function toId(symbol) {
	return symbol.replace(/\W/g, '');
}

function test() {
	picard("sym1", "BID", "20.3", "1000.0", "live");
	picard("sym2", "ASK", "0.3", "1000.0", "live");
	picard("sym3", "BID", "30.3", "1000.0", "live");
	picard("sym4", "BID", "1.65", "233.0", "fade");
	picard("sym4", "BID", "1.65", "233.0", "dead");
}
