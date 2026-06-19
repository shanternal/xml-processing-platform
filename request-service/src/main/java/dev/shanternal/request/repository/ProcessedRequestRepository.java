package dev.shanternal.request.repository;

import dev.shanternal.request.entity.ConversionResult;
import dev.shanternal.request.entity.ConversionResult_;
import dev.shanternal.request.entity.ProcessedRequest;
import dev.shanternal.request.entity.ProcessedRequest_;
import dev.shanternal.request.dto.response.Page;
import dev.shanternal.request.dto.request.ProcessedRequestFilter;
import dev.shanternal.request.dto.response.ProcessedRequestSummary;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProcessedRequestRepository {

    private final SessionFactory sessionFactory;

    public ProcessedRequest save(ProcessedRequest entity) {
        sessionFactory.getCurrentSession().persist(entity);
        return entity;
    }

    public Optional<ProcessedRequest> findByIdWithConversion(Long id) {
        return sessionFactory.getCurrentSession()
                .createQuery("""
                        SELECT pr FROM ProcessedRequest pr
                        JOIN FETCH pr.conversionResult
                        WHERE pr.id = :id
                        """, ProcessedRequest.class)
                .setParameter("id", id)
                .uniqueResultOptional();
    }

    public Page<ProcessedRequestSummary> findAll(ProcessedRequestFilter filter, int page, int size) {
        long totalCount = countByFilter(filter);

        if (totalCount == 0) {
            return new Page<>(List.of(), page, size, 0);
        }

        List<ProcessedRequestSummary> content = findSummaries(filter, page, size);
        return new Page<>(content, page, size, totalCount);
    }

    private long countByFilter(ProcessedRequestFilter filter) {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);

        Root<ProcessedRequest> root = cq.from(ProcessedRequest.class);
        Join<ProcessedRequest, ConversionResult> conversion = root.join(ProcessedRequest_.conversionResult);

        cq.select(cb.count(root)).where(buildPredicate(root, conversion, filter, cb));
        return session.createQuery(cq).getSingleResult();
    }

    private List<ProcessedRequestSummary> findSummaries(ProcessedRequestFilter filter, int page, int size) {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ProcessedRequestSummary> cq = cb.createQuery(ProcessedRequestSummary.class);

        Root<ProcessedRequest> root = cq.from(ProcessedRequest.class);
        Join<ProcessedRequest, ConversionResult> conversion = root.join(ProcessedRequest_.conversionResult);

        cq.select(cb.construct(
                        ProcessedRequestSummary.class,
                        root.get(ProcessedRequest_.id),
                        root.get(ProcessedRequest_.requestedAt),
                        root.get(ProcessedRequest_.processingTimeMs),
                        conversion.get(ConversionResult_.xmlTagsCount),
                        conversion.get(ConversionResult_.jsonKeysCount)
                ))
                .where(buildPredicate(root, conversion, filter, cb))
                .orderBy(cb.desc(root.get(ProcessedRequest_.requestedAt)));

        int offset = page * size;
        return session.createQuery(cq).setFirstResult(offset).setMaxResults(size).getResultList();
    }

    private Predicate buildPredicate(Root<ProcessedRequest> root,
                                     Join<ProcessedRequest, ConversionResult> conversion,
                                     ProcessedRequestFilter filter,
                                     CriteriaBuilder cb) {

        List<Predicate> predicates = new ArrayList<>();

        addRange(predicates, cb, root.get(ProcessedRequest_.requestedAt), filter.requestedAtFrom(), filter.requestedAtTo());
        addRange(predicates, cb, root.get(ProcessedRequest_.processingTimeMs), filter.processingTimeMsMin(), filter.processingTimeMsMax());

        addRange(predicates, cb, conversion.get(ConversionResult_.xmlTagsCount), filter.xmlTagsCountMin(), filter.xmlTagsCountMax());
        addRange(predicates, cb, conversion.get(ConversionResult_.jsonKeysCount), filter.jsonKeysCountMin(), filter.jsonKeysCountMax());

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    private <T extends Comparable<? super T>> void addRange(List<Predicate> predicates,
                                                            CriteriaBuilder cb,
                                                            Path<T> path,
                                                            T from,
                                                            T to) {
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(path, from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(path, to));
        }
    }
}