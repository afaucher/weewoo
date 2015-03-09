package com.beanfarmergames.weewoo.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.beanfarmergames.common.callbacks.RenderCallback;
import com.beanfarmergames.common.callbacks.UpdateCallback;
import com.beanfarmergames.weewoo.Field;
import com.beanfarmergames.weewoo.RenderContext;

public class Hospital extends GameEntity implements UpdateCallback, RenderCallback<RenderContext>{
    
    private final Field field;
    private final Rectangle zone;
    
    private final Map<Car,List<Person>> rescued = new HashMap<Car, List<Person>>();
    
    public Hospital(Field field, Rectangle zone) {
        this.field = field;
        this.zone = zone;
        
        field.getRenderCallbacks().registerCallback(this);
        field.getUpdateCallbacks().registerCallback(this);
    }

    @Override
    public Enum<?> getEntityType() {
        return EntityType.Hospital;
    }

    @Override
    public void updateCallback(long miliseconds) {
        // TODO Auto-generated method stub
        Collection<GameEntity> cars = field.getGameEntities().getFilteredEntityList(EntityType.Car);
        
        for (GameEntity ge : cars) {
            Car car = (Car)ge;
            List<Person> unloadedThisCar = rescued.get(car);
            if (unloadedThisCar == null) {
                unloadedThisCar = new ArrayList<Person>();
                rescued.put(car, unloadedThisCar);
            }
            
            Vector2 pos = car.getPosition();
            if (zone.contains(pos)) {
                Collection<Person> unloaded = car.unloadPeople();
                for (Person p : unloaded) {
                    p.rescue();
                }
                unloadedThisCar.addAll(unloaded);
            }
        }
    }

    @Override
    public void render(RenderContext renderContext) {

        if (!RenderContext.RenderLayer.BLOOD.equals(renderContext.getRenderLayer())) {
            return;
        }
        
        ShapeRenderer renderer = renderContext.getRenderer();
        renderer.begin(ShapeType.Line);
        renderer.setColor(Color.WHITE);
        renderer.rect(zone.x, zone.y, zone.width, zone.height);
        renderer.end();
        
        float headX = zone.x;
        float headY = zone.y;
        for (List<Person> people : rescued.values()) {
            for (Person p : people) {
                Rectangle rect = p.renderHealth(renderContext, new Vector2(headX, headY));
                headX += rect.width * 1.5f;
            }
        }
    }

}
