// Constants
const ARG_SEPARATOR = '\0';
const CMD_SEPARATOR = '\1';
const DATA_SEPARATOR = '\2';

const LEFT_CLICK_BUTTON = 1;
const MIDDLE_CLICK_BUTTON = 2;
const RIGHT_CLICK_BUTTON = 3;

// Globals
let ws;

const Handler = function (ws) {

	let Data = {};
	const Regexps = {};
	const self = this;

	function toggleClass(id, cls, enabled) {
		const elem = q(id);
		if (elem) {
			if (enabled) {
				elem.classList.add(cls);
			} else {
				elem.classList.remove(cls);
			}
		}
	}

	function setData(elementId, key, value) {
		if (!Data[elementId]) {
			Data[elementId] = {};
		}
		Data[elementId][key] = value;
	}

	function q(s) {
		return document.getElementById(s)
	}

	function set(k, v) {
		const elem = q(k);
		if (elem.tagName == "INPUT" && elem.type == "checkbox") {
			elem.checked = v == "true";
		} else if (elem.tagName == "INPUT" || elem.tagName == "SELECT") {
			elem.value = v;
		} else {
			elem.innerText = v;
		}
	}

	function send(/* varargs */) {
		ws.send(Array.prototype.slice.call(arguments).join(ARG_SEPARATOR));
	}

	function packData(elementId, extra) {
		const data = [];
		const elem = q(elementId);
		const stored = Data[elementId];
		if (stored) {
			for (var k in stored) {
				if (stored.hasOwnProperty(k)) {
					data.push(k);
					data.push(stored[k]);
				}
			}
		}
		if (extra) {
			for (var k in extra) {
				if (extra.hasOwnProperty(k)) {
					data.push(k);
					data.push(extra[k]);
				}
			}
		}
		data.push("value");
		if (elem.tagName == "INPUT" && elem.type == "checkbox") {
			data.push(elem.checked ? "true" : "false");
		} else if (elem.tagName == "INPUT" || elem.tagName == "SELECT") {
			data.push(elem.value);
		} else {
			data.push(elem.innerText);
		}
		return data.join(DATA_SEPARATOR);
	}

	function getButton(e) {
		const button =
			e.which == LEFT_CLICK_BUTTON ? "left"
				: e.which == MIDDLE_CLICK_BUTTON ? "middle"
				: e.which == RIGHT_CLICK_BUTTON ? "right" : "none";
		return button;
	}

	function getRegexp(pattern, modifiers) {
		const key = (pattern + "_" + modifiers);
		let regExp = Regexps[key];
		if (!regExp) {
			regExp = new RegExp(pattern, modifiers);
			Regexps[key] = regExp;
		}
		return regExp;
	}

	self.getData = function (elID, key) {
		return Data[elID][key];
	};

	self.clear = function () {
		Data = {};
	};

	self.msg = function (data) {
		const cmds = data.split(CMD_SEPARATOR);
		cmds.forEach(function (cmd) {
			const args = cmd.split(ARG_SEPARATOR);
			if (self[args[0]]) {
				self[args[0]](args);
			} else {
				console.log('error: no handler for ', args, ' in ', this);
			}
		})
	};

	self.txt = function (args) {
		for (let i = 1; i < args.length - 1; i += 2) {
			set(args[i], args[i + 1]);
		}
	};

	self.cls = function (args) {
		for (let i = 1; i < args.length - 2; i += 3) {
			toggleClass(args[i], args[i + 1], args[i + 2] == "true");
		}
	};

	self.height = function (args) {
		for (let i = 1; i < args.length - 2; i += 3) {
			const moveId = args[i];
			const refId = args[i + 1];
			const heightFraction = 0.5 - parseFloat(args[i + 2]);
			const refCell = $(q(refId));
			const ladderTop = refCell.offsetParent().offsetParent().offset().top;
			const refTop = refCell.offset().top;
			const newTop = (refTop - ladderTop) + heightFraction * refCell.outerHeight(true);
			$(q(moveId)).css({top: newTop - 1});
		}
	};

	self.width = function (args) {
		for (let i = 1; i < args.length - 1; i += 2) {
			const moveId = args[i];
			const width = args[i + 1];
			$(q(moveId)).css({width: width});
		}
	};

	self.data = function (args) {
		for (let i = 1; i < args.length - 2; i += 3) {
			const elementId = args[i];
			const key = args[i + 1];
			const value = args[i + 2];
			setData(elementId, key, value);
		}
	};

	self.eval = function (args) {
		for (let i = 1; i < args.length; i++) {
			eval(args[i]);
		}
	};

	self.clickable = function (args) {
		for (let i = 1; i < args.length; i++) {
			toggleClass(args[i], 'clickable', true);
			$(args[i]).unbind("dblclick")
				.bind("dblclick", function (e) {
					const event = 'dblclick';
					const elementId = e.target.id;
					const button = getButton(e);
					send(event, elementId, packData(elementId, {"button": button}));
					e.stopPropagation();
				})
				.unbind("mousedown")
				.bind("mousedown", function (e) {
					const event = 'click';
					const elementId = e.target.id;
					const button = getButton(e);
					const isCtrl = e.ctrlKey ? "true" : "false";
					send(event, elementId, packData(elementId, {"button": button, "isCtrlPressed": isCtrl}));
					e.stopPropagation();
				})
				.bind("contextmenu", function (e) {
					e.preventDefault();
					return false;
				});
		}
	};

	self.scrollable = function (args) {
		for (let i = 1; i < args.length; i++) {
			$(args[i]).unbind('mousewheel').bind('mousewheel', function (event) {
				send('scroll', event.wheelDelta > 0 ? "up" : "down", event.target.id, packData(event.target.id));
			})
		}
	};

	self.title = function (args) {
		window.document.title = args[1];
	};

	self.tooltip = function (args) {
		for (let i = 1; i < args.length - 1; i += 2) {
			const elementID = args[i];
			const tooltip = args[i + 1];
			q(elementID).setAttribute("title", tooltip);
		}
	};

	self.updateOn = function (el, s) {
		$(el).unbind(s).bind(s, function () {
			send("update", el.id, packData(el.id));
		});
	};

	self.send = send;

	$(window).unbind("keydown").bind("keydown", function (event) {
		send("keydown", event.keyCode);
	});

};
