/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.sun.net.httpserver.HttpServer;

import oshi.SystemInfo;
import oshi.annotation.SuppressForbidden;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

@SuppressForbidden(reason = "Demo test uses com.sun.net.httpserver and System.out for demonstration purposes")
public class PrometheusDemoTest {

    // Hold references for the lifetime of the application, as you would in production
    // (e.g., as a Spring @Bean or an application-scoped field)
    private final PrometheusMeterRegistry registry;
    private final OshiMetrics oshiMetrics;

    PrometheusDemoTest() {
        this.registry = new PrometheusMeterRegistry(new PrometheusConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(5);
            }
        });
        SystemInfo si = new SystemInfo();
        this.oshiMetrics = new OshiMetrics(si.getHardware(), si.getOperatingSystem());
        this.oshiMetrics.bindTo(registry);
    }

    void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/metrics", exchange -> {
            byte[] bytes = registry.scrape().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        System.out.println("OSHI Prometheus metrics at http://localhost:8080/metrics");
        System.out.println("Press Ctrl+C to stop.");
    }

    public static void main(String[] args) throws IOException {
        new PrometheusDemoTest().start();
    }
}
