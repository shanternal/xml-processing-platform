package dev.shanternal.request.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.Hibernate;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "processed_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class ProcessedRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @ToString.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "xml_hash", nullable = false)
    private ConversionResult conversionResult;

    @NotNull
    @PastOrPresent(message = "Request timestamp cannot be in the future")
    @Column(name = "requested_at", nullable = false)
    @ToString.Include
    private OffsetDateTime requestedAt;

    @NotNull
    @PositiveOrZero(message = "Processing time cannot be negative")
    @Column(name = "processing_time_ms", nullable = false)
    @ToString.Include
    private Long processingTimeMs;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;

        ProcessedRequest that = (ProcessedRequest) o;

        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}