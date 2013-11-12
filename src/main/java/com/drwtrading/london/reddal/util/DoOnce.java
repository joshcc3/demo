package com.drwtrading.london.reddal.util;

public class DoOnce{
    private boolean didOnce = false;
    public static Runnable NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };
    public void doOnce(Runnable runnable){
        if (!didOnce){
            didOnce = true;
            runnable.run();
        }
    }
    public void reset(){
        didOnce = false;
    }
    public boolean calledAtLeastOnce(){
        return didOnce;
    }
}
