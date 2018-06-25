$(function () {
	ws = connect();
	ws.logToConsole = true;
	ws.onmessage = function (x) {
		eval(x);
	};
	const hash = document.location.hash.substr(1);
	const symbol = hash.split(",")[0];
	const price = hash.split(",")[1];
	ws.send(command('subscribe', [symbol, price]));
	setInterval(highlightChangedQty, 250);
});

function getRow(chainId) {
	const orders = $("#orders");
	let row;
	if (orders.find('#' + chainId).size() > 0) {
		row = orders.find('#' + chainId);
	} else {
		row = orders.find('.template').clone().removeClass('template');
		orders.append(row);
	}
	return row;
}

function getKey(fromServer, order) {
	return fromServer + "_" + order.chainId;
}

function orders(os) {
	$("#orders").find("tr:not(.template,.header,.managed)").toggleClass("invisible", true);
	$(os).each(function (i, x) {
		const order = x.workingOrderUpdate;
		const key = getKey(x.fromServer, order);
		const row = getRow(key);
		row.addClass('order').addClass('regular');
		row.toggleClass('BID', order.side == 'BID');
		row.toggleClass('OFFER', order.side == 'OFFER');
		row.find('.remainingQty').text(order.totalQuantity - order.filledQuantity);
		row.find('.qtyBox').data('qty', order.totalQuantity - order.filledQuantity);
		row.find('.type').text(order.workingOrderType);
		row.find('.tag').text(order.tag);
		row.attr('id', key);
		row.toggleClass('invisible', order.workingOrderState == 'DEAD');
		row.find('.go').unbind().bind('click', function () {
			fixQuantity(row);
			ws.send(command('modifyQuantity', [order.symbol, key, parseInt(row.find('.qtyAdjustment').text())]));
		});
		row.find('.cancel').unbind().bind('click', function () {
			ws.send(command('cancelOrder', [order.symbol, key]));
		});
	})
}

function managedOrders(os) {
	$("#orders").find("tr:not(.template,.header,.regular)").toggleClass("invisible", true);
	$(os).each(function (i, x) {
		const order = x.update;
		const key = x.key;
		const row = getRow(key);
		row.addClass('order').addClass('managed');
		row.toggleClass('BID', order.order.side == 'BUY');
		row.toggleClass('OFFER', order.order.side == 'SELL');
		row.find('.remainingQty').text(order.remainingQty);
		row.find('.qtyBox').data('qty', order.remainingQty);
		row.find('.qtyBox').attr('disabled', true);
		row.find('.type').text("MANAGED");
		row.find('.tag').text("MANAGED");
		row.attr('id', key);
		row.toggleClass('invisible', order.dead);
		row.find('.go').toggleClass('invisible', true);
		row.find('.cancel').unbind().bind('click', function () {
			ws.send(command('cancelManagedOrder', [x.symbol, key]));
		});
	})
}

function fixQuantity(x) {

	const qtyAdjustment = x.find('.qtyAdjustment').text();
	const fixed = qtyAdjustment.replace(/[^0-9]/g, "");
	x.find('.qtyAdjustment').text(fixed);
}

function highlightChangedQty() {
	const boxes = $('#orders').find("tr:not(.template,.header)").find(".qtyBox");
	boxes.each(function (i, x) {
		x = $(x);
		x.toggleClass('edited', x.data('qty') != x.find('.remainingQty').text());
	});
}