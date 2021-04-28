package rea.dev.rmil.remote;

import java.io.Serializable;
import java.rmi.Remote;

public interface DistributedItem<T> extends Remote, Serializable {

    T get();

}
