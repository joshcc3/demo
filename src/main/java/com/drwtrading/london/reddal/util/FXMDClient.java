package com.drwtrading.london.reddal.util;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.marketData.transport.udpShaped.fiveLevels.ILevelTwoBookListener;

public class FXMDClient implements ILevelTwoBookListener {

    private final FXCalc<?> fxCalc;

    private final LongMap<IBook<?>> updatedBooks;

    public FXMDClient(final FXCalc<?> fxCalc) {
        this.fxCalc = fxCalc;
        this.updatedBooks = new LongMap<>();
    }

    @Override
    public boolean instrumentDefUpdated(final short transportID, final IBook<?> book) {
        updatedBooks.put(transportID, book);
        return true;
    }

    @Override
    public boolean bookUpdated(final short transportID, final BookSide side, final IBook<?> book) {
        updatedBooks.put(transportID, book);
        return true;
    }

    @Override
    public void batchComplete() {
        for (final LongMapNode<IBook<?>> bookNode : updatedBooks) {
            fxCalc.bookUpdated(bookNode.getValue());
        }
        updatedBooks.clear();
    }
}
