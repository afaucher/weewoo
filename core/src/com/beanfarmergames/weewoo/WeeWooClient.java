package com.beanfarmergames.weewoo;

import java.io.IOException;
import java.net.InetAddress;

import com.badlogic.gdx.Gdx;
import com.beanfarmergames.common.callbacks.UpdateCallback;
import com.beanfarmergames.weewoo.audio.AudioAnalyzer;
import com.beanfarmergames.weewoo.audio.AudioProfiles;
import com.beanfarmergames.weewoo.audio.FrequencyDomain;
import com.beanfarmergames.weewoo.audio.FrequencyRange;
import com.beanfarmergames.weewoo.entities.SendClock;
import com.beanfarmergames.weewoo.net.ActualUpdate;
import com.beanfarmergames.weewoo.net.ClientSetup;
import com.beanfarmergames.weewoo.net.TargetUpdate;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class WeeWooClient extends Listener implements UpdateCallback {
    
    private ClientState state = ClientState.Connecting;
    private final Client client;
    private final InetAddress address;
    private AudioPeakRecorder peakRecorder;
    
    private static final long SEND_FREQ_MILISECONDS = 50;
    
    //Homed remotely
    private Integer playerNumber = null;
    private float target = 0;
    
    //Derived Locally
    private FrequencyRange targetRange = null;
    
    //Homed Locally
    private SendClock actualClock = new SendClock(SEND_FREQ_MILISECONDS);
    private float actual = 0;
    
    public WeeWooClient(InetAddress address) {
        this.client = new Client();
        this.address = address;
        
        Kryo k = client.getKryo();
        k.register(ClientSetup.class);
        k.register(TargetUpdate.class);
        k.register(ActualUpdate.class);
        
        client.start();
        client.addListener(this);
        
        peakRecorder = new AudioPeakRecorder(AudioProfiles.WEE_WOO, AudioProfiles.PEAK_FREQ_COUNT);

        peakRecorder.start();
    }

    public enum ClientState {
        Connecting,
        Connected,
        Ready,
        Disconnected
    }
    
    public ClientState getClientState() {
        return state;
    }
    
    public Integer getPlayerNumber() {
        return playerNumber;
    }
    
    public float getTarget() {
        return 0;
    }
    
    public FrequencyRange getTargetRange() {
        return targetRange;
    }
    
    public float getActual() {
        return actual;
    }

    @Override
    public void updateCallback(long miliseconds) {
        
        synchronized (this) {

            if (ClientState.Connecting.equals(state)) {
                try {
                    client.connect(100, address, 54555, 54777);
                    state = ClientState.Connected;
                } catch (IOException e) {
                    //Ignore
                }
            } else if (ClientState.Connected.equals(state)) {
            } else if (ClientState.Ready.equals(state)) {
                FrequencyDomain weeWooDomain = peakRecorder.getLastFilteredDomain();
                if (weeWooDomain != null) {
                    targetRange = weeWooDomain.getRange();
                    actual = AudioAnalyzer.getClosestFreqToTarget(weeWooDomain, getTarget());
                    
                    Gdx.app.log("Client", "T:" + target + " A:" + actual);
                    
                    if (actualClock.shouldSend(miliseconds)) {
                        ActualUpdate u = new ActualUpdate();
                        u.setActual(actual);;
                        client.sendTCP(u);
                    }
                }
            }
        
        }
    }
    
    @Override
    public void connected(Connection connection) {
        synchronized (this) {
            state = ClientState.Connected;
        }
    }
    
    @Override
    public void received(Connection connection, Object object) {
        synchronized (this) {
            if (object instanceof ClientSetup) {
                ClientSetup clientSetup = (ClientSetup)object;
                this.playerNumber = clientSetup.getPlayerNumber();
                this.state = ClientState.Ready;
            } else if (object instanceof TargetUpdate) {
                TargetUpdate targetUpdate = (TargetUpdate)object;
                this.target = targetUpdate.getTarget();
            }
        }
    }
    
    
}
