package com.beanfarmergames.common.field;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.beanfarmergames.common.callbacks.RenderCallback;
import com.beanfarmergames.common.callbacks.UpdateCallback;
import com.beanfarmergames.common.callbacks.impl.ListCallbackHandler;
import com.beanfarmergames.common.entities.BaseGameEntity;
import com.beanfarmergames.common.entities.GameEntityList;
import com.beanfarmergames.common.entities.impl.ArrayGameEntityList;

public abstract class AbstractField<T, X extends BaseGameEntity> implements UpdateCallback,
        RenderCallback<T>, Disposable {
    public static final String TAG = "Field";

    private World world = null;

    private ListCallbackHandler<UpdateCallback> updateCallbacks = new ListCallbackHandler<UpdateCallback>();
    private ListCallbackHandler<RenderCallback<T>> renderCallbacks = new ListCallbackHandler<RenderCallback<T>>();
    private ListCallbackHandler<Disposable> disposeCallbacks = new ListCallbackHandler<Disposable>();
    private ArrayGameEntityList<X> gameEntities = new ArrayGameEntityList<X>();

    protected final AssetManager assetManager;

    public AbstractField(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Get the constant gravity to use while constructing the world.
     * 
     * @return
     */
    protected abstract Vector2 getStaticGravity();

    /**
     * Populate the given world.
     *  
     * @param w
     */
    protected abstract void buildWorld(World w);

    public void resetLevel() {
        Vector2 gravity = getStaticGravity();
        boolean doSleep = false;
        if (world != null) {
            world.dispose();
            world = null;
        }
        world = new World(gravity, doSleep);
        updateCallbacks.clear();
        renderCallbacks.clear();

        disposeDisposeable();
        
        buildWorld(world);
    }

    @Override
    public void render(T renderContext) {

        for (RenderCallback<T> callback : renderCallbacks.getCallbacks()) {
            callback.render(renderContext);
        }
    }

    @Override
    public void updateCallback(long miliseconds) {
        // FIXME: HARDCODE 4 ITERS PER FRAME
        long iters = 4;// (long)(miliseconds * WORLD_STEPS_MILISECOND);
        float seconds = (float) miliseconds;
        float worldStepSeconds = seconds / (float) iters;

        for (int i = 0; i < iters; i++) {
            world.step(worldStepSeconds, 10, 10);
        }

        for (UpdateCallback callback : updateCallbacks.getCallbacks()) {
            callback.updateCallback(miliseconds);
        }
    }

    public World getWorld() {
        return world;
    }

    public ListCallbackHandler<UpdateCallback> getUpdateCallbacks() {
        return updateCallbacks;
    }

    public ListCallbackHandler<RenderCallback<T>> getRenderCallbacks() {
        return renderCallbacks;
    }

    public ListCallbackHandler<Disposable> getDisposeCallbacks() {
        return disposeCallbacks;
    }

    @Override
    public void dispose() {
        disposeDisposeable();

        if (world != null) {
            world.dispose();
            world = null;
        }
        
        assetManager.dispose();
    }

    private void disposeDisposeable() {
        for (Disposable d : disposeCallbacks.getCallbacks()) {
            d.dispose();
        }
        disposeCallbacks.clear();
    }

    public GameEntityList<X> getGameEntities() {
        return gameEntities;
    }
}
