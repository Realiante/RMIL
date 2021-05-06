package rea.dev.rmil.remote;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.UUID;

public interface RemoteExecutor extends Serializable, Remote {

    /**
     * Sends an execute request with new function to the executor server and passes a new argument to it.
     * Depending on settings this could either return the result object or a link to it.
     *
     * @param function function to execute
     * @param argument argument
     * @param <R>      Return type
     * @param <T>      Argument type
     * @return Result of the function with the given argument (Or a distributed item link to it)
     */
    <R, T> R executeFunction(DistFunction<T, R> function, T argument);

    /**
     * Sends an execute request to an already loaded function to the executor server and passes a new argument to it.
     * Depending on settings this could either return the result object or a link to it.
     *
     * @param functionID id of the function to execute
     * @param argument   argument
     * @param <R>        Return type
     * @param <T>        Argument type
     * @return Result of the function with the given argument (Or a distributed item link to it)
     */
    <R, T> R executeFunction(UUID functionID, T argument);

    /**
     * ends an execute request with new function to the executor server with argument id.
     * Depending on settings this could either return the result object or a link to it.
     *
     * @param function   function to execute
     * @param argumentID argument id
     * @param <R>        Return type
     * @return Result of the function with the given argument (Or a distributed item link to it)
     */
    <R, T> R executeFunction(DistFunction<T, R> function, UUID argumentID);

    <R, T, A> R executeBiFunction(DistBiFunction<T, A, R> function, T argument, A anotherArgument);

    <R, T, A> R executeBiFunction(DistBiFunction<T, A, R> function, UUID argumentID, A anotherArgument);

    <R, T, A> R executeBiFunction(UUID functionID, T argument, A anotherArgument);
}
