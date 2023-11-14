insert into PUBLIC.ARCHIVE_TYPE (ARCHIVE_TYPE_ID, NAME)
values (1, 'gzip'),
       (2, 'brotli'),
       (3, 'lzma');
insert into PUBLIC.PACKAGE_TYPE (PACKAGE_TYPE_ID, NAME)
values (1, 'bin'),
       (2, 'lib'),
       (3, 'docs');
insert into PUBLIC.LICENSE (LICENSE_ID, NAME)
VALUES (1, 'GNU'),
       (2, 'MIT'),
       (3, 'BEAR');
insert into PUBLIC.PACKAGE_INFO(PACKAGE_ID, NAME, PUBLISHER_ID, STATUS, LICENSE_ID, PACKAGE_TYPE_ID)
VALUES (1, 'PetOS Kernel', 1, 1, 1, 1),
       (2, 'PUM', 1, 1, 2, 1),
       (3, 'PetOS Music', 1, 1, 1, 1),
       (4, 'Rust Standard Library', 2, 1, 3, 2);
insert into PACKAGE_ALIAS(ALIAS_ID, NAME, PACKAGE_INFO_PACKAGE_ID)
VALUES (1, 'kernel', 1),
       (2, 'music', 3),
       (3, 'rust-std', 4);
insert into PUBLIC.PACKAGE_INSTANCE(PACKAGE_INSTANCE_ID, PUBLICATION_TIME, VERSION, PACKAGE_ID)
VALUES (1, CURRENT_TIMESTAMP, '0.0.2', 1),
       (2, CURRENT_TIMESTAMP, '0.0.1', 2),
       (3, CURRENT_TIMESTAMP, '0.0.3', 3),
       (4, CURRENT_TIMESTAMP, '0.0.2', 4);
insert into PACKAGE_DEPENDENCY(PACKAGE_DEPENDENCY_ID, DEPENDENCY_PACKAGE_INSTANCE_ID, TARGET_PACKAGE_INSTANCE_ID)
VALUES (1, 4, 3),
       (2, 4, 2);
insert into PACKAGE_INSTANCE_ARCHIVE (ARCHIVE_TYPE_ID, PACKAGE_INSTANCE_ID, PAYLOAD_PATH, PAYLOAD_SIZE)
values (1, 1, '/home/worker/downloads/kernel.bin', 10000),
       (1, 2, '/home/worker/downloads/pum.bin', 6000),
       (1, 3, '/home/worker/downloads/music.bin', 8000),
       (1, 4, '/home/worker/downloads/rust-std.so', 20000);
