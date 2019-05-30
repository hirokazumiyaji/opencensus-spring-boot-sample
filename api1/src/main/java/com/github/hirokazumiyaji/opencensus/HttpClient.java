package com.github.hirokazumiyaji.opencensus;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Span.Kind;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.propagation.TextFormat;
import io.opencensus.trace.samplers.Samplers;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.java8.Java8CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Service
public class HttpClient {
    private final EchoRepository repository;

    public HttpClient() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new TracingInterceptor())
                .build();
        repository = new Retrofit.Builder()
                .baseUrl("http://localhost:8082/")
                .client(okHttpClient)
                .addCallAdapterFactory(Java8CallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(new ObjectMapper()))
                .build()
                .create(EchoRepository.class);
    }

    public Message echo(Message message) {
        try {
            Response<Message> response = repository.echo(message).execute();
            if (response.isSuccessful()) {
                return response.body();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException();
    }

    public static class Tag {
        @Getter
        private Span span;
        @Getter
        private Object tag;

        public Tag(Object tag) {
            this.tag = tag;
        }

        public Tag(Tag tag, Span span) {
            this.span = span;
            this.tag = tag.tag;
        }
    }

    @Slf4j
    public static class TracingInterceptor implements Interceptor {
        private static final Tracer tracer = Tracing.getTracer();
        private static final TextFormat textFormat = Tracing.getPropagationComponent().getB3Format();
        private static final TextFormat.Setter<Request.Builder> setter = new TextFormat.Setter<>() {
            @Override
            public void put(Request.Builder carrier, String key, String value) {
                carrier.addHeader(key, value);
            }
        };

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            okhttp3.Response response;
            String spanName = String.format(
                    "okhttp %s %s",
                    chain.request().method().toUpperCase(),
                    chain.request().url().toString()
            );
            SpanContext spanContext;
            SpanBuilder spanBuilder;
            Span span;
            Request.Builder builder = chain.request().newBuilder();
            Request request;
            Object tag = chain.request().tag();
            Tag t;
            if (chain.connection() == null) {
                t = tag instanceof Tag ? (Tag) tag : new Tag(tag);
                spanContext = tracer.getCurrentSpan().getContext();
            } else if (tag instanceof Tag) {
                t = (Tag) tag;
                spanContext = t.span.getContext();
            } else {
                t = new Tag(tag);
                spanContext = tracer.getCurrentSpan().getContext();
            }
            textFormat.inject(spanContext, builder, setter);
            spanBuilder = tracer.spanBuilderWithRemoteParent(spanName, spanContext)
                                .setSpanKind(Kind.CLIENT);
            span = spanBuilder.setRecordEvents(true)
                              .setSampler(Samplers.alwaysSample())
                              .startSpan();
            request = builder.tag(new Tag(t, span)).build();
            span.putAttribute("http.method", AttributeValue.stringAttributeValue(request.method()));
            try (Scope s = tracer.withSpan(span)) {
                response = chain.proceed(request);
            } catch (Throwable e) {
                span.putAttribute("exception", AttributeValue.stringAttributeValue(e.getMessage()));
                throw e;
            } finally {
                span.end();
            }
            return response;
        }
    }
}
