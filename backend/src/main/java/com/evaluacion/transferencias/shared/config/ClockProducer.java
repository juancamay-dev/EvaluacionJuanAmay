package com.evaluacion.transferencias.shared.config;

import java.time.Clock;
import java.time.ZoneId;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Reloj único de la aplicación; las pruebas usan Clock.fixed para fijar el tiempo. */
public class ClockProducer {

    @Produces
    @Singleton
    Clock reloj(@ConfigProperty(name = "app.zona-horaria") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
