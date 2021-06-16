package dev.rea.rmil.engine.backend;

import rea.dev.rmil.remote.*;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*todo: Enforce thread limit across clients, this can be done by client to sever operation, that will reserve a thread
 *  for that client. Thread will be released for other clients after the operation was successfully completed or after a
 *  short timeout - Daniel - 15/06/21*/

final class RmilRemoteEngine implements RemoteEngine {
    private static final long serialVersionUID = 4541222L;
    private static final String FUNC_ERROR_MSG = "Function with this ID does not exist on " +
            "this system, or it is not of the correct type";

    //todo: making the config transient, should study this and see if it can cause unexpected behavior
    private final transient ServerConfiguration configuration;
    private final transient Map<UUID, DistributedMethod> functionMap = new ConcurrentHashMap<>();
    private final transient Map<UUID, Object> objectMap = new ConcurrentHashMap<>();

    protected RmilRemoteEngine(ServerConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration);
    }

    @Override
    public ServerConfiguration getConfiguration() throws RemoteException {
        return configuration;
    }

    @Override
    public void registerFunction(FunctionPackage functionPackage) {
        //Todo: Could be a source of collisions, perhaps functions should be stored separately for each client?
        functionMap.put(functionPackage.getFunctionID(), functionPackage.getFunction());
    }

    @Override
    public boolean removeFunction(UUID functionID) {
        return functionMap.remove(functionID) != null;
    }

    @SuppressWarnings("unchecked") //Should be fine
    @Override
    public <R> R getItem(UUID itemID) throws RemoteException {
        var object = objectMap.get(itemID);
        if (object == null) {
            throw new IllegalArgumentException("Requested object doesnt exist or does not match the parameter type!");
        }
        try {
            return (R) object;
        } catch (ClassCastException | NullPointerException exception) {
            throw new IllegalArgumentException("Requested object doesnt exist or does not match the parameter type!");
        }
    }

    @Override
    public boolean removeItem(UUID itemID) throws RemoteException {
        return objectMap.remove(itemID) != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, T> R checkAndReturnValue(UUID functionID, ArgumentPackage<T> argumentPackage) throws RemoteException {
        putArgumentPackage(argumentPackage);
        return (R) getDistCheck(functionID).check(argumentPackage.getArgument());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R checkAndReturnValue(UUID functionID, UUID itemID) throws RemoteException {
        try {
            return (R) getDistCheck(functionID).check(getItem(itemID));
        } catch (ClassCastException exception) {
            throw new IllegalArgumentException(FUNC_ERROR_MSG);
        }
    }


    @Override
    public <R, T> R applyFunction(UUID functionID, ArgumentPackage<T> argumentPackage) throws RemoteException {
        return applyFunction(functionID, argumentPackage.getItemID(), argumentPackage.getItemID());
    }

    @Override
    public <R> R applyFunction(UUID functionID, UUID itemID) throws RemoteException {
        var origin = getItem(itemID);
        return applyFunction(functionID, itemID, origin);
    }

    @SuppressWarnings("unchecked")
    private <R, T> R applyFunction(UUID functionID, UUID itemID, T origin) {
        var result = (R) getDistFunction(functionID).apply(origin);
        objectMap.put(itemID, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T, R> DistributedMethod.DistFunction<T, R> getDistFunction(UUID functionID) {
        var function = functionMap.get(functionID);
        if (!(function instanceof DistributedMethod.DistFunction)) {
            throw new IllegalArgumentException(FUNC_ERROR_MSG);
        }
        try {
            return (DistributedMethod.DistFunction<T, R>) function;
        } catch (ClassCastException exception) {
            throw new IllegalArgumentException(FUNC_ERROR_MSG);
        }
    }

    @SuppressWarnings("unchecked")
    private <T, R> DistributedMethod.DistCheck<T, R> getDistCheck(UUID functionID) {
        var function = functionMap.get(functionID);
        if (!(function instanceof DistributedMethod.DistCheck)) {
            throw new IllegalArgumentException(FUNC_ERROR_MSG);
        }
        try {
            return (DistributedMethod.DistCheck<T, R>) function;
        } catch (ClassCastException exception) {
            throw new IllegalArgumentException(FUNC_ERROR_MSG);
        }
    }

    protected <T> void putArgumentPackage(ArgumentPackage<T> argumentPackage) {
        objectMap.put(argumentPackage.getItemID(), argumentPackage.getArgument());
    }

}
