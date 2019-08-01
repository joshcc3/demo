
let symbol;

$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (x) {
		eval(x);
	};
	const hash = document.location.hash.substr(1);
	let arguments = hash.split(",");
	symbol = arguments[0];
	const price = arguments[1];
	const bidPrice = arguments[2];
	const askPrice = arguments[3];
	ws.send(command("subscribe", [symbol, price, bidPrice, askPrice]));
	setInterval(highlightChangedQty, 250);
});

function getRow(chainId) {
	const orders = $("#orders");
	let row;
	if (orders.find("#" + chainId).size() > 0) {
		row = orders.find("#" + chainId);
	} else {
		row = orders.find(".template").clone().removeClass("template");
		orders.append(row);
	}
	return row;
}

function getKey(fromServer, order) {
	return fromServer + "_" + order.chainId;
}

function orders(os) {
	$("#orders").find("tr:not(.template,.header,.managed)").toggleClass("invisible", true);
	$(os).each(function (i, order) {

		const key = order["id"];
		const isBid = "BID" === order["side"];
		const remainingQty = order["remainingQty"];
		const orderType = order["type"];
		const tag = order["tag"];
		const price = order["price"];
		const row = getRow(key);

		row.addClass("order").addClass("regular");
		row.attr("id", key);
		row.toggleClass("BID", isBid);
		row.toggleClass("OFFER", !isBid);
		row.find(".remainingQty").text(remainingQty);
		row.find(".qtyBox").data("qty", remainingQty);
		row.find(".type").text(orderType);
		row.find(".tag").text(tag);
		row.find(".price").text(price);
		row.toggleClass("invisible", false);

		row.find(".go").unbind().bind("click", function () {
			fixQuantity(row);
			ws.send(command("modifyQuantity", [symbol, key, parseInt(row.find(".qtyAdjustment").text())]));
		});
		row.find(".cancel").unbind().bind("click", function () {
			ws.send(command("cancelOrder", [symbol, key]));
		});
	})
}

function managedOrders(os) {
	$("#orders").find("tr:not(.template,.header,.regular)").toggleClass("invisible", true);
	$(os).each(function (i, x) {
		const order = x.update;
		const key = x.key;
		const row = getRow(key);
		row.addClass("order").addClass("managed");
		row.toggleClass("BID", order.order.side == "BUY");
		row.toggleClass("OFFER", order.order.side == "SELL");
		row.find(".remainingQty").text(order.remainingQty);
		row.find(".qtyBox").data("qty", order.remainingQty);
		row.find(".qtyBox").attr("disabled", true);
		row.find(".type").text("MANAGED");
		row.find(".tag").text("MANAGED");
		row.find(".price").text(order.order.price / 1e9);
		row.attr("id", key);
		row.toggleClass("invisible", order.dead);
		row.find(".go").toggleClass("invisible", true);
		row.find(".cancel").unbind().bind("click", function () {
			ws.send(command("cancelManagedOrder", [x.symbol, key]));
		});
	})
}

function fixQuantity(x) {

	const qtyAdjustment = x.find(".qtyAdjustment").text();
	const fixed = qtyAdjustment.replace(/[^0-9]/g, "");
	x.find(".qtyAdjustment").text(fixed);
}

function highlightChangedQty() {
	const boxes = $("#orders").find("tr:not(.template,.header)").find(".qtyBox");
	boxes.each(function (i, x) {
		x = $(x);
		x.toggleClass("edited", x.data("qty") != x.find(".remainingQty").text());
	});
}