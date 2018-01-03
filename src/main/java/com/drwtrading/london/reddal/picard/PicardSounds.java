package com.drwtrading.london.reddal.picard;

public enum PicardSounds {

    ETF("/sounds/ee_computer_buzz_warble_tone_20250.wav"),
    FUTURES("/sounds/ee_short_musical_bright_swell_20182.wav"),
    SPREADER("/sounds/ee_beep_tone_30226.wav"),
    STOCKS("/stockAlerts/calf-slap.wav");

    public final String fileName;

    PicardSounds(final String fileName) {
        this.fileName = fileName;
    }
}
