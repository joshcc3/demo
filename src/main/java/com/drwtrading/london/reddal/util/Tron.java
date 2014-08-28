package com.drwtrading.london.reddal.util;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.jetlang.autosubscribe.TypedChannels;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.jetlang.builder.FiberGroup;
import com.drwtrading.london.util.Struct;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.simplewebserver.WebApplication;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import drw.london.json.JSONGenerator;
import drw.london.json.Jsonable;
import org.jetlang.channels.Publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Tron {


    public static void main(String[] args) throws IOException, InterruptedException {


        TypedChannel<Throwable> errors = TypedChannels.create(Throwable.class);
        FiberGroup fiberGroup = new FiberGroup("fibers", errors);
        FiberBuilder uiFiber = fiberGroup.create("ui");


        WebApplication webApplication = new WebApplication(8082, errors);
        webApplication.enableSingleSignOn();

        webApplication.alias("/tron", "/tron.html");
        TypedChannel<WebSocketControlMessage> websocket = TypedChannels.create(WebSocketControlMessage.class);
        webApplication.createWebSocket("/tron/ws/", websocket, uiFiber.getFiber());

        TronPresenter tronPresenter = new TronPresenter();
        uiFiber.subscribe(tronPresenter, websocket);
        uiFiber.getFiber().scheduleWithFixedDelay(tronPresenter.tickRunnable(), 1000, 50, TimeUnit.MILLISECONDS);


        fiberGroup.start();
        webApplication.serveStaticContent("web");
        webApplication.start();
        new CountDownLatch(1).await();
    }

    public static class TronGame {

        static class Point extends Struct {
            public final int x;
            public final int y;

            Point(final int x, final int y) {
                this.x = x;
                this.y = y;
            }

            public Point toward(Direction direction) {
                return new Point(x + direction.dx, y + direction.dy);
            }

        }

        static enum Direction {
            S(0, 1), N(0, -1), E(-1, 0), W(1, 0);
            public final int dx;
            public final int dy;

            Direction(final int dx, final int dy) {
                this.dx = dx;
                this.dy = dy;
            }
        }

        public static class BoardPosition implements Jsonable {
            public final int x;
            public final int y;
            public final int player;

            public BoardPosition(final Point position, final int player) {
                this.x = position.x;
                this.y = position.y;
                this.player = player;
            }

            @Override
            public void toJson(final Appendable out) throws IOException {
                JSONGenerator.jsObject(out
                        , "x", x
                        , "y", y
                        , "player", player
                );
            }
        }

        final int size;
        final int players;

        final Direction[] directions;
        final Point[] positions;
        final Map<Point, Integer> board = new HashMap<>();
        final Set<Integer> dead = new HashSet<>();


        boolean done = false;
        int winner = -1;

        public TronGame(final int size, final int players) {
            this.size = size;
            this.players = players;
            directions = new Direction[players];
            positions = new Point[players];
            setStartingPositions();
            updateBoard();
        }

        private void setStartingPositions() {
            Random random = new Random();
            for (int p = 0; p < players; p++) {
                positions[p] = new Point(random.nextInt(size), random.nextInt(size));
            }
        }

        public void setDirection(int p, Direction direction) {
            directions[p] = direction;
        }

        public boolean canStart() {
            return Collections2.filter(Arrays.asList(directions), isNull()).size() == 0;
        }

        public boolean tick() {

            if (canStart() && !done) {
                updatePositions();
                updateBoard();
                updateWinner();
                return true;
            } else {
                return false;
            }
        }

        public void killPlayer(int p) {
            dead.add(p);
        }

        public boolean isPlayerDead(int p) {
            return dead.contains(p);
        }

        public Collection<BoardPosition> getBoard() {
            ArrayList<BoardPosition> ret = new ArrayList<>();
            for (Map.Entry<Point, Integer> entry : board.entrySet()) {
                ret.add(new BoardPosition(entry.getKey(), entry.getValue()));
            }
            return ret;
        }

        public Collection<Integer> getDead() {
            return new HashSet<>(dead);
        }

        public int getSize() {
            return size;
        }

        private void updatePositions() {
            // Update positions
            for (int p = 0; p < players; p++) {
                Point pos = positions[p].toward(directions[p]);
                positions[p] = pos;
                if (board.containsKey(pos) || // Bumped into somebody else's trail
                        pos.x < 0 || pos.x >= size || pos.y < 0 || pos.y >= size) {// Bumped into wall
                    dead.add(p);
                }
            }
        }

        private void updateBoard() {
            for (int p = 0; p < players; p++) {
                if (!dead.contains(p)) {
                    Point pos = positions[p];
                    board.put(pos, p);
                }
            }
        }

        private void updateWinner() {
            if (players - dead.size() <= 0) {
                done = true;
                for (int p = 0; p < players; p++) {
                    if (!isPlayerDead(p)) {
                        winner = p;
                    }
                }
            }
        }

        public boolean isDone() {
            return done;
        }

        public Integer getWinner() {
            return winner > -1 ? winner : null;
        }

        public int getPlayers() {
            return players;
        }

        public static <T> Predicate<T> isNull() {
            return new Predicate<T>() {
                @Override
                public boolean apply(final T input) {
                    return input == null;
                }
            };
        }

    }

    public static class TronPresenter {

        public static final int SIZE = 100;
        WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
        TronGame tronGame = new TronGame(16, 2);

        Map<Publisher<WebSocketOutboundData>, View> playerViews = new HashMap<>();
        Map<Publisher<WebSocketOutboundData>, TronPlayer> players = new HashMap<>();

        public static class TronPlayer implements Jsonable {
            public String name;
            public Integer player;
            public TronPlayer(final String name, final Integer player) {
                this.name = name;
                this.player = player;
            }
            @Override
            public void toJson(final Appendable appendable) throws IOException {
                JSONGenerator.jsObject(appendable, "name", name, "player", player);
            }
        }

        @Subscribe
        public void on(WebSocketConnected webSocketConnected) {
            View view = views.register(webSocketConnected);
            playerViews.put(webSocketConnected.getOutboundChannel(), view);
            players.put(webSocketConnected.getOutboundChannel(), new TronPlayer("", players.size()));
            initGame();
        }

        private void initGame() {
            int i = 0;
            for (TronPlayer tronPlayer : players.values()) {
                tronPlayer.player = i++;
            }
            tronGame = new TronGame(SIZE * players.size(), players.size());
            views.all().init(tronGame.getSize(), players.values());
        }

        @Subscribe
        public void on(WebSocketDisconnected webSocketDisconnected) {
            views.unregister(webSocketDisconnected);
            players.remove(webSocketDisconnected.getOutboundChannel());
            playerViews.remove(webSocketDisconnected.getOutboundChannel());
            initGame();
        }

        @Subscribe
        public void on(WebSocketInboundData webSocketInboundData) {
            views.invoke(webSocketInboundData);
        }

        @FromWebSocketView
        public void direction(String direction, WebSocketInboundData inboundData) {
            Integer player = players.get(inboundData.getOutboundChannel()).player;
            tronGame.setDirection(player, Tron.TronGame.Direction.valueOf(direction));
        }

        @FromWebSocketView
        public void login(String name, WebSocketInboundData inboundData) {
            players.get(inboundData.getOutboundChannel()).name = name;
            initGame();
        }

        public void tick() {
            tronGame.tick();
            views.all().display(tronGame.getSize(), tronGame.getPlayers(), tronGame.getBoard(), tronGame.isDone(), tronGame.getWinner(), tronGame.canStart(), tronGame.getDead());
        }

        public Runnable tickRunnable() {
            return new Runnable() {
                @Override
                public void run() {
                    tick();
                }
            };
        }

        public static interface View {
            public void init(final int size, final Collection<TronPlayer> players);
            public void display(final int size, final int players, final Collection<TronGame.BoardPosition> board, final boolean done, final Integer winner, final boolean b, final Collection<Integer> dead);
        }

    }

}
