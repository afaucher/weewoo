package com.beanfarmergames.weewoo.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.beanfarmergames.common.callbacks.RenderCallback;
import com.beanfarmergames.common.callbacks.UpdateCallback;
import com.beanfarmergames.weewoo.CarControl;
import com.beanfarmergames.weewoo.Field;
import com.beanfarmergames.weewoo.RenderContext;
import com.beanfarmergames.weewoo.RenderContext.RenderLayer;

public class Car extends GameEntity implements UpdateCallback, RenderCallback<RenderContext>, Disposable {
    private static final float CAR_TURNING_TORQUE = 30f;
    private static final float CAR_THRUST = 0.20f;
    private static final float CAR_SPEED_THRUST_LIMIT = 0.5f;
    private static final float CAR_BOOST_MULTIPLIER = 2;
    private static final float MAX_PEOPLE_CARRIED = 2;
    private final Body body;
    private SpriteBatch batch = null;
    private final Field field;
    private final CarControl carControl = new CarControl();
    
    private final float SPRITE_SCALE = 3f;
    
    private List<Person> people = new ArrayList<Person>();

    private Texture sprite = new Texture("art/ambulance.png");

    // FIXME: Stolen from boat
    private Fixture attachShape(Body body, Vector2 offset, float radius, Object userData) {
        Fixture fixture = null;

        //CircleShape sd = new CircleShape();
        //sd.setRadius(radius);
        //sd.setPosition(offset);
        
        float width = sprite.getWidth() / SPRITE_SCALE;
        float height = sprite.getHeight() / SPRITE_SCALE;
        
        PolygonShape sd = new PolygonShape();
        sd.setAsBox(width / 2, height / 2);
        
        

        FixtureDef fdef = new FixtureDef();
        fdef.shape = sd;
        fdef.density = 0.2f;
        fdef.friction = 0.5f;
        fdef.restitution = 0.6f;

        fixture = body.createFixture(fdef);
        fixture.setUserData(userData);

        return fixture;
    }

    public CarControl getCarControl() {
        return carControl;
    }
    
    public Vector2 getPosition() {
        return body.getPosition().cpy();
    }

    public Car(Field field, Vector2 spwan) {
        this.field = field;

        batch = new SpriteBatch();

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

        attachShape(body, new Vector2(), 5, this);

        body.setTransform(spwan, 0);

        // Insert into field
        field.getRenderCallbacks().registerCallback(this);
        field.getUpdateCallbacks().registerCallback(this);
        field.getDisposeCallbacks().registerCallback(this);
    }

    @Override
    public Enum<?> getEntityType() {
        return EntityType.Car;
    }

    @Override
    public void render(RenderContext renderContext) {
        if (!RenderContext.RenderLayer.CAR.equals(renderContext.getRenderLayer())) {
            return;
        }

        Vector2 pos = body.getTransform().getPosition();

        // TODO: There has to be an easier way to do this
        Matrix4 m1 = new Matrix4().trn(pos.x, pos.y, 0);
        Matrix4 m2 = new Matrix4().rotateRad(0, 0, 1, body.getAngle());
        
        

        float width = sprite.getWidth() / SPRITE_SCALE;
        float height = sprite.getHeight() / SPRITE_SCALE;

        batch.begin();
        batch.setTransformMatrix(m1.mul(m2));
        batch.draw(sprite, -width / 2, -height / 2, width, height);
        batch.end();
        batch.setTransformMatrix(new Matrix4());
    }
    
    private static float getSpeedMuliplierFromMapProperties(MapProperties prop) {
        if (prop == null) {
            return 0.1f;
        }
        String type = (String)prop.get("type");
        if ("road".equals(type)) {
            return 1.0f;
        } else if ("water".equals(type)) {
            return 0.25f;
        } else if ("grass".equals(type)) {
            return 0.75f;
        } else {
            return 0.1f;
        }
    }

    @Override
    public void updateCallback(long miliseconds) {
        MapProperties prop = field.getMapPropertiesAt(body.getPosition());
        float mapMultiplier = getSpeedMuliplierFromMapProperties(prop);

        float torque = -CAR_TURNING_TORQUE * carControl.getX().getX();
        float boost = carControl.getBoost().getX();
        float baseThrust = CAR_THRUST * (1 + boost * CAR_BOOST_MULTIPLIER) * mapMultiplier;
        float thrustNewtons = baseThrust * carControl.getY().getX();
        if (thrustNewtons < 0) {
            // Flip controls if driving backwards
            torque = -torque;
        }
        float speed = body.getLinearVelocity().len();
        float speedRatio = speed / CAR_SPEED_THRUST_LIMIT;
        float speedMultiplier = Math.min(Math.max(speedRatio, 0), 1);
        torque *= speedMultiplier;
        body.applyTorque(torque, true);

        Transform transform = body.getTransform();
        Vector2 point = transform.getPosition().cpy();
        float rotationRad = transform.getRotation() + MathUtils.PI * 1.0f / 2.0f;
        float xThrustNewtons = (float) Math.cos(rotationRad) * thrustNewtons;
        float yThrustNewtons = (float) Math.sin(rotationRad) * thrustNewtons;
        Vector2 thrust = new Vector2(xThrustNewtons, yThrustNewtons);
        body.applyForce(thrust, point, true);
    }

    @Override
    public void dispose() {
        if (body != null) {
            World world = field.getWorld();
            world.destroyBody(body);
            // body = null;
        }
        field.getRenderCallbacks().removeCallback(this);
        field.getUpdateCallbacks().removeCallback(this);
        field.getDisposeCallbacks().removeCallback(this);

        batch.dispose();
    }
    
    @Override
    public void handleContact(GameEntity hit, Contact physicsContact) {
        super.handleContact(hit, physicsContact);
        
        if (hit != null && EntityType.Person.equals(hit.getEntityType())) {
            Person person = (Person)hit;
            if (people.size() > MAX_PEOPLE_CARRIED) {
                return;
            }
            if (person.pickup()) {
                people.add(person);
            }
        }
    }
    
    public Collection<Person> unloadPeople() {
        Collection<Person> unloaded = new ArrayList<Person>(people);
        people.clear();
        return unloaded;
    }
    
    public Collection<Person> getPeople() {
        Collection<Person> copy = new ArrayList<Person>(people);
        return copy;
    }

}
