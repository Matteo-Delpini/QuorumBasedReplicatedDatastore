package it.polimi.distrsyst;

public interface State {
    /**
     *
     * @return The information about the state to be saved into the snapshot
     */
    String getDump();
}
