package dev.shanternal.request.repository;

import dev.shanternal.request.entity.ConversionResult;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConversionResultRepository {

    private final SessionFactory sessionFactory;

    public ConversionResult saveIfAbsent(ConversionResult entity) {
        Session session = sessionFactory.getCurrentSession();

        //TODO: При переходе на Postgres 19 заменить на ON CONFLICT (...) DO SELECT (...)
        ConversionResult inserted = session.createNativeQuery("""
                    INSERT INTO conversion_results
                        (xml_hash, canonical_xml, xml_tags_count, target_json, json_keys_count)
                    VALUES
                        (:xmlHash, :canonicalXml, :xmlTagsCount, :targetJson, :jsonKeysCount)
                    ON CONFLICT (xml_hash) DO NOTHING
                    RETURNING *
                    """, ConversionResult.class)
                .setParameter("xmlHash", entity.getXmlHash())
                .setParameter("canonicalXml", entity.getCanonicalXml())
                .setParameter("xmlTagsCount", entity.getXmlTagsCount())
                .setParameter("targetJson", entity.getTargetJson())
                .setParameter("jsonKeysCount", entity.getJsonKeysCount())
                .getSingleResultOrNull();

        if (inserted == null) {
            inserted = session.get(ConversionResult.class, entity.getXmlHash());
        }

        return inserted;
    }

    public Optional<ConversionResult> findByXmlHash(String xmlHash) {
        return Optional.ofNullable(
                sessionFactory.getCurrentSession().get(ConversionResult.class, xmlHash)
        );
    }

    public Optional<ConversionResult> claimNextForMigration() {
        Session session = sessionFactory.getCurrentSession();

        return session.createNativeQuery("""
                    SELECT * FROM conversion_results
                    WHERE external_id IS NULL
                    ORDER BY xml_hash
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1
                    """, ConversionResult.class)
                .uniqueResultOptional();
    }
}
