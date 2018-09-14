package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.reddal.data.LaserLineValue;

import java.util.List;

class OpxlLadderTextRow {

    final List<OpxlLadderText> ladderText;
    final List<LaserLineValue> laserLines;

    OpxlLadderTextRow(final List<OpxlLadderText> ladderText, final List<LaserLineValue> laserLines) {

        this.ladderText = ladderText;
        this.laserLines = laserLines;
    }
}
