package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.icepie.transport.data.FreeTextCell;
import com.drwtrading.london.icepie.transport.data.LaserLineType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

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
        final com.drwtrading.london.reddal.fastui.html.FreeTextCell[] reddalCells = com.drwtrading.london.reddal.fastui.html.FreeTextCell.values();
        Assert.assertEquals(transportCells.length, reddalCells.length);
        for (int i = 0; i < transportCells.length; i++) {
            Assert.assertEquals(transportCells[i].name(), reddalCells[i].name());
        }
    }
}
