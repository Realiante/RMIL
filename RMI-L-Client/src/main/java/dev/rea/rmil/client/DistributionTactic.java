package dev.rea.rmil.client;

/**
 * Local Only - Not recommended. Tasks run on local machine only. Use Dist to Local functions to collect
 * distributed objects if any are present, otherwise the system will throw an exception.
 * Remote Only - Wont attempt to run tasks locally, instead all tasks will be performed on remote servers.
 * Standard - Recommended. Runs tasks on both local jvm and remote servers.
 */
public enum DistributionTactic {
    LOCAL_ONLY,
    REMOTE_ONLY,
    STANDARD
}
