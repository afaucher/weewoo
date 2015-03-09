package com.beanfarmergames.weewoo;

import java.util.Iterator;
import java.util.Random;

import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.beanfarmergames.common.field.AbstractField;
import com.beanfarmergames.weewoo.RenderContext.RenderLayer;
import com.beanfarmergames.weewoo.debug.DebugSettings;
import com.beanfarmergames.weewoo.entities.GameEntity;
import com.beanfarmergames.weewoo.entities.Hospital;
import com.beanfarmergames.weewoo.entities.Person;

public class Field extends AbstractField<RenderContext, GameEntity> implements ContactListener {

    private TiledMap map = null;
    private TiledMapRenderer mapRenderer = null;
    private static final String level = "maps/LoopTrack.tmx";
    private static final Random rand = new Random();
    Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();

    public Field() {
        super(WeeWooGame.getAssetManager());

        assetManager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
        assetManager.load(level, TiledMap.class);
        assetManager.finishLoading();

        map = assetManager.get(level);
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);
    }

    public MapProperties getMapPropertiesAt(Vector2 at) {
        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("ground");
        int x = (int) (at.x / layer.getTileWidth());
        int y = (int) (at.y / layer.getTileHeight());
        Cell cell = layer.getCell(x, y);
        if (cell == null) {
            // Off the map
            return null;
        }
        TiledMapTile tile = cell.getTile();
        if (tile == null) {
            // Off the map
            return null;
        }
        MapProperties prop = tile.getProperties();
        return prop;
    }

    @Override
    public void render(RenderContext renderContext) {
        super.render(renderContext);

        OrthographicCamera camera = renderContext.getCamera();

        if (RenderLayer.FIELD.equals(renderContext.getRenderLayer())) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }

        if (RenderLayer.DEBUG.equals(renderContext.getRenderLayer())) {
            if (DebugSettings.DEBUG_DRAW) {
                ShapeRenderer renderer = renderContext.getRenderer();
                renderer.setColor(Color.ORANGE);
                Matrix4 proj = renderer.getProjectionMatrix().cpy();
                debugRenderer.render(getWorld(), proj);
            }
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
        w.setContactListener(this);

        for (int i = 0; i < 10; i++) {
            // TODO: Make this fill the whole area and pick better spots
            Vector2 spwan = new Vector2(rand.nextFloat() * 600, rand.nextFloat() * 600);
            Person p = new Person(this, spwan, false);
            this.getGameEntities().registerEntity(p);
        }

        MapLayer layer = map.getLayers().get("physics");
        Iterator<MapObject> objectIt = layer.getObjects().iterator();

        while (objectIt.hasNext()) {
            MapObject mo = objectIt.next();

            String type = (String) mo.getProperties().get("type");
            if ("hospital".equals(type)) {
                if (!(mo instanceof RectangleMapObject)) {
                    throw new RuntimeException("Unknown shape");
                }
                RectangleMapObject rmo = (RectangleMapObject) mo;
                Hospital h = new Hospital(this, rmo.getRectangle());
                this.getGameEntities().registerEntity(h);
            } else {
                throw new RuntimeException(type);
            }
        }
    }

    private GameEntity getEntityFromFixture(Fixture f) {
        if (f == null) {
            return null;
        }
        Object userData = f.getUserData();
        if (userData != null && userData instanceof GameEntity) {
            return (GameEntity) userData;
        }
        userData = f.getBody().getUserData();
        if (userData != null && userData instanceof GameEntity) {
            return (GameEntity) userData;
        }
        return null;
    }

    @Override
    public void beginContact(Contact contact) {
        GameEntity a = getEntityFromFixture(contact.getFixtureA());
        GameEntity b = getEntityFromFixture(contact.getFixtureB());
        if (a != null) {
            a.handleContact(b, contact);
        }
        if (b != null) {
            b.handleContact(a, contact);
        }
    }

    @Override
    public void endContact(Contact contact) {
        // TODO Auto-generated method stub

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        // TODO Auto-generated method stub

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        // TODO Auto-generated method stub

    }

}
