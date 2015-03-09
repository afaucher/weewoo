package com.beanfarmergames.weewoo.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.beanfarmergames.common.callbacks.RenderCallback;
import com.beanfarmergames.common.callbacks.UpdateCallback;
import com.beanfarmergames.weewoo.Field;
import com.beanfarmergames.weewoo.RenderContext;
import com.beanfarmergames.weewoo.RenderContext.RenderLayer;

public class Person extends GameEntity implements UpdateCallback, RenderCallback<RenderContext>, Disposable {

    private static final float RADIUS = 5;
    private static final float LIFE_DRAIN_PER_SECOND = 0.1f;
    private static final float LIFE_DRAIN_PICKED_UP_PER_SECOND = 0.01f;
    private static final float HURT_PROBABILITY_SECOND = 0.01f;

    private Body body = null;
    private final Field field;
    private boolean pickedUp = false;
    // TODO: Move this into the field
    private Vector2 blood = null;
    private boolean hurt = false;
    private float life = 1.0f;

    private static Fixture attachShape(Body body, Vector2 offset, float radius, Object userData) {
        Fixture fixture = null;

        CircleShape sd = new CircleShape();
        sd.setRadius(radius);
        sd.setPosition(offset);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = sd;
        fdef.density = 1.0f;
        fdef.friction = 0.5f;
        fdef.restitution = 0.6f;

        fixture = body.createFixture(fdef);
        fixture.setUserData(userData);

        return fixture;
    }

    public Person(Field field, Vector2 spwan, boolean hurt) {
        this.field = field;
        this.hurt = hurt;

        // Build Body
        World world = field.getWorld();

        BodyDef bd = new BodyDef();
        bd.allowSleep = true;
        bd.position.set(0, 0);
        body = world.createBody(bd);
        body.setBullet(true);
        body.setAngularDamping(0.1f);
        body.setLinearDamping(0.005f);
        body.setUserData(this);
        body.setType(BodyDef.BodyType.DynamicBody);

        attachShape(body, new Vector2(), RADIUS, this);

        body.setTransform(spwan, 0);
        if (hurt) {
            blood = spwan.cpy();
        }

        // Insert into field
        field.getRenderCallbacks().registerCallback(this);
        field.getUpdateCallbacks().registerCallback(this);
        field.getDisposeCallbacks().registerCallback(this);
    }

    @Override
    public Enum<?> getEntityType() {
        return EntityType.Person;
    }

    public boolean pickup() {
        if (!hurt || pickedUp) {
            return false;
        }

        pickedUp = true;
        return true;
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }
    
    public void hurt() {
        if (!hurt && body != null) {
            hurt = true;
            blood = body.getPosition().cpy();
        }
    }

    @Override
    public void render(RenderContext renderContext) {

        ShapeRenderer renderer = renderContext.getRenderer();

        if (RenderLayer.PEOPLE.equals(renderContext.getRenderLayer())) {

            if (body != null) {

                renderer.begin(ShapeType.Filled);
                renderer.setColor(Color.PINK);
                Vector2 pos = body.getPosition().cpy();
                renderer.circle(pos.x, pos.y, RADIUS);
                renderer.end();
                renderer.setColor(Color.BLACK);
                renderer.begin(ShapeType.Line);
                renderer.circle(pos.x, pos.y, RADIUS);
                renderer.end();
            }
        }

        if (RenderLayer.BLOOD.equals(renderContext.getRenderLayer())) {
            if (blood != null) {
                renderer.begin(ShapeType.Filled);
                renderer.setColor(Color.RED);
                renderer.circle(blood.x - RADIUS, blood.y + RADIUS / 2, RADIUS * 2);
                renderer.end();

            }
        }

    }
    
    public Rectangle renderHealth(RenderContext renderContext, Vector2 pos) {
        final float headRad = 10;
        
        ShapeRenderer renderer = renderContext.getRenderer();
        
        float life = getLife();
        renderer.begin(ShapeType.Filled);
        //TODO: Draw a skull on death
        renderer.setColor(Color.BLACK);
        renderer.circle(pos.x, pos.y, headRad);
        float degrees = life * 360;
        renderer.setColor(Color.PURPLE);
        renderer.arc(pos.x, pos.y, headRad, 0, degrees);
        renderer.end();
        
        Rectangle rect = new Rectangle(pos.x, pos.y, headRad * 2, headRad * 2);
        return rect;
    }

    @Override
    public void updateCallback(long miliseconds) {
        if (pickedUp && body != null) {
            World w = body.getWorld();
            w.destroyBody(body);
            body = null;
        }
        
        if (life > 0 && !hurt && !pickedUp) {
            boolean gotHurt = Math.random() < HURT_PROBABILITY_SECOND * miliseconds / 1000.0f;
            if (gotHurt) {
                hurt();
            }
        }

        if (life > 0 && hurt) {
            if (pickedUp) {
                life = Math.max(0, life - miliseconds * LIFE_DRAIN_PICKED_UP_PER_SECOND / 1000.0f );
            } else {
                life = Math.max(0, life - miliseconds * LIFE_DRAIN_PER_SECOND / 1000.0f);
            }
        }
    }
    
    public void rescue() {
        hurt = false;
        pickedUp = false;
    }
    
    public float getLife() {
        return life;
    }

}
