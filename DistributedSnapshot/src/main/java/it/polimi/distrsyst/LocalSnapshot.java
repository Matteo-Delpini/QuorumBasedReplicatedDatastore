package it.polimi.distrsyst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LocalSnapshot {
    private final State state;
    private final Map<Channel,Collection<Message>> channelSnapshot;

    public LocalSnapshot(State s, Collection<Channel> channels){
        state = s;
        channelSnapshot = new HashMap<>();
        for (Channel c : channels){
            channelSnapshot.put(c,new ArrayList<>());
        }
    }

    public void logMessage(Channel c, Message m){
        Collection<Message> messages = channelSnapshot.get(c);
        messages.add(m);
        channelSnapshot.put(c,messages);
    }

}
