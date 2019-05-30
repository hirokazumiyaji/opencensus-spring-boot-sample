package com.github.hirokazumiyaji.opencensus;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/echo")
public class EchoController {
    private final HttpClient httpClient;

    public EchoController(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @PostMapping("")
    public ResponseEntity<Message> echo(@Validated @RequestBody Message message) {
        return ResponseEntity.ok(httpClient.echo(message));
    }
}
