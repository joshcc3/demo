let ws;
let date;

let sound;

$(function () {
	ws = connect();
	ws.logToConsole = false;
	ws.onmessage = eval;
	$("#ack").unbind('click').bind('click', function () {
		ack();
	});
	setInterval(blink, 1000);

	sound = new Audio("sounds/ee_musical_synth_soft_0103743.wav");
});

function ack() {
	ws.send("ack");
}

function add(description, time) {

	const ackDiv = $("#ack");

	if (ackDiv.attr('disabled')) {
		playSound();
	}

	ackDiv.attr('disabled', false);

	const marketNumberDiv = $("#marketnumbers");

	const row = marketNumberDiv.find("tr.template").clone().removeClass("template");
	row.find(".description").text(description);
	row.find(".time").text(time);
	row.addClass("real");

	marketNumberDiv.prepend(row);
}

function clear() {
	$("#ack").attr('disabled', true);
	$(".real").remove();
}

function blink() {

	const ackDiv = $("#ack");

	if (!ackDiv.attr('disabled')) {
		ackDiv.toggleClass("blink");
	}
}

function playSound() {

	if (!sound.readyState) {
		sound.load();
	}
	sound.play();
}
