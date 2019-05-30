package com.github.hirokazumiyaji.opencensus;

import java.time.ZonedDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/echo")
public class EchoController {
    @PostMapping("")
    public ResponseEntity<Message> echo(@Validated @RequestBody Message message) {
        return ResponseEntity.ok(
                new Message(ZonedDateTime.now().toString() + ' ' + message.getMessage())
        );
    }
}
