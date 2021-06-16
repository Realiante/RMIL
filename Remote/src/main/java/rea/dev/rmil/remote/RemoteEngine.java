package rea.dev.rmil.remote;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface RemoteEngine extends Remote, Serializable {

    ServerConfiguration getConfiguration() throws RemoteException;

    /**
     * Registers a function on a remote server for further execution
     *
     * @param functionPackage package containing function and its id
     */
    void registerFunction(FunctionPackage functionPackage) throws RemoteException;

    /**
     * Removes a function from the remote server
     *
     * @param functionID id of the function to be removed
     * @return true - if function was successfully removed
     */
    boolean removeFunction(UUID functionID) throws RemoteException;

    /**
     * Downloads an item from a remote server and deletes the reference to it
     *
     * @param itemID id of the item to download
     * @param <R>    expected return type
     * @return item
     */
    <R> R getItem(UUID itemID) throws RemoteException;


    /**
     * Removes an item from a remote server.
     *
     * @param itemID id of the item to remove
     * @return true - if removed successfully
     */
    boolean removeItem(UUID itemID) throws RemoteException;


    /**
     * Applies a filter on a remote server.
     * This version of filter uploads a new argument package to the server and returns the filter test result.
     *
     * @param functionID      id of the filter to apply
     * @param argumentPackage package containing a function and its id
     * @param <T>             argument type
     * @param <R>             expected return type
     * @return filter test result
     */
    <R, T> R checkAndReturnValue(UUID functionID, ArgumentPackage<T> argumentPackage) throws RemoteException;

    /**
     * Applies a filter on a remote server.
     * This version of filter method uses a version of the item already uploaded to
     * the server and returns the filter test result.
     *
     * @param functionID id of the filter to apply
     * @param itemID     id of the item to test
     * @param <R>        expected return type
     * @return filter test result
     */
    <R> R checkAndReturnValue(UUID functionID, UUID itemID) throws RemoteException;


    <R, T> R applyFunction(UUID functionID, ArgumentPackage<T> argumentPackage) throws RemoteException;

    <R> R applyFunction(UUID functionID, UUID itemID) throws RemoteException;

}
