Rows = {};

$(function () {
	ws = connect();
	ws.logToConsole = false;
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
	var cancelAllButton = $("button.cancelAll");
	cancelAllButton.unbind();
	cancelAllButton.toggleClass("isClickable", false);
}

function updateWorkingOrder(key, chainID, instrument, side, price, filledQuantity, quantity, state, orderType, tag, server, isDead) {

	var row = Rows[key];

	if (isDead) {
		if (row) {
			delete Rows[key];
			row.remove();
		}
	} else {

		if (!row) {
			row = $("#header").clone().removeAttr("id");
			Rows[key] = row;

			var serverBlock = $('#' + server);
			if (!serverBlock[0]) {
				serverBlock = $("#serverBlockTemplate").clone();
				serverBlock.attr("id", server);
				serverBlock.toggleClass("hidden", false);

				var headerName = serverBlock.find(".serverName");
				headerName.text(server);

				var rowsBlock = serverBlock.find(".rows");
				headerName.unbind().bind("click", function () {
					rowsBlock.toggleClass("hidden", !rowsBlock.hasClass("hidden"))
				});

				$("#workingOrders").append(serverBlock);
			}

			var rows = serverBlock.find(".rows");
			rows.append(row);
		}

		row.find(".button").toggleClass("hidden", false);

		row.find(".key").text(key);

		var symbolCell = row.find(".symbol");
		symbolCell.text(instrument);
		symbolCell.die().bind("click", function () {
			launchLadder(instrument);
		});

		row.find(".side").text(side);
		row.find(".price").text(price);
		row.find(".filledQuantity").text(filledQuantity);
		row.find(".quantity").text(quantity);
		row.find(".state").text(state);
		row.find(".orderType").text(orderType);
		row.find(".tag").text(tag);
		row.find(".chainID").text(chainID);
		row.find(".server").text(server);

		row.toggleClass("bid", side == "BID");
		row.toggleClass("offer", side == "OFFER");

		row.find(".cancelOrder").die().bind("click", function () {
			ws.send(command("cancelOrder", [key]));
		});
	}
}
