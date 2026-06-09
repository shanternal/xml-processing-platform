package dev.shanternal.xml2json.service;

import dev.shanternal.xml2json.exception.InvalidXmlException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Xml2JsonService {

    public String convert(String xml) {

        if (!StringUtils.hasText(xml)) {
            throw new InvalidXmlException("XML is empty");
        }

        try {
            JSONObject json = XML.toJSONObject(xml);

            if (json.isEmpty()) {
                throw new InvalidXmlException("Invalid XML");
            }

            return json.toString();

        } catch (JSONException e) {
            throw new InvalidXmlException("Invalid XML", e);
        }
    }
}
