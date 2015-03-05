package com.beanfarmergames.common.entities.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.beanfarmergames.common.entities.BaseGameEntity;
import com.beanfarmergames.common.entities.GameEntityList;

public class ArrayGameEntityList<T extends BaseGameEntity>  implements GameEntityList<T> {
    
    private List<T> entities = new ArrayList<T>();
    private List<T> immutableEntities = Collections.unmodifiableList(entities);
    
    @Override
    public void registerEntity(T entity) {
        entities.add(entity);
    }
    
    @Override
    public void unregisterEntity(T entity) {
        entities.remove(entity);
    }
    

    @Override
    public Collection<T> getEntityList() {
        return immutableEntities;
    }

    @Override
    public Collection<T> getFilteredEntityList(Enum filter) {
        List<T> filteredList = new ArrayList<T>();
        
        for (T e : entities) {
            if (filter.equals(e.getEntityType())) {
                filteredList.add(e);
            }
        }
        
        return filteredList;
    }
}
