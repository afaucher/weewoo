package com.beanfarmergames.common.entities;

import java.util.Collection;

public interface GameEntityList<T extends BaseGameEntity> {
    public Collection<T> getEntityList();
    public Collection<T> getFilteredEntityList(Enum filter);
    
    public void registerEntity(T entity);
    public void unregisterEntity(T entity);
}
