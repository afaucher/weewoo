package com.beanfarmergames.weewoo;

import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.beanfarmergames.common.field.AbstractField;
import com.beanfarmergames.weewoo.RenderContext.RenderLayer;
import com.beanfarmergames.weewoo.entities.GameEntity;

public class Field extends AbstractField<RenderContext, GameEntity> {

    private TiledMap map = null;
    private TiledMapRenderer mapRenderer = null;
    private static final String level = "maps/LoopTrack.tmx";

    public Field() {
        super(WeeWooGame.getAssetManager());

        assetManager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
        assetManager.load(level, TiledMap.class);
        assetManager.finishLoading();

        map = assetManager.get(level);
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);
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
    public void render(RenderContext renderContext) {
        super.render(renderContext);

        if (RenderLayer.FIELD.equals(renderContext.getRenderLayer())) {
            mapRenderer.setView(renderContext.getCamera());
            mapRenderer.render();
        }
        if (RenderLayer.UI.equals(renderContext.getRenderLayer())) {
            drawGuage(renderContext, new Vector2(50, 50), 0.5f, 0.7f, 0.3f);
        }
    }

    @Override
    public void dispose() {
        assetManager.dispose();
    }

    @Override
    protected Vector2 getStaticGravity() {
        return new Vector2(0, 0);
    }

    @Override
    protected void buildWorld(World w) {
        // No obstacles yet
    }

}
