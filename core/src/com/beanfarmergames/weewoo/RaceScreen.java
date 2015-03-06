package com.beanfarmergames.weewoo;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.beanfarmergames.weewoo.entities.Car;

public class RaceScreen implements Screen, InputProcessor {

    private final Field field;
    private OrthographicCamera camera = null;
    private ShapeRenderer renderer = new ShapeRenderer();

    private Car car;

    public RaceScreen() {
        field = new Field();
        field.resetLevel();

        int x = Gdx.app.getGraphics().getWidth();
        int y = Gdx.app.getGraphics().getHeight();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, x, y);

        car = new Car(field, new Vector2(30, 30));
        field.getGameEntities().registerEntity(car);

        Gdx.input.setInputProcessor(this);
        
        bindings.add(new KeyBinding(Input.Keys.A, Input.Keys.D, Input.Keys.W, Input.Keys.S));
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        long miliseconds = (long)(delta * 1000);
        field.updateCallback(miliseconds);

        camera.update();
        for (RenderContext.RenderLayer layer : RenderContext.RenderLayer.values()) {
            RenderContext renderContext = new RenderContext(renderer, layer, camera);
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
    
    private List<KeyBinding> bindings = new ArrayList<KeyBinding>();

    class KeyBinding {
        public final int keycodeLeft;
        public final int keycodeRight;
        public final int keycodeFoward;
        public final int keycodeBack;

        public KeyBinding(int keycodeLeft, int keycodeRight, int keycodeFoward, int keycodeBack) {
            super();
            this.keycodeLeft = keycodeLeft;
            this.keycodeRight = keycodeRight;
            this.keycodeFoward = keycodeFoward;
            this.keycodeBack = keycodeBack;
        }

        public boolean matches(int keycode) {
            return (keycode == keycodeLeft) || (keycode == keycodeRight) || (keycode == keycodeFoward)
                    || (keycode == keycodeBack);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        
        for (int i = 0; i < bindings.size(); i++) {
            KeyBinding binding = bindings.get(i);
            if (!binding.matches(keycode)) {
                continue;
            }

            CarControl carControl = car.getCarControl();
            if (keycode == binding.keycodeLeft) {
                carControl.getX().setX(-1);
            } else if (keycode == binding.keycodeRight) {
                carControl.getX().setX(1);
            } else if (keycode == binding.keycodeFoward) {
                carControl.getY().setX(1);
            } else if (keycode == binding.keycodeBack) {
                carControl.getY().setX(-1);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        
        for (int i = 0; i < bindings.size(); i++) {
            KeyBinding binding = bindings.get(i);
            if (!binding.matches(keycode)) {
                continue;
            }

            CarControl carControl = car.getCarControl();
            if (keycode == binding.keycodeLeft) {
                carControl.getX().setX(0);
            } else if (keycode == binding.keycodeRight) {
                carControl.getX().setX(0);
            } else if (keycode == binding.keycodeFoward) {
                carControl.getY().setX(0);
            } else if (keycode == binding.keycodeBack) {
                carControl.getY().setX(0);
            }
            return true;
        }
        
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        // TODO Auto-generated method stub
        return false;
    }

}
