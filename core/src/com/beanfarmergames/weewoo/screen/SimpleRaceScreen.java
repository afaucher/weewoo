package com.beanfarmergames.weewoo.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.beanfarmergames.common.controls.AxisControl;
import com.beanfarmergames.weewoo.AudioPeakRecorder;
import com.beanfarmergames.weewoo.CarControl;
import com.beanfarmergames.weewoo.Field;
import com.beanfarmergames.weewoo.FrequencyTarget;
import com.beanfarmergames.weewoo.PlayerState;
import com.beanfarmergames.weewoo.RenderContext;
import com.beanfarmergames.weewoo.RenderContext.RenderLayer;
import com.beanfarmergames.weewoo.WeeWooServer;
import com.beanfarmergames.weewoo.audio.AudioAnalyzer;
import com.beanfarmergames.weewoo.audio.AudioProfiles;
import com.beanfarmergames.weewoo.audio.FrequencyDomain;
import com.beanfarmergames.weewoo.audio.FrequencyRange;
import com.beanfarmergames.weewoo.debug.DebugSettings;
import com.beanfarmergames.weewoo.entities.Car;
import com.beanfarmergames.weewoo.entities.Person;

public class SimpleRaceScreen implements Screen, InputProcessor {

    private final WeeWooServer server;

    private OrthographicCamera camera = null;
    private ShapeRenderer renderer = new ShapeRenderer();

    public SimpleRaceScreen(WeeWooServer server) {
        this.server = server;

        int x = Gdx.app.getGraphics().getWidth();
        int y = Gdx.app.getGraphics().getHeight();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, x, y);

        Gdx.input.setInputProcessor(this);

        bindings.add(new KeyBinding(Input.Keys.A, Input.Keys.D, Input.Keys.W, Input.Keys.S));
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        long miliseconds = (long) (delta * 1000);
        // Update
        server.updateCallback(miliseconds);

        // Render

        Field field = server.getField();

        camera.update();
        for (RenderContext.RenderLayer layer : RenderContext.RenderLayer.values()) {
            RenderContext renderContext = new RenderContext(renderer, layer, camera);
            field.render(renderContext);
            if (RenderLayer.UI.equals(renderContext.getRenderLayer())) {

                Collection<PlayerState> players = server.getPlayers();
                for (PlayerState playerState : players) {

                    Car car = playerState.getCar();
                    if (car == null) {
                        continue;
                    }

                    float target = playerState.getTarget();
                    float actual = playerState.getActual();
                    float boostRatio = playerState.getBoostRatio();

                    FrequencyRange range = playerState.getTargetRange();

                    float targetRatio = AudioAnalyzer.getFrequencyRatioInRange(range, target);
                    float actualRatio = AudioAnalyzer.getFrequencyRatioInRange(range, actual);
                    RaceUIRenderer.drawGuage(renderContext.getRenderer(), new Vector2(50, 50), targetRatio, actualRatio, boostRatio);

                    Collection<Person> people = car.getPeople();

                    float headX = 50;
                    float headY = 40;

                    for (Person p : people) {
                        Rectangle rect = p.renderHealth(renderContext, new Vector2(headX, headY));
                        headX += rect.width * 1.5f;
                    }
                }
            }
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

        if (Input.Keys.F1 == keycode) {
            DebugSettings.DEBUG_DRAW = !DebugSettings.DEBUG_DRAW;
        } else if (Input.Keys.F2 == keycode) {
            DebugSettings.PERFECT_PITCH = !DebugSettings.PERFECT_PITCH;
        }

        // TODO: Grab the car for player i
        Car firstCar = getFirstCar();
        if (firstCar == null) {
            return false;
        }

        for (int i = 0; i < bindings.size(); i++) {
            KeyBinding binding = bindings.get(i);
            if (!binding.matches(keycode)) {
                continue;
            }

            CarControl carControl = firstCar.getCarControl();
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

    private Car getFirstCar() {
        Car firstCar = null;
        for (PlayerState playerState : server.getPlayers()) {

            firstCar = playerState.getCar();
            if (firstCar != null) {
                break;
            }
        }
        return firstCar;
    }

    @Override
    public boolean keyUp(int keycode) {

        // TODO: Grab the car for player i
        Car firstCar = getFirstCar();
        if (firstCar == null) {
            return false;
        }

        for (int i = 0; i < bindings.size(); i++) {
            KeyBinding binding = bindings.get(i);
            if (!binding.matches(keycode)) {
                continue;
            }

            CarControl carControl = firstCar.getCarControl();
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
