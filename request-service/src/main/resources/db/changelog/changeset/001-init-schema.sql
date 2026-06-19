--liquibase formatted sql

-- changeset shanternal:1
create table conversion_results (
    xml_hash varchar(64) primary key,
    canonical_xml text not null,
    xml_tags_count integer not null
        constraint chk_conversion_results_xml_tags_count check (xml_tags_count > 0),
    target_json text not null,
    json_keys_count integer not null
        constraint chk_conversion_results_json_keys_count check (json_keys_count >= 0)
);

create table processed_requests (
    id bigserial primary key,
    xml_hash varchar(64) not null,
    requested_at timestamp with time zone not null,
    processing_time_ms bigint not null
        constraint chk_processed_requests_processing_time check (processing_time_ms >= 0),
    constraint fk_processed_requests_conversion_result
        foreign key (xml_hash)
            references conversion_results (xml_hash)
);

create index ix_processed_requests_xml_hash
    on processed_requests (xml_hash);

create index ix_processed_requests_requested_at
    on processed_requests (requested_at desc);

-- rollback drop table processed_requests;
-- rollback drop table conversion_results;