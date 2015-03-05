package com.beanfarmergames.weewoo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class RaceScreen implements Screen {

    private final Field field;
    private OrthographicCamera camera = null;

    public RaceScreen() {
        field = new Field();

        int x = Gdx.app.getGraphics().getWidth();
        int y = Gdx.app.getGraphics().getHeight();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, x, y);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        camera.update();
        for (RenderContext.RenderLayer layer : RenderContext.RenderLayer.values()) {
            RenderContext renderContext = new RenderContext(layer, camera);
            field.render(renderContext);
        }

    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub

    }

    @Override
    public void hide() {
        // TODO Auto-generated method stub

    }

    @Override
    public void dispose() {
        field.dispose();
    }

}
