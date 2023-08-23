insert into PACKAGES_TYPES (TYPE_NAME) values ('Application');
insert into PACKAGES_TYPES (TYPE_NAME) values ('Library');
insert into PACKAGES_TYPES (TYPE_NAME) values ('Documentation');
insert into PUBLISHERS_INFO (ID, AUTHOR, EMAIL, HASH, SALT) VALUES (0, 'Paval', '', '42', '42');
insert into LICENCES (NAME) values ('GNU');
insert into PACKAGES_HATS (PACKAGE_NAME, PACKAGE_TYPE, AUTHOR_ID) values ('PetOS', 0, 0);
insert into PACKAGES_INFO (PACKAGE_ID, VERSION_ID, LICENCE_TYPE, VERSION_LABEL) VALUES (1, 1, 0, '0.0.1')