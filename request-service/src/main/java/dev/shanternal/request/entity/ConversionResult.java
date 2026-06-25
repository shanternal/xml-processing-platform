package dev.shanternal.request.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.Objects;

@Entity
@Table(name = "conversion_results")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class ConversionResult {

    @Id
    @NotBlank(message = "Hash cannot be empty")
    @Size(min = 64, max = 64, message = "The hash must consist of 64 characters")
    @Column(name = "xml_hash", length = 64, nullable = false, updatable = false)
    @ToString.Include
    private String xmlHash;

    @Column(name = "canonical_xml", columnDefinition = "text")
    private String canonicalXml;

    @NotNull
    @Positive(message = "XML must contain at least 1 tag")
    @Column(name = "xml_tags_count", nullable = false)
    @ToString.Include
    private Integer xmlTagsCount;

    @Column(name = "target_json", columnDefinition = "text")
    private String targetJson;

    @NotNull
    @PositiveOrZero(message = "JSON keys count cannot be negative")
    @Column(name = "json_keys_count", nullable = false)
    @ToString.Include
    private Integer jsonKeysCount;

    @Size(max = 36, message = "External id must not exceed 36 characters")
    @Column(name = "external_id", length = 36, unique = true)
    @ToString.Include
    private String externalId;

    public boolean isMigrated() {
        return externalId != null;
    }

    public void markAsMigrated(String externalId) {
        this.externalId = externalId;
        this.canonicalXml = null;
        this.targetJson = null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;

        ConversionResult that = (ConversionResult) o;

        return getXmlHash() != null && Objects.equals(getXmlHash(), that.getXmlHash());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(xmlHash);
    }
}
