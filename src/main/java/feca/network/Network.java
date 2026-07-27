package feca.network;

import feca.FECA;
import org.slf4j.Logger;

public class Network {
    public static final Logger LOGGER = FECA.logger(Network.class);

    public static void registerServer() {
        NetworkS2C.registerSend();
        NetworkC2S.registerReceive();
    }

    public static void registerClient() {
        NetworkC2S.registerSend();
        NetworkS2C.registerReceive();
    }
}
