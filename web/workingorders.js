var Rows = {};

$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (x) {
		eval(x)
	};

	var cancelAllNonGTC = $("#cancelNonGTC");
	var cancelAllButton = $("#cancelAll");
	var shutdownAllButton = $("#shutdownAll");

	var headerControls = $("#header");
	var minimiseAllButton = headerControls.find(".buttons .minimiseAll");
	minimiseAllButton.unbind().bind("click", function () {
		$(".rows").toggleClass("hidden", true);
	});

	var maximiseAllButton = headerControls.find(".buttons .maximiseAll");
	maximiseAllButton.unbind().bind("click", function () {
		$(".rows").toggleClass("hidden", false);
	});

	$(".masterControls .omsButtons .enableNuclearOptions").die().bind("click", function () {

		var isCancelAllClickable = cancelAllButton.hasClass("isClickable");
		if (!isCancelAllClickable) {

			cancelAllNonGTC.die().bind("click", function () {
				ws.send(command("cancelAllNonGTC"));
			});
			cancelAllButton.unbind().bind("click", function () {
				ws.send(command("cancelAll"));
			});
			shutdownAllButton.unbind().bind("click", function () {
				ws.send(command("shutdownAll"));
			});

			cancelAllNonGTC.toggleClass("isClickable", true);
			cancelAllButton.toggleClass("isClickable", true);
			shutdownAllButton.toggleClass("isClickable", true);
			window.setTimeout(setButtonDisabled(cancelAllNonGTC, cancelAllButton, shutdownAllButton), 2500);
		}
	});


	$("#enderOnlyInput").change(function() {
		$("#workingOrders").toggleClass("hideNoneEnder", this.checked);
	});

	heartbeat();
	setInterval(heartbeat, 2500);
});

function heartbeat() {
	ws.send(command("heartbeat"));
}

function setButtonDisabled(cancelAllGTC, cancelButton, shutdownButton) {

	return function () {

		cancelAllGTC.unbind();
		cancelAllGTC.toggleClass("isClickable", false);

		cancelButton.unbind();
		cancelButton.toggleClass("isClickable", false);

		shutdownButton.unbind();
		shutdownButton.toggleClass("isClickable", false);
	}
}

function addNibbler(server, connectionEstablished) {

	var serverBlock = $('#' + server);
	if (!serverBlock[0]) {
		serverBlock = $("#serverBlockTemplate").clone();
		serverBlock.attr("id", server);
		serverBlock.toggleClass("hidden", false);

		var headerName = serverBlock.find(".serverName");
		headerName.text(server);

		var rowsBlock = serverBlock.find(".rows");
		serverBlock.find(".serverDetails").unbind().bind("click", function () {
			rowsBlock.toggleClass("hidden", !rowsBlock.hasClass("hidden"))
		});

		var cancelNonGTCButton = serverBlock.find(".cancelNonGTC");
		var cancelAllButton = serverBlock.find(".cancelAll");
		var shutdownAllButton = serverBlock.find(".shutdownOMS");

		serverBlock.find(".enableNuclearOptions").die().bind("click", function () {

			var isCancelAllClickable = cancelAllButton.hasClass("isClickable");
			if (!isCancelAllClickable) {

				cancelNonGTCButton.die().bind("click", function () {
					ws.send(command("cancelExchangeNonGTC", [server]));
				});
				cancelAllButton.unbind().bind("click", function () {
					ws.send(command("cancelExchange", [server]));
				});
				shutdownAllButton.unbind().bind("click", function () {
					ws.send(command("shutdownExchange", [server]));
				});

				cancelNonGTCButton.toggleClass("isClickable", true);
				cancelAllButton.toggleClass("isClickable", true);
				shutdownAllButton.toggleClass("isClickable", true);
				window.setTimeout(setButtonDisabled(cancelNonGTCButton, cancelAllButton, shutdownAllButton), 2500);
			}
		});

		$("#workingOrders").append(serverBlock);
		setOrderCount(serverBlock);
	}

	serverBlock.toggleClass("connectionLost", !connectionEstablished);

	return serverBlock;
}

function updateWorkingOrder(key, chainID, instrument, side, price, filledQuantity, quantity, state, orderType, tag, server, isDead) {

	var row = Rows[key];
	var serverBlock = addNibbler(server);

	if (isDead) {
		if (row) {
			delete Rows[key];
			row.remove();
		}
	} else {

		if (!row) {
			row = $("#header").clone().removeAttr("id");
			Rows[key] = row;

			var serverBlock = addNibbler(server);
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
		row.toggleClass("notEnder", tag != "Ender");

		row.find(".cancelOrder").die().bind("click", function () {
			ws.send(command("cancelOrder", [key]));
		});
	}

	setOrderCount(serverBlock);
}

function setOrderCount(server) {

	var orderCountDiv = server.find(".orderCount");
	var orders = server.find(".rows").children().length;
	orderCountDiv.text(orders);
}
