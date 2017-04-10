
function addSortedDiv(tableRows, row, comparator) {

	var bottom = 0;
	var top = tableRows.length;
	while (bottom < top - 1) {
		var mid = Math.floor((bottom + top) / 2);

		if (0 < comparator(row, tableRows[mid])) {
			bottom = mid;
		} else {
			top = mid;
		}
	}
	row.insertAfter($(tableRows[bottom]));
}
