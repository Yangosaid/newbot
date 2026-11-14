package com.busbattle.app;

/** A live platform connection that feeds the game (TikTok gifts or YouTube chat). */
public interface LiveSource {
    void start();
    void stop();
}
