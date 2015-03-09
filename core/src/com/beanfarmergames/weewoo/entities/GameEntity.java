package com.beanfarmergames.weewoo.entities;

import com.badlogic.gdx.physics.box2d.Contact;
import com.beanfarmergames.common.entities.BaseGameEntity;

public abstract class GameEntity implements BaseGameEntity {

    public void handleContact(GameEntity hit, Contact physicsContact) {
        //Default is do nothing.
    }
}
