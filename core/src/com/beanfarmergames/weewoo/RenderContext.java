package com.beanfarmergames.weewoo;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class RenderContext {
    public enum RenderLayer {
        FIELD, CAR, UI
    }
    
    private final RenderLayer renderLayer;
    private final OrthographicCamera camera;
    private final ShapeRenderer renderer;

    public RenderContext(ShapeRenderer renderer, RenderLayer renderLayer, OrthographicCamera camera) {
        super();
        this.renderLayer = renderLayer;
        this.camera = camera;
        this.renderer = renderer;
    }

    public RenderLayer getRenderLayer() {
        return renderLayer;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public ShapeRenderer getRenderer() {
        return renderer;
    }
    
    
    
}
