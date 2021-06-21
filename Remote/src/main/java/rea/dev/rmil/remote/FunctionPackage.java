package rea.dev.rmil.remote;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class FunctionPackage implements Serializable {

    private final UUID functionID;
    private final DistributedMethod function;

    public FunctionPackage(UUID functionID, DistributedMethod function) {
        if (!(function instanceof Serializable)) {
            throw new IllegalArgumentException("Expecting a Serializable function");
        }
        this.functionID = functionID;
        this.function = function;
    }

    public UUID getFunctionID() {
        return functionID;
    }

    public DistributedMethod getFunction() {
        return function;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionPackage that = (FunctionPackage) o;
        return functionID.equals(that.functionID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionID);
    }
}
