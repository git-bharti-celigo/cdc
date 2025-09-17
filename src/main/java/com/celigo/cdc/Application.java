package com.celigo.cdc;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Minimal CDC POC Application
 */
@QuarkusMain
public class Application {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
