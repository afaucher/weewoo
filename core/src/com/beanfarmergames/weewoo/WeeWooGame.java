package com.beanfarmergames.weewoo;

import com.badlogic.gdx.Game;

public class WeeWooGame extends Game {

    @Override
    public void create() {
        setScreen(new MainMenu());
    }

}
