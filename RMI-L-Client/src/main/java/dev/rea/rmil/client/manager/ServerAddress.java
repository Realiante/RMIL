package dev.rea.rmil.client.manager;

class ServerAddress {
    private final String address;
    private final int port;

    public ServerAddress(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
