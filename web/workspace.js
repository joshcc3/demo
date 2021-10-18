let ws;

const ladderWidth = 245;
const ladderHeight = 332;

ws = connect("ws://" + document.location.host + "/workspace/ws/");
ws.logToConsole = false;
ws.onmessage = function (x) {
	eval(x);
};

let LastRows = 0;
let LastCols = 0;
let NumBoxes = LastRows * LastCols;

const Frames = [];
const Ladders = {};
let Locked = false;
let Sets = false;

$(function () {

	const parameters = parseHashcode();

	if (parameters.locking) {
		toggleLock();
	}

	if (parameters.sets) {
		toggleSets();
	}

	setInterval(function () {

		// Compute number of rows and columns
		const height = document.body.offsetHeight;
		const width = $(window).width();

		const rows = Math.floor(height / ladderHeight);
		const cols = Math.floor(width / ladderWidth);

		if (rows !== LastRows || cols !== LastCols) {
			LastRows = rows;
			LastCols = cols;
			NumBoxes = LastRows * LastCols;

			doLayout();
			doLadders();

		}

	}, 250);

	$("#lock").click(function () {
		toggleLock();
	});

	$("#sets").click(function () {
		toggleSets();
	});

	window.onhashchange = function () {
		doLadders();
	}

});

function sendReplace(from, to) {
	ws.send(command("replace", [from, to]));
}

function replace(from, to) {
	document.location.hash = document.location.hash.split(from).join(to);
}

function refreshHashCode() {
	const params = parseHashcode();
	document.location.hash = generateHashcode(params.symbols);
}

function doLayout() {

	const template = $('.ladder.template').clone().removeClass('template');
	template.width(ladderWidth);
	template.height(ladderHeight);

	const existing = $('.ladder:not(.template)');
	const numExisting = existing.size();

	if (numExisting > NumBoxes) {
		const spliced = Frames.splice(NumBoxes);
		spliced.forEach(function (frame) {
			frame.remove();
		});
	}

	for (let i = numExisting; i < NumBoxes; i++) {
		const frame = template.clone();
		$('#workspace').append(frame);
		Frames.push(frame);
	}

}

function doLadders() {
	const parameters = parseHashcode();
	const symbols = parameters.symbols;
	for (let i = 0; i < Math.min(symbols.length, Frames.length); i++) {

		const frameTarget = symbols[i];

		let symbolEnd = frameTarget.indexOf(';', 0);
		let isStack;
		if (symbolEnd < 0) {
			symbolEnd = frameTarget.length;
			isStack = false;
		} else {
			const symbolSwitch = frameTarget.substr(symbolEnd + 1);
			isStack = symbolSwitch === 'S'
		}

		if (isStack) {
			const symbol = frameTarget.substr(0, symbolEnd);
			setFrame(Frames[i], symbol, true);
		} else {
			setFrame(Frames[i], frameTarget, false);
		}
	}
}

function moveFrameToFront(index) {
	if (index === 0) {
		return;
	}
	if (index < Frames.length) {
		const el = Frames[index];
		Frames.splice(index, 1);
		Frames.unshift(el);
		el.detach();
		$("#workspace").prepend(el);
	}
}

function addSymbol(symbol) {

	const parameters = parseHashcode();
	const symbols = parameters.symbols;

	const index = findSymbol(symbols, symbol);
	if (-1 !== index) {
		// Move the symbol to the front and move its frame
		moveFrameToFront(index, Frames);
		symbols.splice(index, 1);
		symbols.unshift(symbol);
	} else {
		// Add the symbol to the front
		symbols.unshift(symbol);
		// Roll the last frame around to the front
		moveFrameToFront(Frames.length - 1, Frames);
	}
	document.location.hash = generateHashcode(symbols);
	doLadders();
}

function findSymbol(symbols, frameTarget) {

	let symbolEnd = frameTarget.indexOf(';', 0);
	let isStack;
	if (symbolEnd < 0) {
		symbolEnd = frameTarget.length;
		isStack = false;
	} else {
		const symbolSwitch = frameTarget.substr(symbolEnd + 1);
		isStack = symbolSwitch === 'S'
	}

	if (isStack) {
		return symbols.indexOf(frameTarget);
	} else {

		const symbol = frameTarget.substr(0, symbolEnd);
		for (let i = 0; i < Math.min(symbols.length, Frames.length); i++) {

			const symbolsTarget = symbols[i];
			const symbolsTargetEnd = symbolsTarget.indexOf(';', 0);
			if (symbolsTargetEnd < 0) {
				if (symbol === symbolsTarget) {
					return i;
				}
			} else {
				const symbolsTargetSwitch = symbolsTarget.substr(symbolsTargetEnd + 1);
				if ('S' !== symbolsTargetSwitch && symbol === symbolsTarget.substr(0, symbolsTargetEnd)) {
					return i;
				}
			}
		}
		return -1;
	}
}

function toggleLock() {
	Locked = !Locked;
	ws.send(command(Locked ? "lock" : "unlock", []));
	$("#lock").toggleClass("enabled", Locked);
	refreshHashCode();
}

function toggleSets() {
	Sets = !Sets;
	ws.send(command(Sets ? "setify" : "unsetify", []));
	$("#sets").toggleClass("enabled", Sets);
	refreshHashCode();
}

// -- utilities

function parseHashcode() {
	const hash = document.location.hash.substr(1).replace(/%20/g, " ");
	let symbols, locking, sets;
	locking = hash.indexOf("!") !== -1;
	sets = hash.indexOf("*") !== -1;
	symbols = hash.split("!").join("").split("*").join("").split(",");
	if (symbols.length === 1 && symbols[0] === "") {
		symbols = [];
	}
	return {symbols: symbols, locking: locking, sets: sets};
}

function generateHashcode(symbols) {
	return symbols.join(",")
		+ (Locked ? "!" : "")
		+ (Sets ? "*" : "");
}

function getUrl(symbol) {
	const port = parseInt(document.location.port);
	const host = document.location.hostname;
	return 'http://' + host + ':' + (port - 1) + '/ladder#' + symbol;
}

function setFrame(ladder, symbol, isStack) {

	const ladderDiv = $(ladder);

	let idSymbol;
	let urlSymbol;
	if (isStack) {
		idSymbol = symbol + "-S";
		urlSymbol = symbol + ";S";
	} else {
		idSymbol = symbol;
		urlSymbol = symbol;
	}

	if (ladderDiv.attr("id") !== symbolId(idSymbol)) {
		ladderDiv.attr("id", symbolId(idSymbol));
		ladderDiv.find(".frame").attr("src", getUrl(urlSymbol));
	}
}

function symbolId(symbol) {
	return symbol.replace(/\W/g, '_');
}
