
function addSortedDiv(tableRows, row, comparator) {

	let bottom = 0;
	let top = tableRows.length;
	while (bottom < top - 1) {
		const mid = Math.floor((bottom + top) / 2);

		if (0 < comparator($(row), $(tableRows[mid]))) {
			bottom = mid;
		} else {
			top = mid;
		}
	}
	row.insertAfter($(tableRows[bottom]));
}
