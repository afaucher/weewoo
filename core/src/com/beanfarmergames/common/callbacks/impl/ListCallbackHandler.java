package com.beanfarmergames.common.callbacks.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.beanfarmergames.common.callbacks.CallbackHandler;

public class ListCallbackHandler<T> implements CallbackHandler<T> {
    
    private List<T> callbacks = new ArrayList<T>();
    
    public Collection<T> getCallbacks() {
        return callbacks;
    }
    
    public void clear() {
        callbacks.clear();
    }

    @Override
    public void registerCallback(T callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeCallback(T callback) {
        callbacks.remove(callback);
    }

}
