package dev.shanternal.xml2json.controller;

import dev.shanternal.xml2json.service.Xml2JsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class Xml2JsonController {

    private final Xml2JsonService xml2JsonService;

    @PostMapping(
            value = "/xml2json",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> convertXml2Json(@RequestBody(required = false) String xml) {
        String json = xml2JsonService.convert(xml);
        return ResponseEntity.ok(json);
    }
}
