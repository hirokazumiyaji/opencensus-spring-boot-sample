package com.github.hirokazumiyaji.opencensus;

import org.springframework.context.annotation.Configuration;

import io.opencensus.exporter.trace.jaeger.JaegerExporterConfiguration;
import io.opencensus.exporter.trace.jaeger.JaegerTraceExporter;

@Configuration
public class JeagerExporter {
    public JeagerExporter() {
        JaegerTraceExporter.createAndRegister(
                JaegerExporterConfiguration.builder()
                                           .setServiceName("api2")
                                           .setThriftEndpoint("http://127.0.0.1:14268/api/traces")
                                           .build()
        );
    }
}
