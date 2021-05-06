package rea.dev.rmil.remote;

import java.util.Optional;
import java.util.UUID;

public interface ItemProvider<T> {

    Optional<? extends T> get(UUID itemID);

}
