package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.reddal.data.LaserLine;

import java.util.List;

class OpxlLadderTextRow {

    final List<LadderTextUpdate> ladderText;
    final List<LaserLine> laserLines;

    OpxlLadderTextRow(final List<LadderTextUpdate> ladderText, final List<LaserLine> laserLines) {

        this.ladderText = ladderText;
        this.laserLines = laserLines;
    }
}
