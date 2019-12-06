var ws;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (msg) {
		eval(msg);
	};

	const sideSelect = $("#sideSelect");
	setSelectColour(sideSelect);

	$("#addOrder").unbind().bind("click", function () {
		const symbol = $("#symbolInput").val();
		const side = sideSelect.val();
		const qty = $("#qty").val();
		ws.send(command("createOrderLine", [symbol, side, qty]));
	});

	$("#allStart").unbind().bind("click", sendOrders);

	$("#bettermentSearch").unbind().bind("click", searchForBettermentOrders)
});

function printError(msg) {

	const errorDiv = $("#errorMessages");
	errorDiv.text(msg);
	errorDiv.removeClass("hidden");
}

function addPricedOrder(symbol, side, price, qty) {

	const row = addOrder(symbol, side, qty);

	const priceField = row.find(".priceInput");
	priceField.val(price);
}

function addOrder(symbol, side, qty) {

	const errorDiv = $("#errorMessages");
	errorDiv.toggleClass("hidden", true);

	const row = $(".template").clone();
	row.removeClass("template");
	row.removeClass("hidden");

	const symbolField = row.find(".symbol");
	const sideSelect = row.find(".sideSelect");
	const qtyField = row.find(".qtyInput");
	const deleteButton = row.find(".delete");

	symbolField.text(symbol);
	sideSelect.val(side);
	qtyField.val(qty);

	symbolField.bind('click', function () {
		launchLadder(symbol);
	});

	deleteButton.unbind().bind("click", function () {
		row.remove();
	});

	const orders = $("#orders");
	orders.append(row);

	setSelectColour(sideSelect);

	return row;
}

function sendOrders() {

	const orderRows = $("#orders").find(".orderRow");
	const orderCmds = [];

	orderRows.each(function () {

		const orderRow = $(this);
		console.log("ROW", this);
		const order = {
			symbol: orderRow.find(".symbol").text(),
			side: orderRow.find(".sideSelect").val(),
			price: orderRow.find(".priceInput").val(),
			qty: orderRow.find(".qtyInput").val(),
		};

		orderCmds.push(order);
	});

	ws.send(command("sendOrders", [orderCmds]));
}

function searchForBettermentOrders() {

	clearOrders();
	ws.send(command("createOrderForAllBettermentOrders"));
}

function clearOrders() {

	const orderRows = $("#orders").find(".orderRow");
	orderRows.remove();
}

function setSelectColour(select) {

	select.change(function () {
		const selectedItem = $(this).find("option:selected");
		$(this).css("backgroundColor", selectedItem.css("backgroundColor"));
	});

	const selectedItem = select.find("option:selected");
	$(select).css("backgroundColor", selectedItem.css("backgroundColor"));
}
