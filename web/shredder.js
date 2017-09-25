let handler;

// Handler for incoming data

let TAB = 9;
let numLevels = 0;
let pixelsPerSize = 0.05;
let orderSizeAnchor = 10;
let maxOrderSizeInPixels = 400;
let currentLevels = 0;

$(function () {
	ws = connect();
	ws.logToConsole = false;
	handler = new Handler(ws);
	ws.onmessage = handler.msg;
	handler.heartbeat = function (args) {
		handler.send(args[0], args[1], args[2]);
	};

	subscribe();
	setInterval(resizeIfNecessary, 200);

	$(document).keydown(function (e) {
		const keyCode1 = (e.keyCode ? e.keyCode : e.which);
		if (keyCode1 == TAB) {
			e.preventDefault();
		}
	});
});

function draw(levels, ordersPerRow) {
	console.log(`Redrawing ${levels} levels with ${ordersPerRow} orders per row`);
	currentLevels = levels;
	let rows = $("#rows");
	rows.find(".row").remove();

	for (let i = 0; i < levels; i++) {
		let bookRow = $("#row_template").clone().removeClass("template");
		bookRow.attr('id', 'row_' + i);
		bookRow.find('.price').attr('id', 'price_' + i).text(' ');
		bookRow.find('.side').attr('id', 'side_' + i).text(' ');

		let shredded_orders = bookRow.find('.shredded');
		shredded_orders.attr('id', 'shredded_' + i).text(' ');

		for (let j = 0; j < ordersPerRow; j++) {
			shredded_orders.append($('<div \>', {
				id: `order_${i}_${j}`,
				class: "blank_order"
			}))
		}

		rows.append(bookRow);
	}
}

function subscribe() {

	let symbolEnd = document.location.hash.indexOf(';', 0);
	if (symbolEnd < 0) {
		symbolEnd = document.location.hash.length;
	}
	const symbol = document.location.hash.substr(1, symbolEnd - 1);
	numLevels = calcLevels();

	if (0 < symbolEnd) {
		const ladderSwitch = document.location.hash.substr(symbolEnd + 1);
		handler.send("shredder-subscribe", symbol, numLevels, ladderSwitch);
	} else {
		handler.send("shredder-subscribe", symbol, numLevels);
	}
	window.addEventListener("hashchange", function () {
		window.location.reload();
	}, false);
}

function resizeIfNecessary() {
	if (calcLevels() != numLevels) {
		console.log("subscribing", calcLevels(), numLevels, $(window).height());
		subscribe();
	}

	width = $(document).width() - 100;
}

function calcLevels() {
	return parseInt(($(window).height() - 23) / 15);
}


function orderWidth(qty) {
	if (qty > orderSizeAnchor * 5) {
		orderSizeAnchor = qty;
		pixelsPerSize = maxOrderSizeInPixels / orderSizeAnchor;

		resizeOrders();
	}

	let retWidth = Math.ceil(qty * pixelsPerSize);
	return Math.max(20, retWidth);
}


function order(qty, orderId) {
	let width = orderWidth(qty);

	return $('<div />', {
		id: `order_${orderId}`,
		class: "order",
		text: textToDisplay(width,  qty),
		css: {
			width: `${width}px`
		},
	});
}