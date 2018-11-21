var headerRow;
var table;

var rfqSound;
var etfRfqSound;
var atCloseSound;
var sweepSound;
var twapSound;
var tweetSound;
var unknownSound;


var AutoOpenRFQ = false;

$(function () {
    ws = connect();
    ws.logToConsole = true;
    ws.onmessage = function (x) {
        eval(x)
    };

    table = $("#stockAlerts");
    headerRow = $("#header");

    rfqSound = new Audio("stockAlerts/RFQ.wav");
    etfRfqSound = new Audio("sounds/honeybadgerdoesntcare.wav");
    atCloseSound = new Audio("stockAlerts/calf-slap.wav");
    sweepSound = new Audio("stockAlerts/sword-schwing.wav");
    twapSound = new Audio("stockAlerts/TWAP.wav");
    tweetSound = new Audio("stockAlerts/tweet.wav");
    bigRfqSound = new Audio("stockAlerts/pulp.wav");
    unknownSound = new Audio("stockAlerts/huh-humm.wav");

    setTimeout(function() {
	if (localStorage['auto-open-rfq']) {
	    setAutoOpen(JSON.parse(localStorage['auto-open-rfq']));
	}
    }, 2000);

    $(".autoButton").unbind().bind("click", function() {
	setAutoOpen(!AutoOpenRFQ);
    });
});

function setAutoOpen(autoOpen) {
    AutoOpenRFQ = autoOpen;
    localStorage['auto-open-rfq'] =  JSON.stringify(autoOpen);
    $(".autoButton").val(AutoOpenRFQ ? "auto":"no-auto");
}

function stockAlert(timestamp, type, symbol, msg, isOriginal) {

    var id = (type + symbol + timestamp).replace(/ |\/|\.|:/g, "_");
    var row = $('#' + id);

    if (row[0]) {
        row.remove();
    } else {
        row = table.find("#header").clone();
        row.attr("id", id);
    }

    row.addClass(type);

    row.find(".timestamp").text(timestamp);
    row.find(".type").text(type);

    var symbolCell = row.find(".symbol");
    symbolCell.text(symbol);
    symbolCell.unbind().bind("click", function () {
        launchLadder(symbol);
    });

    row.find(".msg").text(msg);
    row.insertAfter(headerRow);

    var rows = table.children();
    if (16 < rows.length) {
        table.children().last().remove();
    }

    if (isOriginal) {
        playSound(type);
    }

    if(type.includes("ETF_RFQ")) {
        var o = symbol.split(" ")[0];
        var origSymbol = o.slice(0,-2) + " " + o.slice(-2);
        if (AutoOpenRFQ) {
            launchLadder(symbol, true);
            launchLadder(origSymbol, true);
        }
    }
}

function playSound(type) {

    var sound;
    if (type.startsWith("BIG_")) {
    	sound = bigRfqSound;
	} else if ("RFQ" == type) {
        sound = rfqSound;
    } else if ("AT_CLOSE" == type) {
        sound = atCloseSound;
    } else if ("SWEEP" == type) {
        sound = sweepSound;
    } else if ("TWAP" == type) {
        sound = twapSound;
    } else if ("ETF_RFQ" == type) {
        sound = etfRfqSound;
    } else if ("TWEET" == type) {
    	sound = tweetSound;
	} else {
        sound = unknownSound;
    }
    if (!sound.readyState) {
        sound.load();
    }
    sound.play();
}


