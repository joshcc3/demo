let headerRow;
let table;

let rfqSound;
let atCloseSound;
let sweepSound;
let twapSound;
let tweetSound;
let onFireSound;
let inFlamesSound;
let divsSound;
let unknownSound;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (x) {
		eval(x)
	};

	const hash = document.location.hash.substr(1);
	let communityStr;
	if (0 === hash.length) {
		communityStr = "DM";
	} else {
		communityStr = hash;
	}

	ws.send("subscribeToCommunity," + communityStr)

	table = $("#stockAlerts");
	headerRow = $("#header");

	rfqSound = new Audio("stockAlerts/RFQ.wav");
	atCloseSound = new Audio("stockAlerts/calf-slap.wav");
	sweepSound = new Audio("stockAlerts/sword-schwing.wav");
	twapSound = new Audio("stockAlerts/TWAP.wav");
	tweetSound = new Audio("stockAlerts/tweet.wav");
	onFireSound = new Audio("stockAlerts/on_fire.ogg");
	inFlamesSound = new Audio("stockAlerts/yogaflame.wav");
	divsSound = new Audio("stockAlerts/red_alert_awaiting_orders.wav");
	unknownSound = new Audio("stockAlerts/huh-humm.wav");
});

function stockAlert(timestamp, type, symbol, msg, isOriginal) {

	const id = (type + symbol + timestamp + msg).replace(/ |\/|\.|:/g, "_");
	let row = $('#' + id);

	if (row[0]) {
		row.remove();
	} else {
		row = table.find("#header").clone();
		row.attr("id", id);
	}

	row.addClass(type);

	row.find(".timestamp").text(timestamp);
	row.find(".type").text(type);

	const symbolCell = row.find(".symbol");
	symbolCell.text(symbol);
	symbolCell.unbind().bind("click", function () {
		launchLadder(symbol);
	});

	row.find(".msg").text(msg);
	row.insertAfter(headerRow);

	const rows = table.children();
	if (16 < rows.length) {
		table.children().last().remove();
	}

	if (isOriginal) {
		playSound(type, msg);
	}
}

function playSound(type, msg) {

	let sound;

	if ("RFQ" === type) {
		sound = rfqSound;
	} else if ("AT_CLOSE" === type) {
		sound = atCloseSound;
	} else if ("SWEEP" === type) {
		sound = sweepSound;
	} else if ("TWAP" === type) {
		sound = twapSound;
	} else if ("TWEET" === type) {
		if (msg.includes("ON FIRE")) {
			sound = onFireSound;
		} else if (msg.includes("IN FLAMES")) {
			sound = inFlamesSound;
		} else {
			sound = tweetSound;
		}
	} else if ("DIV" === type) {
		sound = divsSound;
	} else {
		sound = unknownSound;
	}
	if (!sound.readyState) {
		sound.load();
	}
	sound.play();
}


