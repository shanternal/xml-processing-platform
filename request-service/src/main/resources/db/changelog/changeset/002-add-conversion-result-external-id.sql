--liquibase formatted sql

-- changeset shanternal:2
alter table conversion_results
    alter column canonical_xml drop not null,
    alter column target_json drop not null;

alter table conversion_results
    add column external_id varchar(36);

alter table conversion_results
    add constraint uk_conversion_results_external_id
        unique (external_id);

alter table conversion_results
    add constraint chk_conversion_results_migration_state
        check (
            (external_id is null
                and canonical_xml is not null
                and target_json is not null)
            or
            (external_id is not null
                and canonical_xml is null
                and target_json is null)
        );

create index ix_conversion_results_unmigrated
    on conversion_results (xml_hash)
    where external_id is null;

-- rollback drop index ix_conversion_results_unmigrated;
-- rollback alter table conversion_results drop constraint chk_conversion_results_migration_state;
-- rollback alter table conversion_results drop constraint uk_conversion_results_external_id;
-- rollback alter table conversion_results drop column external_id;
-- rollback alter table conversion_results alter column canonical_xml set not null;
-- rollback alter table conversion_results alter column target_json set not null;
