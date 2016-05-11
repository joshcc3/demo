package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.marketData.book.AggressorSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelTwoMonitor;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;

public class BookLevelTwoViewerAdaptor implements IBookLevelTwoMonitor<IBookLevel> {

    public static final BookLevelTwoViewerAdaptor INSTANCE = new BookLevelTwoViewerAdaptor();

    private BookLevelTwoViewerAdaptor() {
        // SINGLETON INSTANCE
    }

    @Override
    public void addLevel(final IBook book, final IBookLevel level) {

    }

    @Override
    public void modifyLevel(final IBook book, final IBookLevel level, final long oldQty) {

    }

    @Override
    public void modifyLevel(final IBook book, final IBookLevel level, final boolean wasTopOfBook, final long oldPrice, final long oldQty) {

    }

    @Override
    public void deleteLevel(final IBook book, final IBookLevel level) {

    }

    @Override
    public void bookCreated(final IBook book) {

    }

    @Override
    public void clearBook(final IBook book) {

    }

    @Override
    public void trade(final IBook book, final long execID, final AggressorSide side, final long price, final long qty) {

    }

    @Override
    public void statusUpdate(final IBook book) {

    }

    @Override
    public void referencePrice(final IBook book, final IBookReferencePrice referencePriceData) {

    }

    @Override
    public void bookValidated(final IBook book) {

    }

    @Override
    public void impliedQty(final IBook<IBookLevel> book, final IBookLevel level) {

    }

    @Override
    public void logErrorMsg(final String msg) {

    }

    @Override
    public void logErrorMsg(final String msg, final Throwable t) {

    }

    @Override
    public void batchComplete() {

    }
}
