Rows = {};

$(function () {
	ws = connect();
	ws.logToConsole = true;
	ws.onmessage = function (x) {
		eval(x)
	};

	$("button.cancelNonGTC").die().bind("click", function () {
		ws.send(command("cancelNonGTC"));
	});

	var cancelAllButton = $("button.cancelAll");

	$("button.enableCancelAll").die().bind("click", function () {

		var isClickable = cancelAllButton.hasClass("isClickable");
		if (!isClickable) {
			cancelAllButton.unbind().bind("click", function () {
				ws.send(command("cancelAll"));
			});
			cancelAllButton.toggleClass("isClickable", true);
			window.setTimeout(setButtonDisabled, 2500);
		}
	});
});

function setButtonDisabled() {
	console.log("RUN");
	var cancelAllButton = $("button.cancelAll");
	cancelAllButton.unbind();
	cancelAllButton.toggleClass("isClickable", false);
}

function updateWorkingOrder(key, instrument, side, price, filledQuantity, quantity, state, orderType, tag, server, isDead) {

	var row = Rows[key];

	if (isDead) {
		if (row) {
			delete Rows[key];
			row.remove();
		}
	} else {

		if (!row) {
			row = $("table#workingOrders tr.template").clone().removeClass("template");
			Rows[key] = row;
			$("table#workingOrders").append(row);
		}

		row.find(".key").text(key);
		row.find(".instrument").text(instrument);
		row.find(".side").text(side);
		row.find(".price").text(price);
		row.find(".filledQuantity").text(filledQuantity);
		row.find(".quantity").text(quantity);
		row.find(".state").text(state);
		row.find(".orderType").text(orderType);
		row.find(".tag").text(tag);
		row.find(".server").text(server);

		row.toggleClass("bid", side == "BID");
		row.toggleClass("offer", side == "OFFER");

		row.find(".cancelOrder").die().bind("click", function () {
			ws.send(command("cancelOrder", [key]));
		});
	}
}
