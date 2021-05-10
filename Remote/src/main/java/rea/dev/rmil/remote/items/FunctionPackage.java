package rea.dev.rmil.remote.items;

import rea.dev.rmil.remote.BaseFunction;

import java.io.Serializable;
import java.util.UUID;

public final class FunctionPackage implements Serializable {

    private final UUID functionID;
    private final BaseFunction function;

    public FunctionPackage(UUID functionID, BaseFunction function) {
        this.functionID = functionID;
        this.function = function;
    }

    public UUID getFunctionID() {
        return functionID;
    }

    public BaseFunction getFunction() {
        return function;
    }


}
