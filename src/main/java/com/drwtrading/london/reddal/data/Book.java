package com.drwtrading.london.reddal.data;

import com.drwtrading.frontoffice.book.BookCursor;
import com.drwtrading.frontoffice.book.BookFactory;
import com.drwtrading.frontoffice.book.BookSide;
import com.drwtrading.london.protocols.photon.marketdata.BestPrice;
import com.drwtrading.london.protocols.photon.marketdata.BookConsistencyMarker;
import com.drwtrading.london.protocols.photon.marketdata.BookSnapshot;
import com.drwtrading.london.protocols.photon.marketdata.Level;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.PriceType;
import com.drwtrading.london.protocols.photon.marketdata.PriceUpdate;
import com.drwtrading.london.protocols.photon.marketdata.ProductReset;
import com.drwtrading.london.protocols.photon.marketdata.Side;
import com.drwtrading.london.protocols.photon.marketdata.TopOfBook;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Mutable Book which applies MarketDataService updates and updates the state.
 * If it becomes useful, apply() can return a Map<Price, Delta> representing the modified levels
 * This is done (but relatively untested) in Stringy, since we didn't end up using the deltas for anything
 * <p/>
 * This class probably belongs in MDS, but I'll let it bake here for a bit first
 */
public class Book {
    public final String symbol;
    private final long tickSize;
    private final BookFactory factory;

    private final BookSide bids;
    private final BookSide offers;

    private List<PriceUpdate> priceUpdates = new ArrayList<PriceUpdate>();

    public Book(String symbol, long tickSize, BookFactory factory) {
        this.symbol = symbol;
        this.tickSize = tickSize;
        this.factory = factory;
        this.bids = factory.createBids(tickSize);
        this.offers = factory.createOffers(tickSize);
    }

    private BookSide getBookSide(Side side) {
        if (side == Side.BID) {
            return bids;
        } else {
            return offers;
        }
    }

    public Book clone(){
        final Book book = new Book(symbol, tickSize, factory);
        final Iterator<Level> levelIterator = sideIterator(Side.BID);
        while(levelIterator.hasNext()){
            final Level next = levelIterator.next();
           book.bids.setPriceAndQuantity(next.getPrice(), next.getQuantity());
        }
        final Iterator<Level> asklevelIterator = sideIterator(Side.OFFER);
        while(asklevelIterator.hasNext()){
            final Level next = asklevelIterator.next();
            book.offers.setPriceAndQuantity(next.getPrice(), next.getQuantity());
        }
        book.priceUpdates .addAll(this.priceUpdates);
        return book;
    }

    public TopOfBook getTopOfBook() {
        BestPrice bestBid = bids.isEmpty() ? new BestPrice(false, 0, 0) : new BestPrice(true, bids.getBestPrice(), bids.getBestQuantity());
        BestPrice bestOffer = offers.isEmpty() ? new BestPrice(false, 999999999, 0) : new BestPrice(true, offers.getBestPrice(), offers.getBestQuantity());
        return new TopOfBook(symbol, PriceType.DIRECT, bestBid, bestOffer);
    }

    public Level getLevel(long price, Side side) {
        return new Level(price, getBookSide(side).getQuantityForPrice(price));
    }

    @Override
    public String toString() {
        return "Book{" +
                "symbol='" + symbol + '\'' +
                ", bids=" + bids +
                ", offers=" + offers +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Book book = (Book) o;

        if (tickSize != book.tickSize) return false;
        if (bids != null ? !bids.equals(book.bids) : book.bids != null) return false;
        if (factory != null ? !factory.equals(book.factory) : book.factory != null) return false;
        if (offers != null ? !offers.equals(book.offers) : book.offers != null) return false;
        if (priceUpdates != null ? !priceUpdates.equals(book.priceUpdates) : book.priceUpdates != null) return false;
        if (symbol != null ? !symbol.equals(book.symbol) : book.symbol != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = symbol != null ? symbol.hashCode() : 0;
        result = 31 * result + (int) (tickSize ^ (tickSize >>> 32));
        result = 31 * result + (factory != null ? factory.hashCode() : 0);
        result = 31 * result + (bids != null ? bids.hashCode() : 0);
        result = 31 * result + (offers != null ? offers.hashCode() : 0);
        result = 31 * result + (priceUpdates != null ? priceUpdates.hashCode() : 0);
        return result;
    }

    /**
     * Updates the state of the book to reflect the event
     *
     * @return whether the event was of a type which may modify the book - note that PriceUpdates return false, since the modification doesn't occur until the BookConsistencyMarker is received
     */
    public boolean apply(MarketDataEvent event) {
        if (event instanceof BookSnapshot) {
            BookSnapshot snapshot = (BookSnapshot) event;
            if (!snapshot.getSymbol().equals(symbol)) {
                throw new IllegalArgumentException("Can't apply " + event + " to " + symbol + " book.");
            }
            getBookSide(snapshot.getSide()).clear();
            for (Level level : snapshot.getLevels()) {
                getBookSide(snapshot.getSide()).setPriceAndQuantity(level.getPrice(), level.getQuantity());
            }
            return true;
        } else if (event instanceof PriceUpdate) {
            PriceUpdate priceUpdate = (PriceUpdate) event;
            if (!priceUpdate.getSymbol().equals(symbol)) {
                throw new IllegalArgumentException("Can't apply " + event + " to " + symbol + " book.");
            }
            priceUpdates.add(priceUpdate);
            return false;
        } else if (event instanceof ProductReset) {
            if (!((ProductReset) event).getSymbol().equals(symbol)) {
                throw new IllegalArgumentException("Can't apply " + event + " to " + symbol + " book.");
            }
            resetBook();
            return true;
        } else if (event instanceof BookConsistencyMarker) {
            for (PriceUpdate priceUpdate : priceUpdates) {
                getBookSide(priceUpdate.getSide()).setPriceAndQuantity(priceUpdate.getPrice(), priceUpdate.getQuantity());
            }
            priceUpdates.clear();
            return true;
        }

        return false;
    }

    private void resetBook() {
        priceUpdates.clear();
        bids.clear();
        offers.clear();
    }

    public Iterator<Level> sideIterator(final Side side) {
        return new Iterator<Level>() {
            Level next;
            BookCursor cursor;

            {
                cursor = factory.createSparseCursor();
                cursor.reset(side == Side.BID ? bids : offers);
                next = nextLevel();
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Level next() {
                Level result = next;
                next = nextLevel();
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private Level nextLevel() {
                return cursor.next() ? new Level(cursor.getPrice(), cursor.getQuantity()) : null;
            }
        };
    }

    public void clear() {
        resetBook();
    }
}
