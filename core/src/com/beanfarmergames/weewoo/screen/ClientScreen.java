package com.beanfarmergames.weewoo.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.beanfarmergames.weewoo.Field;
import com.beanfarmergames.weewoo.WeeWooClient;
import com.beanfarmergames.weewoo.WeeWooClient.ClientState;
import com.beanfarmergames.weewoo.audio.AudioAnalyzer;
import com.beanfarmergames.weewoo.audio.FrequencyRange;

public class ClientScreen implements Screen {
    
    private final WeeWooClient client;
    private SpriteBatch batch = null;
    private Texture connecting = new Texture("art/import.png");
    private Texture warning = new Texture("art/warning.png");
    private Texture[] gamepads;
    ShapeRenderer renderer = new ShapeRenderer(500); 
    
    public ClientScreen(WeeWooClient client) {
        super();
        this.client = client;
        
        batch = new SpriteBatch();
        
        gamepads = new Texture[5];
        gamepads[0] = new Texture("art/gamepad.png");
        for (int i = 1; i <= 4; i++) {
            gamepads[i] = new Texture("art/gamepad" + i + ".png");
        }
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        
        //Update
        long miliseconds = (long) (delta * 1000);
        client.updateCallback(miliseconds);
        
        //Render
        Color background = Field.GROUND_COLOR;
        Gdx.gl.glClearColor(background.r, background.b, background.g, background.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        ClientState state = client.getClientState();
        
        int x = Gdx.app.getGraphics().getWidth();
        int y = Gdx.app.getGraphics().getHeight();
        
        batch.begin();
        if (ClientState.Connecting.equals(state) || ClientState.Connected.equals(state)) {
            batch.draw(connecting, x / 2 - connecting.getWidth() / 2, y / 2 - connecting.getHeight() / 2);
        } else if (ClientState.Ready.equals(state)) {
            int playerNumber = client.getPlayerNumber();
            int gamepadIndex = (playerNumber >= gamepads.length ? 0 : playerNumber);
            Texture gamepad = gamepads[gamepadIndex];
            batch.draw(gamepad, x / 2 - gamepad.getWidth() / 2, y / 2 - gamepad.getHeight() / 2);
        
            float target = client.getTarget();
            float actual = client.getActual();
            float boost = 0;//client.getBoost();
            FrequencyRange range = client.getTargetRange();
            if (range != null) {
                float targetRatio = AudioAnalyzer.getFrequencyRatioInRange(range, target);
                float actualRatio = AudioAnalyzer.getFrequencyRatioInRange(range, actual);
                
                RaceUIRenderer.drawGuage(renderer, new Vector2(10,10), targetRatio, actualRatio, boost);
            }
        } else if (ClientState.Disconnected.equals(state)) {
            batch.draw(warning, x / 2 - warning.getWidth() / 2, y / 2 - warning.getHeight() / 2);
        }
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        batch = null;
    }

}
