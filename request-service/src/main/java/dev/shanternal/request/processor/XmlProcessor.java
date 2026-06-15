package dev.shanternal.request.processor;

import lombok.RequiredArgsConstructor;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class XmlProcessor {

    private final DocumentBuilderFactory factory;

    public Document parseXml(String xml) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            return builder.parse(new InputSource(new StringReader(xml)));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("Failed to parse XML", e);
        }
    }

    public String canonicalizeXml(Document document) {
        try {
            Canonicalizer canonicalizer = Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            canonicalizer.canonicalizeSubtree(document, output);
            return output.toString(StandardCharsets.UTF_8);

        } catch (CanonicalizationException | InvalidCanonicalizerException e) {
            throw new IllegalStateException("Failed to canonicalize XML document", e);
        }
    }

    public int countXmlTags(Document document) {
        return countElements(document.getDocumentElement());
    }

    private int countElements(Element element) {
        NodeList children = element.getChildNodes();

        int count = 1;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                count += countElements((Element) child);
            }
        }
        return count;
    }
}