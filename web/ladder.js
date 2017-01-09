var handler;

// Handler for incoming data

var TAB = 9;

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
	var ladder = $("#ladder");
	var rows = $("#rows");
	rows.find(".row").remove();
	for (var i = 0; i < levels; i++) {
		var row = $("#row_template").clone().removeClass("template");
		row.attr('id', 'row_' + i);
		row.find('.order').attr('id', 'order_' + i).text(' ');
		row.find('.bid').attr('id', 'bid_' + i).text(' ');
		row.find('.price').attr('id', 'price_' + i).text(' ');
		row.find('.offer').attr('id', 'offer_' + i).text(' ');
		row.find('.trade').attr('id', 'trade_' + i).text(' ');
		row.find('.volume').attr('id', 'volume_' + i).text(' ');
		row.find('.text').attr('id', 'text_' + i).text(' ');
		rows.append(row);
	}
}

function trading(clickTrading, workingOrderTags, orderTypesLeft, orderTypesRight) {

	$("#clicktrading").toggleClass("invisible", !clickTrading);

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

	$("input[type=text], input[type=number]").each(function (i, el) {
		handler.updateOn(el, 'keyup');
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
	return parseInt(($(window).height() - 58) / 15);
}

var NumLevels = 0;

function subscribe() {

	var symbolEnd = document.location.hash.indexOf(';', 0);
	if (symbolEnd < 0) {
		symbolEnd = document.location.hash.length;
	}
	var symbol = document.location.hash.substr(1, symbolEnd - 1);
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