module rmil.client {
    requires java.rmi;
    requires rmil.remote;
    requires org.slf4j;

    exports dev.rea.rmil.client;
    exports dev.rea.rmil.client.grid;
}