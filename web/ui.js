function showHoverBoxByElement(element, hoverElement) {
    hoverElement.css({top: element.offset().top - hoverElement.outerHeight() - 2, left: element.offset().left});
}

function showHoverBoxBelowElement(element, hoverElement) {
    hoverElement.css({top: element.offset().top + element.outerHeight() + 2, left: element.offset().left});
}

function showHelpTextOnInputElements() {
    $("*[title]").each(function(index, element) {
        element = $(element);
        var helpText = element.attr("title");

        var showHelpText = function(e) {
            var target = $(e.target);
            var hoverBox = $("#inputHelpHoverBox");
            hoverBox.text(helpText);
            showHoverBoxByElement(target, hoverBox);
            hoverBox.show();
        };

        var hideHelpText = function(e) {
            $("#inputHelpHoverBox").hide();
        };

        element.hover(showHelpText, hideHelpText);
        element.focusin(showHelpText);
        element.focusout(hideHelpText);
    });
}

function showError(message) {
    var error = $("#error");
    error.text(message);
    error.show();
}

$(showHelpTextOnInputElements);