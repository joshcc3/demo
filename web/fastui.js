// Constants
var ARG_SEPARATOR = '\0';
var CMD_SEPARATOR = '\1';
var DATA_SEPARATOR = '\2';

var LEFT_CLICK_BUTTON = 1;
var MIDDLE_CLICK_BUTTON = 2;
var RIGHT_CLICK_BUTTON = 3;

// Globals
var ws;


var Handler = function (ws) {

    var Data = {};
    var Regexps = {};
    var self = this;


    function toggleClass(id, cls, enabled) {
        var elem = q(id);
        if (elem) {
            if (!enabled) {
                elem.className = elem.className.replace(getRegexp("(?:^|\\s)" + cls + "(?!\\S)", "g"), "");
            } else {
                var hasCls = elem.className.match(getRegexp("(?:^|\\s)" + cls + "(?!\\S)", "g"));
                if (!hasCls) {
                    elem.className += " " + cls;
                }
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
        var elem = q(k);
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
        var data = [];
        var elem = q(elementId);
        var stored = Data[elementId];
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
		data.push("working_order_tags");
		data.push($("#working_order_tags").val());
        return data.join(DATA_SEPARATOR);
    }


    function getButton(e) {
        var button =
                e.which == LEFT_CLICK_BUTTON ? "left"
            : e.which == MIDDLE_CLICK_BUTTON ? "middle"
            : e.which == RIGHT_CLICK_BUTTON ? "right" : "none";
        return button;
    }

    function getRegexp(pattern, modifiers) {
        var key = (pattern + "_" + modifiers);
        var regExp = Regexps[key];
        if (!regExp) {
            regExp = new RegExp(pattern, modifiers);
            Regexps[key] = regExp;
        }
        return regExp;
    }

    self.clear = function () {
        Data = {};
    };

    self.msg = function (data) {
        var cmds = data.split(CMD_SEPARATOR);
        cmds.forEach(function (cmd) {
            var args = cmd.split(ARG_SEPARATOR);
            if (self[args[0]]) {
                self[args[0]](args);
            } else {
                console.log('error: no handler for ', args, ' in ', this);
            }
        })
    };

    self.txt = function (args) {
        for (var i = 1; i < args.length - 1; i += 2) {
            set(args[i], args[i + 1]);
        }
    };

    self.cls = function (args) {
        for (var i = 1; i < args.length - 2; i += 3) {
            toggleClass(args[i], args[i + 1], args[i + 2] == "true");
        }
    };

    self.height = function (args) {
        for (var i = 1; i < args.length - 2; i += 3) {
            var moveId = args[i];
            var refId = args[i + 1];
            var heightFraction = 0.5 - parseFloat(args[i + 2]);
            var refCell = $(q(refId));
            var ladderTop = refCell.offsetParent().offsetParent().offset().top;
            var refTop = refCell.offset().top;
            var newTop = (refTop - ladderTop) + heightFraction * refCell.outerHeight(true);
            $(q(moveId)).css({top: newTop - 1});
        }
    };

    self.data = function (args) {
        for (var i = 1; i < args.length - 2; i += 3) {
            var elementId = args[i];
            var key = args[i + 1];
            var value = args[i + 2];
            setData(elementId, key, value);
        }
    };

    self.eval = function (args) {
        for (var i = 1; i < args.length; i++) {
            eval(args[i]);
        }
    };

    self.clickable = function (args) {
        for (var i = 1; i < args.length; i++) {
            toggleClass(args[i], 'clickable', true);
            $(args[i]).unbind()
                .bind('dblclick', function (e) {
                    var event = 'dblclick';
                    var elementId = e.target.id;
                    var button = getButton(e);
                    send(event, elementId, packData(elementId, {"button": button}));
                    e.stopPropagation();
                })
                .bind('mousedown', function (e) {
                    var event = 'click';
                    var elementId = e.target.id;
                    var button = getButton(e);
                    send(event, elementId, packData(elementId, {"button": button}));
                    e.stopPropagation();
                })
                .bind("contextmenu", function (e) {
                    e.preventDefault();
                    return false;
                });
        }
    };

    self.scrollable = function (args) {
        for (var i = 1; i < args.length; i++) {
            $(args[i]).unbind('mousewheel').bind('mousewheel', function (event) {
                send('scroll', event.wheelDelta > 0 ? "up" : "down", event.target.id, packData(event.target.id));
            })
        }
    };

    self.title = function (args) {
        window.document.title = args[1];
    };

    self.updateOn = function (el, s) {
        $(el).unbind().bind(s, function () {
            send('update', el.id, packData(el.id));
        });
    };

    self.send = send;

    $(window).unbind('keydown').bind('keydown', function (event) {
        send('keydown', event.keyCode);
    });

}
