package rea.dev.rmil.remote.items;

import rea.dev.rmil.remote.BaseTask;

import java.io.Serializable;
import java.util.UUID;

public final class FunctionPackage implements Serializable {

    private final UUID functionID;
    private final BaseTask function;

    public FunctionPackage(UUID functionID, BaseTask function) {
        this.functionID = functionID;
        this.function = function;
    }

    public UUID getFunctionID() {
        return functionID;
    }

    public BaseTask getFunction() {
        return function;
    }


}
