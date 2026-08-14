alter table device
    add asMessageChannel int default 0;

alter table parent_platform
    add asMessageChannel int default 0;

alter table device
    add mediaServerId varchar(50) default null;

ALTER TABLE device
    ADD COLUMN `switchPrimarySubStream` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Turn on the switch between main and sub-streams（0-Not open，1-Open) The currently known supported devices are Dahua, TP—all LINK series devices' AFTER `keepalive_interval_time`


