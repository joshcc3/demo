#!/usr/bin/bash
fgrep -h STRATEGY_CREATED ./stackLog*.csv | fgrep INDEX | fgrep -v RFQ | fgrep -v "|XEUR|" | fgrep -v "|IFLL|" | fgrep -v "chix" | fgrep -v "bats" | fgrep -v "|XMON|" | fgrep -v baml-futures | fgrep -v euronext-derivs | fgrep -v derivs | cut -d"|" -f5 | sort -u 
