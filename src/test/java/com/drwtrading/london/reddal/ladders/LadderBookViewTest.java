package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.icepie.transport.data.FreeTextCell;
import com.drwtrading.london.icepie.transport.data.LaserLineType;
import com.drwtrading.london.reddal.fastui.html.ReddalFreeTextCell;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class LadderBookViewTest {

    @Test
    public void allLaserLinesMappedToColourTest() {
        for (final LaserLineType laserLineType : LaserLineType.values()) {
            Assert.assertTrue(LadderBookView.LASER_LINE_HTML_MAP.containsKey(laserLineType));
        }
    }

    @Test
    public void allFreeTextCellEnumsMappedToTransportTest() {
        final FreeTextCell[] transportCells = FreeTextCell.values();
        final ReddalFreeTextCell[] reddalCells = ReddalFreeTextCell.values();
        Assert.assertEquals(transportCells.length, reddalCells.length);
        for (int i = 0; i < transportCells.length; i++) {
            Assert.assertEquals(transportCells[i].name(), reddalCells[i].name());
        }
    }

    @Test
    public void freeTextCellsMappedCorrectlyTest() {
        for (final Map.Entry<FreeTextCell, ReddalFreeTextCell> entries : ReddalFreeTextCell.FREETEXT_CELL_TO_REDDAL_FREETEXT_CELL_MAP.entrySet()) {
            Assert.assertEquals(entries.getKey(), FreeTextCell.valueOf(entries.getValue().name()));
            Assert.assertEquals(ReddalFreeTextCell.valueOf(entries.getKey().name()), entries.getValue());
        }
    }
}
