package rea.dev.rmil.remote;

import java.util.UUID;

public interface ItemOrchestrator {

    <R> R getItem(UUID nodeID, UUID itemID);

    <R> R getLocalItem(UUID itemID);

}
