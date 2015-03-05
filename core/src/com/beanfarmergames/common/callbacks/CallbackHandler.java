package com.beanfarmergames.common.callbacks;

public interface CallbackHandler<T> {

    public void registerCallback(T callback);
    public void removeCallback(T callback);
}
