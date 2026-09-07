package io.github.izanMG.boe;

public class BoeException extends RuntimeException {

    public BoeException(String mensaje) {
        super(mensaje);
    }

    public BoeException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}