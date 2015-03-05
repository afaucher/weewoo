package com.beanfarmergames.weewoo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.beanfarmergames.common.entities.BaseGameEntity;
import com.beanfarmergames.common.field.AbstractField;
import com.beanfarmergames.weewoo.RenderContext.RenderLayer;

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



    @Override
    public void render(RenderContext renderContext) {
        super.render(renderContext);
        
        if (RenderLayer.FIELD.equals(renderContext.getRenderLayer())) {
            mapRenderer.setView(renderContext.getCamera());
            mapRenderer.render();
        }
    }

    @Override
    public void dispose() {
        assetManager.dispose();
    }

    @Override
    protected Vector2 getStaticGravity() {
        return new Vector2(0,0);
    }

    @Override
    protected void buildWorld(World w) {
        //No obstacles yet
    }

}
