/**
 * Useful jQuery extras.
 */

/**
 * Returns the only instance of a specified selector. Throws an error if there is not exactly 1 element matching
 * selector.
 *
 * Example: $("body").only("#resultsTable").remove();
 */
jQuery.fn.only = function(selector) {
    return this.find(selector).checkOnly();
};

/**
 * Returns the only instance of a specified selector. Throws an error if there is not exactly 1 element matching
 * selector.
 *
 * Example: $.only("#resultsTable").remove();
 */
jQuery.only = function(selector) {
    return $(selector).checkOnly();
};

// Aliases of only
jQuery.fn.findOne = jQuery.fn.only;
jQuery.findOne = jQuery.only;
jQuery.fn.findOnly = jQuery.fn.only;
jQuery.findOnly = jQuery.only;

/**
 * Returns the result of a selector given that it matches 0 or 1 elements. Throws an error if there are more than 1 elements matching.
 *
 * Example: $("body").atMostOne(".possiblyExistingClass").remove();
 */
jQuery.fn.atMostOne = function(selector) {
    return this.find(selector).checkAtMostOne();
};

/**
 * Returns the result of a selector given that it matches 0 or 1 elements. Throws an error if there are more than 1 elements matching.
 *
 * Example: $.atMostOne(".possiblyExistingClass").remove();
 */
jQuery.atMostOne = function(selector) {
    return $(selector).checkAtMostOne();
}

/**
 * Returns the only DOM element in the current set of matched elements. Throws an error if there is not exactly 1 element
 * matching.
 *
 * Example: $("body").onlyElement();
 */
jQuery.fn.onlyElement = function() {
    return this.checkOnly().get()[0];
};

/**
 * Returns whether or not the current set of match elements contains anything.
 *
 * Example: if ($("#resultsTable").exists()) { doSomething(); }
 */
jQuery.fn.exists = function() {
    return this.get().length > 0;
};

/**
 * Throws an error if the current set of matched elements does not match anything.
 *
 * Example: $("body").checkExists().remove();
 */
jQuery.fn.checkExists = function() {
    if (!this.exists()) {
        throw "There is not exactly 1 of: " + this.selector + ". Got: " + items.length;
    }

    return this;
};

/**
 * Throws an error if the current set of matched elements does not match exactly 1 element.
 *
 * Example: $("body").checkOnly().remove();
 */
jQuery.fn.checkOnly = function() {
    var items = this.get();
    if (items.length != 1) {
        throw "There is not exactly 1: " + this.selector + ". Got: " + items.length;
    }

    return this;
};

/**
 * Throws an error if the current set of matched elements contains more than one element.
 *
 * Example: $("body").checkAtMostOne().remove();
 */
jQuery.fn.checkAtMostOne = function() {
    var items = this.get();
    if (items.length > 1) {
        throw "There are more than 1 elements: " + this.selector + ". Got: " + items.length;
    }

    return this;
};

/**
 * Adds each element matched by selector to the current collection(s) in sorted order according to comparator
 * Assumes that each collection is already sorted according to comparator
 * The elements added to the first collection will be the originals (uncloned). Any further insertions will be cloned
 *
 * Example: $('table').addSorted(newRows, f);
 */
jQuery.fn.addSorted = function(selector, comparator) {
    var sortedCollection = $(selector).sort(comparator);
    this.each(function(collIdx, coll) {
        var collection = $(coll);
        if (collection.children().length == 0) {
            collection.append(collIdx == 0 ? sortedCollection : sortedCollection.clone());
        } else {
            var i = 0;
            collection.children().each(function(childIdx, child) {
                for (; i < sortedCollection.length && comparator(child, sortedCollection[i]) > 0; i++) {
                    (collIdx == 0 ? $(sortedCollection[i]) : $(sortedCollection[i]).clone()).insertBefore($(child));
                }
            });
            for (; i < sortedCollection.length; i++) {
                collection.append(collIdx == 0 ? sortedCollection[i] : $(sortedCollection[i]).clone());
            }
        }
    });

    return this;
};

/**
 * Similar to addSorted, but adds the current elements to the collections matched by the selector
 *
 * Example: newRows.addSortedTo('table', f);
 */
jQuery.fn.addSortedTo = function(selector, comparator) {
    return $(selector).addSorted(this, comparator);
};
