package com.github.hirokazumiyaji.opencensus;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Span.Kind;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.propagation.SpanContextParseException;
import io.opencensus.trace.propagation.TextFormat;
import io.opencensus.trace.samplers.Samplers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TracingFilter extends OncePerRequestFilter {
    private static final Tracer tracer = Tracing.getTracer();
    private static final TextFormat textFormat = Tracing.getPropagationComponent().getB3Format();
    private static final TextFormat.Getter<HttpServletRequest> getter = new TextFormat.Getter<>() {
        @Override
        public String get(HttpServletRequest httpRequest, String s) {
            return httpRequest.getHeader(s);
        }
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        SpanContext spanContext;
        SpanBuilder spanBuilder;
        final String spanName = String.format(
                "%s %s",
                request.getMethod().toUpperCase(),
                request.getRequestURI()
        );
        try {
            spanContext = textFormat.extract(request, getter);
            spanBuilder = tracer.spanBuilderWithRemoteParent(spanName, spanContext)
                                .setSpanKind(Kind.SERVER);
        } catch (SpanContextParseException e) {
            spanBuilder = tracer.spanBuilder(spanName);
        }
        Span span = spanBuilder.setRecordEvents(true)
                               .setSampler(Samplers.alwaysSample())
                               .startSpan();
        span.putAttribute("http.method", AttributeValue.stringAttributeValue(request.getMethod()));
        try (Scope s = tracer.withSpan(span)) {
            filterChain.doFilter(request, response);
        } finally {
            span.end();
        }
    }
}
