insert into PACKAGES_TYPES (TYPE_NAME) values ('Application');
insert into PACKAGES_TYPES (TYPE_NAME) values ('Library');
insert into PACKAGES_TYPES (TYPE_NAME) values ('Documentation');
INSERT INTO LICENCES (NAME) VALUES('MIT');
INSERT INTO LICENCES (NAME) VALUES('GNU');
INSERT INTO LICENCES (NAME) VALUES('Apache');
INSERT INTO LICENCES (NAME) VALUES('Bear');
INSERT INTO ARCHIVES VALUES(0,'None');
INSERT INTO ARCHIVES VALUES(1,'Brotli');
INSERT INTO ARCHIVES VALUES(2,'GZIP');
insert into PUBLISHER_INFO (ID, AUTHOR, EMAIL) VALUES (0, 'Paval', '');
insert into PUBLISHER_INFO (ID, AUTHOR, EMAIL) VALUES (1, 'Nikita', '');
insert into PUBLISHER_INFO (ID, AUTHOR, EMAIL) VALUES (2, 'Vova', '');
insert into PACKAGES_HATS (PACKAGE_NAME, PACKAGE_TYPE, AUTHOR_ID) values ('PetOS', 0, 0);
insert into PACKAGES_ALIASES (HAT_ID, ALIAS) VALUES (0, 'kernel');
insert into PACKAGES_HATS (PACKAGE_NAME, PACKAGE_TYPE, AUTHOR_ID) VALUES ('rust-utils', 1, 0);
insert into PACKAGES_INFO (PACKAGE_ID, VERSION_ID, LICENCE_TYPE, VERSION_LABEL) VALUES (0, 1, 0, '0.0.1');
insert into PACKAGES_INFO (PACKAGE_ID, VERSION_ID, LICENCE_TYPE, VERSION_LABEL) values (1, 1, 1, '0.0.2');
insert into DEPENDENCIES (PACKAGE_ID, VERSION_ID, DEPENDENCY_PACKAGE, DEPENDENCY_VERSION) VALUES (1, 1, 2, 1);