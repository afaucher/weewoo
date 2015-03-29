package com.beanfarmergames.weewoo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.beanfarmergames.common.field.AbstractField;
import com.beanfarmergames.weewoo.RenderContext.RenderLayer;
import com.beanfarmergames.weewoo.debug.DebugSettings;
import com.beanfarmergames.weewoo.entities.GameEntity;
import com.beanfarmergames.weewoo.entities.Hospital;
import com.beanfarmergames.weewoo.entities.Person;

public class Field extends AbstractField<RenderContext, GameEntity> implements ContactListener {

    private TiledMap map = null;
    private final WeeWooServer server;
    private TiledMapRenderer mapRenderer = null;
    private static final String level = "maps/LoopTrack.tmx";
    private static final Random rand = new Random();
    private Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
    private TmxMapLoaderMagic magicLoader = new TmxMapLoaderMagic(new InternalFileHandleResolver());
    
    public static final Color GROUND_COLOR = new Color(139/255.0f,74/255.0f,181/255.0f,1);

    private class TmxMapLoaderMagic extends TmxMapLoader {
        Map<Integer, List<Rectangle>> tileObjects = new HashMap<Integer, List<Rectangle>>();

        public TmxMapLoaderMagic(FileHandleResolver resolver) {
            super(resolver);
        }

        public Map<Integer, List<Rectangle>> getTileObjects() {
            return tileObjects;
        }

        @Override
        protected void loadTileSet(TiledMap map, Element element, FileHandle tmxFile, ImageResolver imageResolver) {

            /**
             * This accumulates into a member variable which totally breaks the loader model.
             */
            Array<Element> tiles = element.getChildrenByName("tile");
            int gTileId = element.getIntAttribute("firstgid");
            for (Element tile : tiles) {
                int tileId = tile.getIntAttribute("id");
                Array<Element> objectgroups = tile.getChildrenByName("objectgroup");
                for (Element objectgroup : objectgroups) {
                    Array<Element> objects = objectgroup.getChildrenByName("object");
                    for (Element object : objects) {
                        int objectX = object.getIntAttribute("x");
                        int objectY = object.getIntAttribute("y");
                        int objectWidth = object.getIntAttribute("width");
                        int objectHeight = object.getIntAttribute("height");

                        Rectangle rect = new Rectangle(objectX, objectY, objectWidth, objectHeight);

                        List<Rectangle> rects = tileObjects.get(gTileId + tileId);
                        if (rects == null) {
                            rects = new ArrayList<Rectangle>();
                            tileObjects.put(gTileId + tileId, rects);
                        }
                        rects.add(rect);
                    }
                }
            }

            super.loadTileSet(map, element, tmxFile, imageResolver);
        }
    }

    public Field(WeeWooServer server) {
        super(WeeWooGame.getAssetManager());
        this.server = server;

        assetManager.setLoader(TiledMap.class, magicLoader);
        assetManager.load(level, TiledMap.class);
        assetManager.finishLoading();

        map = assetManager.get(level);
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f);
    }

    public WeeWooServer getServer() {
        return server;
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

    private int[] getLayerIndex(String name) {
        for (int i = 0; i < map.getLayers().getCount(); i++) {
            if (map.getLayers().get(i).getName().equals(name)) {
                return new int[] { i };
            }
        }
        throw new RuntimeException("Not found: " + name);
    }

    @Override
    public void render(RenderContext renderContext) {
        super.render(renderContext);

        OrthographicCamera camera = renderContext.getCamera();

        if (RenderLayer.FIELD.equals(renderContext.getRenderLayer())) {
            mapRenderer.setView(camera);
            mapRenderer.render(getLayerIndex("ground"));
        } else if (RenderLayer.ROOF.equals(renderContext.getRenderLayer())) {
            mapRenderer.setView(camera);
            mapRenderer.render(getLayerIndex("roof"));

        } else if (RenderLayer.DEBUG.equals(renderContext.getRenderLayer())) {
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

        // Find hospitals
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

        Map<Integer, List<Rectangle>> tileObjects = magicLoader.getTileObjects();

        TiledMapTileLayer roofLayer = (TiledMapTileLayer) map.getLayers().get("roof");
        for (int x = 0; x < roofLayer.getWidth(); x++) {
            for (int y = 0; y < roofLayer.getHeight(); y++) {
                Cell cell = roofLayer.getCell(x, y);
                if (cell == null) {
                    continue;
                }

                TiledMapTile tile = cell.getTile();
                if (tile == null) {
                    continue;
                }

                List<Rectangle> rects = tileObjects.get(tile.getId());
                if (rects == null) {
                    continue;
                }

                for (Rectangle rect : rects) {
                    BodyDef bd = new BodyDef();
                    bd.allowSleep = true;
                    float rectX = x * roofLayer.getTileWidth() + rect.x + rect.width / 2;
                    float rectY = y * roofLayer.getTileHeight() + rect.y + rect.height / 2;
                    bd.position.set(rectX, rectY);
                    Body body = w.createBody(bd);

                    body.setType(BodyType.StaticBody);
                    PolygonShape sd = new PolygonShape();
                    sd.setAsBox(rect.width / 2, rect.height / 2);

                    FixtureDef fdef = new FixtureDef();
                    fdef.shape = sd;
                    fdef.density = 0.2f;
                    fdef.friction = 0.5f;
                    fdef.restitution = 0.6f;

                    Fixture fixture = body.createFixture(fdef);
                }
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
