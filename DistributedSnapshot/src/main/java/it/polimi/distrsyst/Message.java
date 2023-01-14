package it.polimi.distrsyst;

public interface Message {
    /**
     *
     * @return The information about the message to be saved into the snapshot
     */
    String getDump();

    void buildFromDump(String dump);
}
