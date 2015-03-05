package com.beanfarmergames.weewoo;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class RenderContext {
    public enum RenderLayer {
        FIELD, CAR, UI
    }
    
    private final RenderLayer renderLayer;
    private final OrthographicCamera camera;

    public RenderContext(RenderLayer renderLayer, OrthographicCamera camera) {
        super();
        this.renderLayer = renderLayer;
        this.camera = camera;
    }

    public RenderLayer getRenderLayer() {
        return renderLayer;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }
    
    
    
}
