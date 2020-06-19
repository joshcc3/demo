var handler;

// Handler for incoming data

var TAB = 9;

let highlightedPrices = new Set();

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
		var keyCode1 = (e.keyCode ? e.keyCode : e.which);
		if (keyCode1 == TAB) {
			e.preventDefault();
		}
	});
});

function draw(levels) {

	var rows = $("#rows");
	rows.find(".row").remove();

	for (var i = 0; i < levels; i++) {
		var bookRow = $("#row_template").clone().removeClass("template");
		let priceCell = bookRow.find('.price');
		let volumeCell = bookRow.find('.volume');

		bookRow.attr('id', 'row_' + i);
		priceCell.attr('id', 'price_' + i).text(' ');
		volumeCell.attr('id', 'volume_' + i).text(' ');

		bookRow.find('.order').attr('id', 'order_' + i).text(' ');
		bookRow.find('.bid').attr('id', 'bid_' + i).text(' ');
		bookRow.find('.offer').attr('id', 'offer_' + i).text(' ');
		bookRow.find('.trade').attr('id', 'trade_' + i).text(' ');
		bookRow.find('.text').attr('id', 'text_' + i).text(' ');

		priceCell.bind("DOMSubtreeModified", e => {
			priceCell.toggleClass("highlighted", highlightedPrices.has(parseFloat(priceCell.text())));
		});

		volumeCell.bind("contextmenu", e => {
			let price = parseFloat(priceCell.text());
			console.log("HI");
			if (highlightedPrices.has(price)) {
				highlightedPrices.delete(price);
				priceCell.toggleClass("highlighted", false);
			} else {
				highlightedPrices.add(price);
				priceCell.toggleClass("highlighted", true);
			}
			return false;
		});

		rows.append(bookRow);
	}

	for (var j = 0; j < levels; j++) {
		var stackRow = $("#stack_row_template").clone().removeClass("template");
		stackRow.attr("id", "stack_row_" + j);
		stackRow.find(".bid.quote").attr("id", "bid_quote_" + j).text(" ");
		stackRow.find(".bid.picard").attr("id", "bid_picard_" + j).text(" ");
		stackRow.find(".bid.offset").attr("id", "bid_offset_" + j).text(" ");

		stackRow.find(".divider").attr("id", "stack_divider_" + j).text(" ");

		stackRow.find(".ask.offset").attr("id", "ask_offset_" + j).text(" ");
		stackRow.find(".ask.picard").attr("id", "ask_picard_" + j).text(" ");
		stackRow.find(".ask.quote").attr("id", "ask_quote_" + j).text(" ");
		rows.append(stackRow);
	}
}

function trading(clickTrading, workingOrderTags, orderTypesLeft, orderTypesRight) {

	$("#clicktrading").toggleClass("invisible", !clickTrading);

	$("input[type=text], input[type=number]").each(function (i, el) {
		handler.updateOn(el, 'keyup');
	});

	if (!clickTrading) {
		return;
	}

	$("#working_order_tags").each(function (i, el) {
		$(el).find("option").remove();
		workingOrderTags.forEach(function (tag) {
			var option = $("<option value=\"" + tag + "\">" + tag + "</option>");
			option.addClass(tag);
			$(el).append(option);
		});
	});

	$("#order_type_left").each(function (i, el) {
		$(el).find("option").remove();
		orderTypesLeft.forEach(function (orderType) {
			var option = $("<option value=\"" + orderType + "\">" + orderType + "</option>");
			option.addClass(orderType);
			$(el).append(option);
		});
	});

	$("#order_type_right").each(function (i, el) {
		$(el).find("option").remove();
		orderTypesRight.forEach(function (orderType) {
			var option = $("<option value=\"" + orderType + "\">" + orderType + "</option>");
			option.addClass(orderType);
			$(el).append(option);
		});
	});

	$("input[type=checkbox]").each(function (i, el) {
		handler.updateOn(el, 'click');
	});

	$("select").each(function (i, el) {
		handler.updateOn(el, 'change');
	});

}

function setData(elementId, key, value) {
	if (!Data[elementId]) {
		Data[elementId] = {};
	}
	Data[elementId][key] = value;
}

function calcLevels() {
	return parseInt(($(window).height() - 72) / 15);
}

var NumLevels = 0;

function subscribe() {

	var symbolEnd = document.location.hash.indexOf(';', 0);
	if (symbolEnd < 0) {
		symbolEnd = document.location.hash.length;
	}
	var symbol = document.location.hash.substr(1, symbolEnd - 1).replace("%20", " ");
	NumLevels = calcLevels();
	draw(NumLevels);
	if (0 < symbolEnd) {
		var ladderSwitch = document.location.hash.substr(symbolEnd + 1);
		handler.send("ladder-subscribe", symbol, NumLevels, ladderSwitch);
	} else {
		handler.send("ladder-subscribe", symbol, NumLevels);
	}
	window.addEventListener("hashchange", function () {
		window.location.reload();
	}, false);
}

function resizeIfNecessary() {
	if (calcLevels() != NumLevels) {
		subscribe();
	}
}

function replace(from, to) {
	document.location.hash = document.location.hash.split(from).join(to);
}

function goToSymbol(symbol) {
	document.location.hash = "#" + symbol;
}

function goToUrl(url) {
	window.location.href = url;
}