package com.beanfarmergames.weewoo;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.beanfarmergames.common.controls.AxisControl;
import com.beanfarmergames.weewoo.RenderContext.RenderLayer;
import com.beanfarmergames.weewoo.audio.AudioAnalyzer;
import com.beanfarmergames.weewoo.audio.AudioProfiles;
import com.beanfarmergames.weewoo.audio.FrequencyDomain;
import com.beanfarmergames.weewoo.audio.FrequencyRange;
import com.beanfarmergames.weewoo.entities.Car;

public class RaceScreen implements Screen, InputProcessor {

    private final Field field;
    private final FrequencyTarget frequencyTarget = new FrequencyTarget();
    private OrthographicCamera camera = null;
    private ShapeRenderer renderer = new ShapeRenderer();
    private AudioPeakRecorder peakRecorder;
    
    private static final float FREQUENCY_TOLERANCE = 50;

    private Car car;
    private float globalClock = 0;

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

        peakRecorder = new AudioPeakRecorder(AudioProfiles.WEE_WOO, AudioProfiles.PEAK_FREQ_COUNT);
        peakRecorder.start();
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        long miliseconds = (long) (delta * 1000);
        // Update
        field.updateCallback(miliseconds);
        frequencyTarget.updateCallback(miliseconds);

        globalClock += delta;
        
        float target = frequencyTarget.getTarget();
        AxisControl boost = car.getCarControl().getBoost();
        float proposedBoost = 0;
        float actual = 0;

        FrequencyDomain weeWooDomain = peakRecorder.getLastFilteredDomain();

        if (weeWooDomain != null) {
            actual = AudioAnalyzer.getClosestFreqToTarget(weeWooDomain, target);
            
            float hitRatio = 1- Math.min(Math.max(Math.abs(target-actual) / FREQUENCY_TOLERANCE,0),1);
            proposedBoost = hitRatio;
            
            /*
             * update(delta) targetClock += delta; targetFrequency =
             * Sin(targetClock) * range + base. for players accuracy =
             * (freqencyTolerance - Clamp(0,freqencyTolerance,abs(targetFrequency -
             * actualFrequency))) / freqencyTolerance player.points += accuracy *
             * pointsMultipler player.speed = accuracy * playerSpeedMultiplier
             * targetClock += accuracy * clockSpeedupMultiplier
             */
        }
        float currentBoost = boost.getX();
        final float blendRatio = 0.1f;
        float blendedBoost = proposedBoost * blendRatio + currentBoost * (1-blendRatio);
        boost.setX(blendedBoost);

        // Render

        camera.update();
        for (RenderContext.RenderLayer layer : RenderContext.RenderLayer.values()) {
            RenderContext renderContext = new RenderContext(renderer, layer, camera);
            field.render(renderContext);
            if (RenderLayer.UI.equals(renderContext.getRenderLayer())) {
                FrequencyRange range = peakRecorder.getRange();
                float targetRatio = AudioAnalyzer.getFrequencyRatioInRange(range, target);
                float boostRatio = blendedBoost;
                float actualRatio = AudioAnalyzer.getFrequencyRatioInRange(range, actual);
                drawGuage(renderContext, new Vector2(50, 50), targetRatio, actualRatio, boostRatio);
            }
        }

    }

    public void drawGuage(RenderContext renderContext, Vector2 pos, float target, float actual, float multiplier) {

        ShapeRenderer renderer = renderContext.getRenderer();

        int guageWidth = 10;
        int guageHeight = 80;

        renderer.begin(ShapeType.Filled);

        Color actualColor = Color.RED.lerp(Color.BLUE, actual);
        Color targetColor = Color.RED.lerp(Color.BLUE, target);

        // The 'offset' from the target
        if (actual < target) {
            /**
             * [actual][error] [target ]
             */
            renderer.setColor(Color.YELLOW);
            renderer.rect(pos.x, pos.y, guageWidth, guageHeight * target);

            renderer.setColor(actualColor);
            renderer.rect(pos.x, pos.y, guageWidth, guageHeight * actual);
        } else {

            /**
             * [actual ][error] [target ]
             */
            renderer.setColor(Color.YELLOW);
            renderer.rect(pos.x, pos.y, guageWidth, guageHeight * actual);

            renderer.setColor(actualColor);
            renderer.rect(pos.x, pos.y, guageWidth, guageHeight * target);
        }

        renderer.setColor(targetColor);
        renderer.rect(pos.x + guageWidth, pos.x, guageWidth, guageHeight * target);

        //Multiplier
        renderer.setColor(Color.ORANGE);
        renderer.rect(pos.x + guageWidth * 3, pos.x, guageWidth, guageHeight * multiplier);

        renderer.setColor(Color.BLACK);

        // Guage Lines
        final int guageLineWidth = 5;
        final int guageHorizLineCount = 4;
        final int guageHorizLineOvershoot = 2;

        for (int i = 0; i < guageHorizLineCount; i++) {
            float ratio = (float) i / (guageHorizLineCount - 1);
            renderer.rectLine(pos.x - guageHorizLineOvershoot, pos.y + guageHeight * ratio, pos.x + guageWidth * 2
                    + guageHorizLineOvershoot, pos.y + guageHeight * ratio, guageLineWidth);
        }
        // Vert
        renderer.rectLine(pos.x + guageWidth, pos.y, pos.x + guageWidth, pos.y + guageHeight, guageLineWidth);

        renderer.end();

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
