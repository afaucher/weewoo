package com.beanfarmergames.weewoo;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;

public class WeeWooGame extends Game {
    
    private static AssetManager assetManager = new AssetManager();
    
    public static AssetManager getAssetManager() {
        return assetManager;
    }

    @Override
    public void create() {
        setScreen(new MainMenu());
    }

}
