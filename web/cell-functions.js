var cellListeners = {};

/**
 * @websocket
 */
function cell(key, value) {
    var listeners = cellListeners[key];
    if (listeners) {
        listeners.forEach(function(listener) {
            listener(value);
        });
    }
}

/**
 * Listen to key value pair changes.
 */
function onCell(key, callback) {
    var listeners = cellListeners[key];
    if (!listeners) {
        listeners = [];
        cellListeners[key] = listeners;

        // Tell the server we care about this cell
        ws.send("cell," + '"' + key + '"');
    }

    listeners.push(callback);
}

function c(args) {
    var name = [];
    for (var piece in arguments) {
        name.push(arguments[piece]);
    }
    return name.join(".");
}

function command(method, args) {
    if (args && args.length > 0) {
        return method + ',' + args.map(
                function(item) {
                    return JSON.stringify(item);
                }).join(',');
    } else {
        return method;
    }
}