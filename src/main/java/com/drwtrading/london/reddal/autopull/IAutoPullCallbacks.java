package com.drwtrading.london.reddal.autopull;

public interface IAutoPullCallbacks {

    public void runRefreshView(final String message);

    public void ruleFired(final AutoPullerEnabledPullRule rule);

    public static final IAutoPullCallbacks DEFAULT = new IAutoPullCallbacks() {
        @Override
        public void runRefreshView(final String message) {
        }

        @Override
        public void ruleFired(final AutoPullerEnabledPullRule rule) {
        }
    };
}
