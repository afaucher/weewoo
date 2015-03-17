package com.beanfarmergames.weewoo;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.beanfarmergames.common.callbacks.UpdateCallback;
import com.beanfarmergames.common.controls.AxisControl;
import com.beanfarmergames.weewoo.audio.AudioProfiles;
import com.beanfarmergames.weewoo.debug.DebugSettings;
import com.beanfarmergames.weewoo.entities.Car;
import com.beanfarmergames.weewoo.entities.SendClock;
import com.beanfarmergames.weewoo.net.ActualUpdate;
import com.beanfarmergames.weewoo.net.ClientSetup;
import com.beanfarmergames.weewoo.net.TargetUpdate;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class WeeWooServer extends Listener implements UpdateCallback {

    private ServerState state = ServerState.Waiting;
    private final Server server = new Server();
    private final Field field;
    private final FrequencyTarget frequencyTarget = new FrequencyTarget();

    private static final float FREQUENCY_TOLERANCE = 50;
    private static final int MIN_PLAYERS = 1;

    private int playerNumber = 0;
    
    private SendClock targetClock = new SendClock(50);
    
    public enum ServerState {
        Waiting, //Waiting for enough players 
        Ready, //Countdown to start
        Playing, //Playing
        Done
    }

    public ServerState getState() {
        return state;
    }

    public Field getField() {
        return field;
    }

    private Map<Connection, PlayerState> playerConnecions = new HashMap<>();
    
    public Collection<PlayerState> getPlayers() {
        return playerConnecions.values();
    }

    public WeeWooServer() {
        field = new Field();
        field.resetLevel();

        Kryo k = server.getKryo();
        k.register(ClientSetup.class);
        k.register(TargetUpdate.class);
        k.register(ActualUpdate.class);

        server.start();

        try {
            server.bind(54555, 54777);
        } catch (IOException e) {
            // Ignore
            throw new RuntimeException(e);
        }

        server.addListener(this);

        /*
         * int x = Gdx.app.getGraphics().getWidth(); int y =
         * Gdx.app.getGraphics().getHeight();
         * 
         * car = new Car(field, new Vector2(30, 30));
         * field.getGameEntities().registerEntity(car);
         */
    }

    @Override
    public void received(Connection connection, Object object) {
        synchronized (this) {
            if (object instanceof ActualUpdate) {
                ActualUpdate u = (ActualUpdate)object;

                PlayerState playerState = playerConnecions.get(connection);
                playerState.setActual(u.getActual());
            }
        }
    }

    @Override
    public void connected(Connection connection) {
        synchronized (this) {
            PlayerState playerState = new PlayerState();
            
            playerState.setPlayerNumber(playerNumber++);
            playerState.setTargetRange(AudioProfiles.WEE_WOO);

            ClientSetup clientSetup = new ClientSetup();
            clientSetup.setPlayerNumber(playerState.getPlayerNumber());
            

            connection.sendTCP(clientSetup);
            
            playerConnecions.put(connection, playerState);
        }
    }

    @Override
    public void disconnected(Connection connection) {
        synchronized (this) {
            //TODO: Remove car
            playerConnecions.remove(connection);
        }
    }

    @Override
    public void updateCallback(long miliseconds) {
        synchronized (this) {

            // Update
            
            
            if (ServerState.Waiting.equals(state)) {
                updateWaitingState();
            } else if (ServerState.Playing.equals(state)) {
                updatePlayingState(miliseconds);
            }

        }

    }

    private void updateWaitingState() {
        if (playerConnecions.size() >= MIN_PLAYERS) {
            for (PlayerState playerState : playerConnecions.values()) {
                Car car = new Car(field, new Vector2(30, 30));
                field.getGameEntities().registerEntity(car);
                playerState.setCar(car);
            }
            //Skipping ready countdown for now
            state = ServerState.Playing;
        }
    }

    private void updatePlayingState(long miliseconds) {
        if (playerConnecions.size() < MIN_PLAYERS) {
            state = ServerState.Waiting;
            return;
        }
        
        field.updateCallback(miliseconds);
        frequencyTarget.updateCallback(miliseconds);
        
        float target = frequencyTarget.getTarget();
        
        if (targetClock.shouldSend(miliseconds)) {
            TargetUpdate u = new TargetUpdate();
            u.setTarget(target);
            server.sendToAllTCP(u);
        }
        
        for (PlayerState playerState : playerConnecions.values()) {
            playerState.setTarget(target);
   
            Car car = playerState.getCar();
            if (car == null) {
                continue;
            }
            AxisControl boost = car.getCarControl().getBoost();
            float proposedBoost = 0;
   
            float actual = playerState.getActual();
   
            if (DebugSettings.PERFECT_PITCH) {
                actual = target;
            }
   
            float hitRatio = 1 - Math.min(Math.max(Math.abs(target - actual) / FREQUENCY_TOLERANCE, 0), 1);
            proposedBoost = hitRatio;
            
            float currentBoost = boost.getX();
            final float blendRatio = 0.1f;
            float blendedBoost = proposedBoost * blendRatio + currentBoost * (1 - blendRatio);
            
            Gdx.app.log("Server", "T:" + target + " A:" + actual);
            
            playerState.setBoostRatio(blendedBoost);
            
            boost.setX(blendedBoost);
        }
    }

}
