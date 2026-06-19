package dev.shanternal.request.config;

import jakarta.annotation.PostConstruct;
import org.apache.xml.security.Init;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@Configuration
public class XmlProcessorConfig {

    @PostConstruct
    void initApacheXmlSecurity() {
        Init.init();
    }

    @Bean
    public DocumentBuilderFactory documentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // защита от XXE
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(true);

        return factory;
    }
}