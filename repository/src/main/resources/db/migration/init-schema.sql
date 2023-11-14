
CREATE SEQUENCE archive_type_seq AS BIGINT START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE license_seq AS BIGINT START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE package_alias_seq AS BIGINT START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE package_dependency_seq AS BIGINT START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE package_info_seq AS BIGINT START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE package_instance_seq AS BIGINT START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE package_type_seq AS BIGINT START WITH 1 INCREMENT BY 50;

CREATE TABLE archive_type
(
    archive_type_id SMALLINT     NOT NULL,
    name            VARCHAR(255) NOT NULL,
    CONSTRAINT pk_archive_type PRIMARY KEY (archive_type_id)
);

CREATE TABLE license
(
    license_id BIGINT NOT NULL,
    name       VARCHAR(255),
    CONSTRAINT pk_license PRIMARY KEY (license_id)
);

CREATE TABLE package_alias
(
    alias_id                BIGINT NOT NULL,
    name                    VARCHAR(255),
    package_info_package_id BIGINT,
    CONSTRAINT pk_package_alias PRIMARY KEY (alias_id)
);

CREATE TABLE package_dependency
(
    package_dependency_id          BIGINT NOT NULL,
    target_package_instance_id     BIGINT,
    dependency_package_instance_id BIGINT,
    CONSTRAINT pk_package_dependency PRIMARY KEY (package_dependency_id)
);

CREATE TABLE package_info
(
    package_id      BIGINT NOT NULL,
    name            VARCHAR(255),
    status          BIGINT,
    publisher_id    BIGINT,
    license_id      BIGINT,
    package_type_id BIGINT,
    CONSTRAINT pk_package_info PRIMARY KEY (package_id)
);

CREATE TABLE package_instance
(
    package_instance_id BIGINT NOT NULL,
    package_id          BIGINT NOT NULL,
    version             VARCHAR(255) NOT NULL,
    publication_time    TIMESTAMP,
    CONSTRAINT pk_package_instance PRIMARY KEY (package_instance_id)
);

CREATE TABLE package_instance_archive
(
    payload_size        BIGINT,
    payload_path        VARCHAR(255),
    package_instance_id BIGINT   NOT NULL,
    archive_type_id     SMALLINT NOT NULL,
    CONSTRAINT pk_package_instance_archive PRIMARY KEY (package_instance_id, archive_type_id)
);

CREATE TABLE package_type
(
    package_type_id BIGINT NOT NULL,
    name            VARCHAR(255),
    CONSTRAINT pk_package_type PRIMARY KEY (package_type_id)
);

ALTER TABLE license
    ADD CONSTRAINT uc_license_name UNIQUE (name);

ALTER TABLE package_alias
    ADD CONSTRAINT uc_package_alias_name UNIQUE (name);

ALTER TABLE package_info
    ADD CONSTRAINT uc_package_info_name UNIQUE (name);

ALTER TABLE package_instance
    ADD CONSTRAINT uc_package_instance_version UNIQUE (version);

ALTER TABLE package_alias
    ADD CONSTRAINT FK_PACKAGE_ALIAS_ON_PACKAGEINFO_PACKAGE FOREIGN KEY (package_info_package_id) REFERENCES package_info (package_id);

ALTER TABLE package_dependency
    ADD CONSTRAINT FK_PACKAGE_DEPENDENCY_ON_DEPENDENCY_PACKAGE_INSTANCE FOREIGN KEY (dependency_package_instance_id) REFERENCES package_instance (package_instance_id);

ALTER TABLE package_dependency
    ADD CONSTRAINT FK_PACKAGE_DEPENDENCY_ON_TARGET_PACKAGE_INSTANCE FOREIGN KEY (target_package_instance_id) REFERENCES package_instance (package_instance_id);

ALTER TABLE package_info
    ADD CONSTRAINT FK_PACKAGE_INFO_ON_LICENSE FOREIGN KEY (license_id) REFERENCES license (license_id);

ALTER TABLE package_info
    ADD CONSTRAINT FK_PACKAGE_INFO_ON_PACKAGE_TYPE FOREIGN KEY (package_type_id) REFERENCES package_type (package_type_id);

ALTER TABLE package_instance_archive
    ADD CONSTRAINT FK_PACKAGE_INSTANCE_ARCHIVE_ON_ARCHIVE_TYPE FOREIGN KEY (archive_type_id) REFERENCES archive_type (archive_type_id);

ALTER TABLE package_instance_archive
    ADD CONSTRAINT FK_PACKAGE_INSTANCE_ARCHIVE_ON_PACKAGE_INSTANCE FOREIGN KEY (package_instance_id) REFERENCES package_instance (package_instance_id);

ALTER TABLE package_instance
    ADD CONSTRAINT FK_PACKAGE_INSTANCE_ON_PACKAGE FOREIGN KEY (package_id) REFERENCES package_info (package_id);