let ws;

$(function () {

	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = function (m) {
		eval(m);
	};
});
