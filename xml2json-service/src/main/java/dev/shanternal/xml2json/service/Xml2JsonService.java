package dev.shanternal.xml2json.service;

import dev.shanternal.xml2json.exception.InvalidXmlException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.json.XMLParserConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Xml2JsonService {

    private final XMLParserConfiguration xmlParserConfiguration;

    public Xml2JsonService(@Value("${app.xml-converter.max-nesting-depth:50}") int maxDepth) {
        xmlParserConfiguration = new XMLParserConfiguration().withMaxNestingDepth(maxDepth);
    }

    public String convert(String xml) {

        if (!StringUtils.hasText(xml)) {
            throw new InvalidXmlException("XML is empty");
        }

        try {
            JSONObject json = XML.toJSONObject(xml, xmlParserConfiguration);

            if (json.isEmpty()) {
                throw new InvalidXmlException("Invalid XML");
            }

            return json.toString();

        } catch (JSONException e) {
            throw new InvalidXmlException("Invalid XML", e);
        }
    }
}
