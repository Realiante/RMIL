package rea.dev.rmil.remote;

import java.io.Serializable;
import java.rmi.Remote;

public interface DistributedFunction<R> extends Remote, Serializable {

    R apply();

}
