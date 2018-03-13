let handler;

// Handler for incoming data

let TAB = 9;
let numLevels = 0;
let pixelsPerSize = 0.05;
let orderSizeAnchor = 10;
let maxOrderSizeInPixels = 400;
let currentLevels = 0;
let currentNumberOfColumns = 0;
let currentNumberOfRows = 0;

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
	$(window).resize(updateWidth);

	$(document).keydown(function (e) {
		const keyCode1 = (e.keyCode ? e.keyCode : e.which);
		if (keyCode1 == TAB) {
			e.preventDefault();
		}
	});
});

function addOrdersToRow(startingColumn, ordersPerRow, row, shreddedOrders) {
	for (let column = startingColumn; column < ordersPerRow; column++) {
		let orderId = `order_${row}_${column}`;
		let order = $('<div \>', {
			id: orderId,
			class: "blank_order"
		});

		var showPopupBox = function () {
			var summaryHoverBox = $("#orderSummaryHoverBox");
			summaryHoverBox.find("#quantityInFront").text(handler.getData(orderId, 'vIF'));

			summaryHoverBox.find("#orderQuantity").text(handler.getData(orderId, 'quantity'));

			// .text(undefined) does not change the existing text so we have to manually set it to an empty string
			summaryHoverBox.find("#tag").text(handler.getData(orderId, 'tag'));
			summaryHoverBox.find("#orderType").text(handler.getData(orderId, 'orderType'));
			showHoverBoxByElement(order, summaryHoverBox);
			summaryHoverBox.show();
		};

		var hidePopupBox = function () {
			$("#orderSummaryHoverBox").hide();
		};

		order.hover(showPopupBox, hidePopupBox);
		shreddedOrders.append(order)
	}
}

function removeOrders(row, from) {
	$(`#order_${row}_${from}`).nextAll().remove();
}

function updateWidth() {
	if ($("#shredded_0").width()) {
		handler.send("update", $("#shredded_0").width());
	}
}

function draw(levels, ordersPerRow) {
	console.log(`Redrawing ${levels} levels with ${ordersPerRow} orders per row`);
	currentLevels = levels;
	let rows = $("#rows");

	$(`#row_${levels-1}`).nextAll().remove();

	for (let i = 0; i < levels; i++) {
		if (i < currentNumberOfRows) {
			if (ordersPerRow < currentNumberOfColumns ) {
				removeOrders(i,  ordersPerRow);
			}
			let shreddedOrders = $(`#shredded_${i}`);
			addOrdersToRow(currentNumberOfColumns, ordersPerRow, i,  shreddedOrders);
		} else {
			let bookRow = $("#row_template").clone().removeClass("template");
			bookRow.attr('id', 'row_' + i);
			bookRow.find('.price').attr('id', 'price_' + i).text(' ');
			bookRow.find('.side').attr('id', 'side_' + i).text(' ');
			let shreddedOrders = bookRow.find('.shredded');
			shreddedOrders.attr('id', 'shredded_' + i).text(' ');
			addOrdersToRow(0, ordersPerRow, i,  shreddedOrders);
			rows.append(bookRow);
		}
	}

	currentNumberOfColumns = ordersPerRow;
	currentNumberOfRows = levels;
	updateWidth();
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
		subscribe();
	}

	width = $(document).width() - 100;
}

function calcLevels() {
	return parseInt(($(window).height() - 16) / 15);
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

function showHoverBoxByElement(element, hoverElement) {
	hoverElement.css({top: element.offset().top - hoverElement.outerHeight() - 2, left: element.offset().left});
}