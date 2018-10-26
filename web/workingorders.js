const Rows = {};

let disconnectionSound;

$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (x) {
		eval(x)
	};

	disconnectionSound = new Audio("sounds/yuranass.wav");

	const cancelAllNonGTC = $("#cancelNonGTC");
	const cancelAllButton = $("#cancelAll");
	const shutdownAllButton = $("#shutdownAll");

	const headerControls = $("#header");
	const minimiseAllButton = headerControls.find(".buttons .minimiseAll");
	minimiseAllButton.unbind().bind("click", function () {
		$(".rows").toggleClass("hidden", true);
	});

	const maximiseAllButton = headerControls.find(".buttons .maximiseAll");
	maximiseAllButton.unbind().bind("click", function () {
		$(".rows").toggleClass("hidden", false);
	});
	cancelAllNonGTC.unbind().bind("click", function () {
		ws.send(command("cancelAllNonGTC"));
	});
	cancelAllNonGTC.toggleClass("isClickable", true);

	shutdownAllButton.toggleClass("isClickable", true);
	shutdownAllButton.unbind().bind("click", function () {
		ws.send(command("shutdownAll"));
	});

	$(".masterControls .omsButtons .enableNuclearOptions").unbind().bind("click", function () {

		let isCancelAllClickable = cancelAllButton.hasClass("isClickable");
		if (!isCancelAllClickable) {

			cancelAllButton.unbind().bind("click", function () {
				ws.send(command("cancelAll"));
			});
			cancelAllButton.toggleClass("isClickable", true);
			window.setTimeout(setButtonDisabled(cancelAllButton), 2500);
		}
	});

	$("#enderOnlyInput").change(function () {
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

	const serverBlock = getNibbler(server);

	const wasConnected = !serverBlock.hasClass("connectionLost");
	serverBlock.toggleClass("connectionLost", !connectionEstablished);

	if (wasConnected && !connectionEstablished) {

		if (!disconnectionSound.readyState) {
			disconnectionSound.load();
		}
		disconnectionSound.play();
	}
}

function getNibbler(server) {

	let serverBlock = $('#' + server);
	if (!serverBlock[0]) {
		serverBlock = $("#serverBlockTemplate").clone();
		serverBlock.attr("id", server);
		serverBlock.toggleClass("hidden", false);
		serverBlock.toggleClass("serverBlock", true);

		const headerName = serverBlock.find(".serverName");
		headerName.text(server);

		const rowsBlock = serverBlock.find(".rows");
		rowsBlock.toggleClass("hidden", true);
		serverBlock.find(".serverDetails").unbind().bind("click", function () {
			let hide = !rowsBlock.hasClass("hidden");
			rowsBlock.toggleClass("hidden", hide);
			rowsBlock.find("div.row").each(function (i, row) {
				delete Rows[$(row).find(".key").text()];
				$(row).remove();
			});
			if (!hide) {
				ws.send(command("watchServer", [server]));
			} else {
				ws.send(command("unwatchServer", [server]));
			}
		});

		const cancelNonGTCButton = serverBlock.find(".cancelNonGTC");
		const cancelAllButton = serverBlock.find(".cancelAll");
		const shutdownAllButton = serverBlock.find(".shutdownOMS");

		cancelNonGTCButton.toggleClass("isClickable", true);
		cancelNonGTCButton.unbind().bind("click", function () {
			ws.send(command("cancelExchangeNonGTC", [server]));
		});

		shutdownAllButton.toggleClass("isClickable", true);
		shutdownAllButton.unbind().bind("click", function () {
			ws.send(command("shutdownExchange", [server]));
		});

		serverBlock.find(".enableNuclearOptions").unbind().bind("click", function () {
			let isCancelAllClickable = cancelAllButton.hasClass("isClickable");
			if (!isCancelAllClickable) {
				cancelAllButton.unbind().bind("click", function () {
					ws.send(command("cancelExchange", [server]));
				});
				cancelAllButton.toggleClass("isClickable", true);
				window.setTimeout(setButtonDisabled(cancelAllButton), 2500);
			}
		});

		$("#workingOrders").append(serverBlock);
		setOrderCount(serverBlock);
	}

	return serverBlock;
}

function setWorkingOrder(key, chainID, instrument, side, price, filledQuantity, quantity, orderType, tag, server) {

	let row = Rows[key];
	const serverBlock = getNibbler(server);

	if (!row) {
		row = $("#header").clone().removeAttr("id");
		Rows[key] = row;

		const rows = serverBlock.find(".rows");

		const symbolCell = row.find(".symbol");
		symbolCell.text(instrument);
		symbolCell.unbind().bind("click", function () {
			launchLadder(instrument);
		});

		row.find(".key").text(key);
		row.find(".side").text(side);
		row.find(".chainID").text(chainID);
		row.find(".server").text(server);

		row.toggleClass("bid", side == "BID");
		row.toggleClass("offer", side == "ASK");

		row.find(".cancelOrder").unbind().bind("click", function () {
			ws.send(command("cancelOrder", [server, key]));
		});

		rows.append(row);
	}

	row.find(".button").toggleClass("hidden", false);

	row.find(".price").text(price);
	row.find(".filledQuantity").text(filledQuantity);
	row.find(".quantity").text(quantity);
	row.find(".orderType").text(orderType);
	row.find(".tag").text(tag);
	row.toggleClass("notEnder", tag != "Ender");
}

function deleteWorkingOrder(key) {

	let row = Rows[key];
	if (row) {
		delete Rows[key];
		row.remove();
	}
}

function refreshWorkingOrderCounts(server, count) {
	const serverBlock = getNibbler(server);
	setOrderCount(serverBlock, count);
}

function setOrderCount(server, count) {
	const orderCountDiv = server.find(".orderCount");
	orderCountDiv.text(count);
}

function addLoggedInUser(username) {

	const id = "id_" + username;
	const currentUserDiv = $("#" + id)[0];

	console.log("USER", username, currentUserDiv, !currentUserDiv);

	if (!currentUserDiv) {

		const userBlock = $("#userBlock");
		const newUserDiv = $("<div></div>");
		userBlock.append(newUserDiv);
		newUserDiv.attr("id", id);
		newUserDiv.text(username);
	}
}
