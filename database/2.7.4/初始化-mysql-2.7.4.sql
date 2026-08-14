/*Create table*/
-- Store basic information and online status of national standard equipment
drop table IF EXISTS wvp_device;
create table IF NOT EXISTS wvp_device
(
    id                                  serial primary key COMMENT 'primary keyID',
    device_id                           character varying(50) not null COMMENT 'National standard equipment number',
    name                                character varying(255) COMMENT 'Device name',
    manufacturer                        character varying(255) COMMENT 'Equipment manufacturer',
    model                               character varying(255) COMMENT 'Device model',
    firmware                            character varying(255) COMMENT 'Firmware version number',
    transport                           character varying(50) COMMENT 'signaling transfer protocol（TCP/UDP）',
    stream_mode                         character varying(50) COMMENT 'Pull method (active/Passive）',
    on_line                             bool    default false COMMENT 'online status',
    ip                                  character varying(50) COMMENT 'Device IP address',
    create_time                         character varying(50) COMMENT 'creation time',
    update_time                         character varying(50) COMMENT 'Update time',
    port                                integer COMMENT 'Signaling port',
    expires                             integer COMMENT 'Registration validity period',
    subscribe_cycle_for_catalog         integer DEFAULT 0 COMMENT 'Directory subscription cycle',
    subscribe_cycle_for_mobile_position integer DEFAULT 0 COMMENT 'Mobile location subscription period',
    mobile_position_submission_interval integer DEFAULT 5 COMMENT 'Mobile location reporting interval',
    subscribe_cycle_for_alarm           integer DEFAULT 0 COMMENT 'Alarm subscription period',
    host_address                        character varying(50) COMMENT 'Device domain name/host address',
    charset                             character varying(50) COMMENT 'Signaling character set',
    ssrc_check                          bool    default false COMMENT 'Whether to verifySSRC',
    geo_coord_sys                       character varying(50) COMMENT 'Coordinate system type',
    media_server_id                     character varying(50) default 'auto' COMMENT 'Bundled streaming servicesID',
    custom_name                         character varying(255) COMMENT 'Custom display name',
    sdp_ip                              character varying(50) COMMENT 'SDPcarried inIP',
    local_ip                            character varying(50) COMMENT 'local area networkIP',
    password                            character varying(255) COMMENT 'Device authentication password',
    as_message_channel                  bool    default false COMMENT 'Whether to serve as a message channel',
    heart_beat_interval                 integer COMMENT 'heartbeat interval',
    heart_beat_count                    integer COMMENT 'The number of failed heartbeats',
    position_capability                 integer COMMENT 'Positioning capability identifier',
    broadcast_push_after_ack            bool    default false COMMENT 'ACKWhether to automatically push the stream after',
    server_id                           character varying(50) COMMENT 'Belonging signaling serverID',
    constraint uk_device_device unique (device_id)
);

-- Record alarm information reported by each device
drop table IF EXISTS wvp_device_alarm;
create table IF NOT EXISTS wvp_device_alarm
(
    id                serial primary key COMMENT 'primary keyID',
    device_id         character varying(50) not null COMMENT 'National standard equipmentID',
    channel_id        character varying(50) not null COMMENT 'Alarm associated channelID',
    alarm_priority    character varying(50) COMMENT 'Alarm level',
    alarm_method      character varying(50) COMMENT 'Alarm method (video/Voice, etc.）',
    alarm_time        character varying(50) COMMENT 'Alarm occurrence time',
    alarm_description character varying(255) COMMENT 'Alarm description',
    longitude         double precision COMMENT 'Alarm longitude',
    latitude          double precision COMMENT 'Alarm latitude',
    alarm_type        character varying(50) COMMENT 'Alarm type',
    create_time       character varying(50) not null COMMENT 'Data storage time'
);

-- Store data reported by mobile location subscription
drop table IF EXISTS wvp_mobile_position;
create table IF NOT EXISTS wvp_mobile_position
(
    id              serial primary key COMMENT 'primary keyID',
    channel_id      character varying(50) not null COMMENT 'channelID',
    timestamp       BIGINT COMMENT 'Reporting time',
    longitude       double precision COMMENT 'longitude',
    latitude        double precision COMMENT 'Latitude',
    altitude        double precision COMMENT 'altitude',
    speed           double precision COMMENT 'speed',
    direction       double precision COMMENT 'direction angle',
    create_time     character varying(50) COMMENT 'Storage time'
);

-- Save channel information and extended attributes under the device
drop table IF EXISTS wvp_device_channel;
create table IF NOT EXISTS wvp_device_channel
(
    id                           serial primary key COMMENT 'primary keyID',
    device_id                    character varying(50) COMMENT 'Owned equipmentID',
    name                         character varying(255) COMMENT 'Channel name',
    manufacturer                 character varying(50) COMMENT 'Manufacturer',
    model                        character varying(50) COMMENT 'Model',
    owner                        character varying(50) COMMENT 'Belonging unit',
    civil_code                   character varying(50) COMMENT 'Administrative division code',
    block                        character varying(50) COMMENT 'area/Community number',
    address                      character varying(50) COMMENT 'Installation address',
    parental                     integer COMMENT 'Whether there are child nodes',
    parent_id                    character varying(50) COMMENT 'parent channelID',
    safety_way                   integer COMMENT 'Security level',
    register_way                 integer COMMENT 'Registration method',
    cert_num                     character varying(50) COMMENT 'Certificate number',
    certifiable                  integer COMMENT 'Is it certifiable?',
    err_code                     integer COMMENT 'Fault status code',
    end_time                     character varying(50) COMMENT 'Service deadline',
    secrecy                      integer COMMENT 'Confidentiality level',
    ip_address                   character varying(50) COMMENT 'Device IP address',
    port                         integer COMMENT 'Device port',
    password                     character varying(255) COMMENT 'access password',
    status                       character varying(50) COMMENT 'online status',
    longitude                    double precision COMMENT 'longitude',
    latitude                     double precision COMMENT 'Latitude',
    ptz_type                     integer COMMENT 'PTZ type',
    position_type                integer COMMENT 'Point type',
    room_type                    integer COMMENT 'room type',
    use_type                     integer COMMENT 'Nature of use',
    supply_light_type            integer COMMENT 'Fill light method',
    direction_type               integer COMMENT 'towards',
    resolution                   character varying(255) COMMENT 'resolution',
    business_group_id            character varying(255) COMMENT 'business groupingID',
    download_speed               character varying(255) COMMENT 'Download/Stream rate',
    svc_space_support_mod        integer COMMENT 'Airspace SVC capabilities',
    svc_time_support_mode        integer COMMENT 'Time domain SVC capability',
    create_time                  character varying(50) not null COMMENT 'creation time',
    update_time                  character varying(50) not null COMMENT 'Update time',
    sub_count                    integer COMMENT 'Number of child nodes',
    stream_id                    character varying(255) COMMENT 'bound streamID',
    has_audio                    bool default false COMMENT 'Is there audio',
    gps_time                     character varying(50) COMMENT 'GPSPositioning time',
    stream_identification        character varying(50) COMMENT 'stream identifier',
    channel_type                 int  default 0 not null COMMENT 'Channel type',
    map_level                    int  default 0 COMMENT 'Map level',
    gb_device_id                 character varying(50) COMMENT 'GBequipment withinID',
    gb_name                      character varying(255) COMMENT 'GBReported name',
    gb_manufacturer              character varying(255) COMMENT 'GBManufacturer',
    gb_model                     character varying(255) COMMENT 'GBModel',
    gb_owner                     character varying(255) COMMENT 'GBBelong',
    gb_civil_code                character varying(255) COMMENT 'GBAdministrative division',
    gb_block                     character varying(255) COMMENT 'GBarea',
    gb_address                   character varying(255) COMMENT 'GBaddress',
    gb_parental                  integer COMMENT 'GBchild node identifier',
    gb_parent_id                 character varying(255) COMMENT 'GBparent channel',
    gb_safety_way                integer COMMENT 'GBSecurity precautions',
    gb_register_way              integer COMMENT 'GBRegistration method',
    gb_cert_num                  character varying(50) COMMENT 'GBCertificate number',
    gb_certifiable               integer COMMENT 'GBCertification mark',
    gb_err_code                  integer COMMENT 'GBerror code',
    gb_end_time                  character varying(50) COMMENT 'GBDeadline',
    gb_secrecy                   integer COMMENT 'GBConfidentiality level',
    gb_ip_address                character varying(50) COMMENT 'GB IP',
    gb_port                      integer COMMENT 'GBport',
    gb_password                  character varying(50) COMMENT 'GBAccess password',
    gb_status                    character varying(50) COMMENT 'GBStatus',
    gb_longitude                 double COMMENT 'GBlongitude',
    gb_latitude                  double COMMENT 'GBLatitude',
    gb_business_group_id         character varying(50) COMMENT 'GBbusiness grouping',
    gb_ptz_type                  integer COMMENT 'GBPTZ type',
    gb_position_type             integer COMMENT 'GBPoint type',
    gb_room_type                 integer COMMENT 'GBroom type',
    gb_use_type                  integer COMMENT 'GBPurpose',
    gb_supply_light_type         integer COMMENT 'GBfill light',
    gb_direction_type            integer COMMENT 'GBtowards',
    gb_resolution                character varying(255) COMMENT 'GBresolution',
    gb_download_speed            character varying(255) COMMENT 'GBStream rate',
    gb_svc_space_support_mod     integer COMMENT 'GBairspaceSVC',
    gb_svc_time_support_mode     integer COMMENT 'GBtime domainSVC',
    record_plan_id               integer COMMENT 'Bind recording planID',
    data_type                    integer not null COMMENT 'data type identifier',
    data_device_id               integer not null COMMENT 'Data source device primary key',
    gps_speed                    double precision COMMENT 'GPSspeed',
    gps_altitude                 double precision COMMENT 'GPSaltitude',
    gps_direction                double precision COMMENT 'GPSdirection',
    enable_broadcast             integer default 0 COMMENT 'Whether to support broadcast',
    index (data_type),
    index (data_device_id),
    constraint uk_wvp_unique_channel unique (gb_device_id),
    constraint uk_device_channel_source unique (data_device_id, device_id)
);

-- Media server (such as ZLM) node information
drop table IF EXISTS wvp_media_server;
create table IF NOT EXISTS wvp_media_server
(
    id                  character varying(255) primary key COMMENT 'media serverID',
    ip                  character varying(50) COMMENT 'serverIP',
    hook_ip             character varying(50) COMMENT 'hookcallbackIP',
    sdp_ip              character varying(50) COMMENT 'SDPused inIP',
    stream_ip           character varying(50) COMMENT 'Used for push streamingIP',
    http_port           integer COMMENT 'HTTPport',
    http_ssl_port       integer COMMENT 'HTTPSport',
    rtmp_port           integer COMMENT 'RTMPport',
    rtmp_ssl_port       integer COMMENT 'RTMPSport',
    rtp_proxy_port      integer COMMENT 'RTPproxy port',
    rtsp_port           integer COMMENT 'RTSPport',
    rtsp_ssl_port       integer COMMENT 'RTSPSport',
    flv_port            integer COMMENT 'FLVport',
    flv_ssl_port        integer COMMENT 'FLV HTTPSport',
    mp4_port            integer COMMENT 'MP4On-demand port',
    mp4_ssl_port        integer COMMENT 'MP4 HTTPSport',
    ws_flv_port         integer COMMENT 'WS-FLVport',
    ws_flv_ssl_port     integer COMMENT 'WS-FLV HTTPSport',
    jtt_proxy_port      integer COMMENT 'JT/Tproxy port',
    auto_config         bool                  default false COMMENT 'Whether to automatically configure',
    secret              character varying(50) COMMENT 'ZLMVerification key',
    type                character varying(50) default 'zlm' COMMENT 'Node type',
    rtp_enable          bool                  default false COMMENT 'Whether to turn onRTP',
    rtp_port_range      character varying(50) COMMENT 'RTPport range',
    send_rtp_port_range character varying(50) COMMENT 'Send RTP port range',
    record_assist_port  integer COMMENT 'Video auxiliary port',
    default_server      bool                  default false COMMENT 'Whether the default node',
    create_time         character varying(50) COMMENT 'creation time',
    update_time         character varying(50) COMMENT 'Update time',
    hook_alive_interval integer COMMENT 'hookheartbeat interval',
    record_path         character varying(255) COMMENT 'Video directory',
    record_day          integer               default 7 COMMENT 'Video retention days',
    transcode_suffix    character varying(255) COMMENT 'Transcoding command suffix',
    server_id           character varying(50) COMMENT 'Corresponding signaling serverID'
);

-- Registration information of superior national standard platform
drop table IF EXISTS wvp_platform;
create table IF NOT EXISTS wvp_platform
(
    id                    serial primary key COMMENT 'primary keyID',
    enable                bool default false COMMENT 'Whether to enable registration for this platform',
    name                  character varying(255) COMMENT 'Platform name',
    server_gb_id          character varying(50) COMMENT 'National standard code of superior platform',
    server_gb_domain      character varying(50) COMMENT 'Upper level platform domain encoding',
    server_ip             character varying(50) COMMENT 'Superior platformIP',
    server_port           integer COMMENT 'Upper level platform registration port',
    device_gb_id          character varying(50) COMMENT 'The national standard code registered on this platform',
    device_ip             character varying(50) COMMENT 'Signaling on this platformIP',
    device_port           character varying(50) COMMENT 'Signaling port of this platform',
    username              character varying(255) COMMENT 'Register username',
    password              character varying(50) COMMENT 'Registration password',
    expires               character varying(50) COMMENT 'Registration validity period',
    keep_timeout          character varying(50) COMMENT 'Heartbeat timeout',
    transport             character varying(50) COMMENT 'transport protocol（UDP/TCP）',
    civil_code            character varying(50) COMMENT 'Administrative division code',
    manufacturer          character varying(255) COMMENT 'Manufacturer',
    model                 character varying(255) COMMENT 'Model',
    address               character varying(255) COMMENT 'address',
    character_set         character varying(50) COMMENT 'character set',
    ptz                   bool default false COMMENT 'Whether to supportPTZ',
    rtcp                  bool default false COMMENT 'Whether to turn onRTCP',
    status                bool default false COMMENT 'Registration status',
    catalog_group         integer COMMENT 'Directory grouping method',
    register_way          integer COMMENT 'Registration method',
    secrecy               integer COMMENT 'Confidentiality level',
    create_time           character varying(50) COMMENT 'creation time',
    update_time           character varying(50) COMMENT 'Update time',
    as_message_channel    bool default false COMMENT 'Whether to serve as a message channel',
    catalog_with_platform integer default 1 COMMENT 'Whether to push the platform directory',
    catalog_with_group    integer default 1 COMMENT 'Whether to push the group directory',
    catalog_with_region   integer default 1 COMMENT 'Whether to push the regional directory',
    auto_push_channel     bool default true COMMENT 'Whether to automatically push channels',
    send_stream_ip        character varying(50) COMMENT 'Used when pushingIP',
    server_id             character varying(50) COMMENT 'Corresponding signaling serverID',
    constraint uk_platform_unique_server_gb_id unique (server_gb_id)
);

-- Channel mapping relationship issued by the national standard platform
drop table IF EXISTS wvp_platform_channel;
create table IF NOT EXISTS wvp_platform_channel
(
    id                           serial primary key COMMENT 'primary keyID',
    platform_id                  integer COMMENT 'platformID',
    device_channel_id            integer COMMENT 'Local channel table primary key',
    custom_device_id             character varying(50) COMMENT 'Customized national standard coding',
    custom_name                  character varying(255) COMMENT 'custom name',
    custom_manufacturer          character varying(50) COMMENT 'Custom manufacturer',
    custom_model                 character varying(50) COMMENT 'Custom model',
    custom_owner                 character varying(50) COMMENT 'Custom attribution',
    custom_civil_code            character varying(50) COMMENT 'Custom administrative divisions',
    custom_block                 character varying(50) COMMENT 'Custom area',
    custom_address               character varying(50) COMMENT 'Custom address',
    custom_parental              integer COMMENT 'Custom parent/sub-id',
    custom_parent_id             character varying(50) COMMENT 'Custom parent node',
    custom_safety_way            integer COMMENT 'Custom security protection',
    custom_register_way          integer COMMENT 'Custom registration method',
    custom_cert_num              character varying(50) COMMENT 'Custom certificate number',
    custom_certifiable           integer COMMENT 'Custom certifiable mark',
    custom_err_code              integer COMMENT 'Custom error code',
    custom_end_time              character varying(50) COMMENT 'Custom deadline',
    custom_secrecy               integer COMMENT 'Customized confidentiality level',
    custom_ip_address            character varying(50) COMMENT 'CustomizeIP',
    custom_port                  integer COMMENT 'Custom port',
    custom_password              character varying(255) COMMENT 'Custom password',
    custom_status                character varying(50) COMMENT 'Custom status',
    custom_longitude             double precision COMMENT 'Custom longitude',
    custom_latitude              double precision COMMENT 'Custom latitude',
    custom_ptz_type              integer COMMENT 'Custom PTZ type',
    custom_position_type         integer COMMENT 'Custom point type',
    custom_room_type             integer COMMENT 'Custom room type',
    custom_use_type              integer COMMENT 'Custom use',
    custom_supply_light_type     integer COMMENT 'Custom fill light',
    custom_direction_type        integer COMMENT 'Custom orientation',
    custom_resolution            character varying(255) COMMENT 'Custom resolution',
    custom_business_group_id     character varying(255) COMMENT 'Custom business grouping',
    custom_download_speed        character varying(255) COMMENT 'Custom stream rate',
    custom_svc_space_support_mod integer COMMENT 'Custom airspaceSVC',
    custom_svc_time_support_mode integer COMMENT 'Custom time domainSVC',
    constraint uk_platform_gb_channel_platform_id_catalog_id_device_channel_id unique (platform_id, device_channel_id),
    constraint uk_platform_gb_channel_device_id unique (custom_device_id)
);

-- Platforms and Groups (Administrative Divisions/organization) relationship
drop table IF EXISTS wvp_platform_group;
create table IF NOT EXISTS wvp_platform_group
(
    id          serial primary key COMMENT 'primary keyID',
    platform_id integer COMMENT 'platformID',
    group_id    integer COMMENT 'GroupID',
    constraint uk_wvp_platform_group_platform_id_group_id unique (platform_id, group_id)
);

-- Platform and regional relations
drop table IF EXISTS wvp_platform_region;
create table IF NOT EXISTS wvp_platform_region
(
    id          serial primary key COMMENT 'primary keyID',
    platform_id integer COMMENT 'platformID',
    region_id   integer COMMENT 'areaID',
    constraint uk_wvp_platform_region_platform_id_group_id unique (platform_id, region_id)
);

-- Streaming agent/Retweet configuration
drop table IF EXISTS wvp_stream_proxy;
create table IF NOT EXISTS wvp_stream_proxy
(
    id                         serial primary key COMMENT 'primary keyID',
    type                       character varying(50) COMMENT 'Agent type (pull flow/Push streaming）',
    app                        character varying(255) COMMENT 'Application name',
    stream                     character varying(255) COMMENT 'flowID',
    src_url                    character varying(255) COMMENT 'Source address',
    timeout                    integer COMMENT 'Pull timeout',
    ffmpeg_cmd_key             character varying(255) COMMENT 'FFmpegcommand template key',
    rtsp_type                  character varying(50) COMMENT 'RTSPPull method',
    media_server_id            character varying(50) COMMENT 'Specify media serverID',
    enable_audio               bool default false COMMENT 'Whether to enable audio',
    enable_mp4                 bool default false COMMENT 'Record or notMP4',
    pulling                    bool default false COMMENT 'Is the stream currently being pulled?',
    enable                     bool default false COMMENT 'Whether to enable the proxy',
    create_time                character varying(50) COMMENT 'creation time',
    name                       character varying(255) COMMENT 'Agent name',
    update_time                character varying(50) COMMENT 'Update time',
    stream_key                 character varying(255) COMMENT 'Unique stream identifier',
    server_id                  character varying(50) COMMENT 'signaling serverID',
    enable_disable_none_reader bool default false COMMENT 'Whether to automatically stop streaming when no one is watching',
    relates_media_server_id    character varying(50) COMMENT 'Associated media serverID',
    constraint uk_stream_proxy_app_stream unique (app, stream)
);

-- Push session record
drop table IF EXISTS wvp_stream_push;
create table IF NOT EXISTS wvp_stream_push
(
    id                 serial primary key COMMENT 'primary keyID',
    app                character varying(255) COMMENT 'Application name',
    stream             character varying(255) COMMENT 'flowID',
    create_time        character varying(50) COMMENT 'creation time',
    media_server_id    character varying(50) COMMENT 'The media server where the stream is located',
    server_id          character varying(50) COMMENT 'signaling serverID',
    push_time          character varying(50) COMMENT 'Push start time',
    status             bool default false COMMENT 'Push status',
    update_time        character varying(50) COMMENT 'Update time',
    pushing            bool default false COMMENT 'Whether streaming is being pushed',
    self               bool default false COMMENT 'Whether to initiate locally',
    start_offline_push bool default true COMMENT 'Whether to automatically re-push after offline',
    constraint uk_stream_push_app_stream unique (app, stream)
);

-- Cloud video recording
drop table IF EXISTS wvp_cloud_record;
create table IF NOT EXISTS wvp_cloud_record
(
    id              serial primary key COMMENT 'primary keyID',
    app             character varying(255) COMMENT 'Application name',
    stream          character varying(255) COMMENT 'flowID',
    call_id         character varying(255) COMMENT 'sessionID',
    start_time      bigint COMMENT 'Recording start time',
    end_time        bigint COMMENT 'Recording end time',
    media_server_id character varying(50) COMMENT 'media serverID',
    server_id       character varying(50) COMMENT 'signaling serverID',
    file_name       character varying(255) COMMENT 'file name',
    folder          character varying(500) COMMENT 'Directory',
    file_path       character varying(500) COMMENT 'full path',
    collect         bool default false COMMENT 'Whether to collect',
    file_size       bigint COMMENT 'file size',
    time_len        double precision COMMENT 'duration'
);

-- Platform user information
drop table IF EXISTS wvp_user;
create table IF NOT EXISTS wvp_user
(
    id          serial primary key COMMENT 'primary keyID',
    username    character varying(255) COMMENT 'Username',
    password    character varying(255) COMMENT 'Password（MD5）',
    role_id     integer COMMENT 'roleID',
    create_time character varying(50) COMMENT 'creation time',
    update_time character varying(50) COMMENT 'Update time',
    push_key    character varying(50) COMMENT 'push key',
    constraint uk_user_username unique (username)
);

-- User role information
drop table IF EXISTS wvp_user_role;
create table IF NOT EXISTS wvp_user_role
(
    id          serial primary key COMMENT 'primary keyID',
    name        character varying(50) COMMENT 'Character name',
    authority   character varying(50) COMMENT 'Permission ID',
    create_time character varying(50) COMMENT 'creation time',
    update_time character varying(50) COMMENT 'Update time'
);


drop table IF EXISTS wvp_user_api_key;
create table IF NOT EXISTS wvp_user_api_key
(
    id          serial primary key COMMENT 'primary keyID',
    user_id     bigint COMMENT 'Associated usersID',
    app         character varying(255) COMMENT 'Application ID',
    api_key     text COMMENT 'API Key',
    expired_at  bigint COMMENT 'Expiration timestamp',
    remark      character varying(255) COMMENT 'Remarks',
    enable      bool default true COMMENT 'Whether to enable',
    create_time character varying(50) COMMENT 'creation time',
    update_time character varying(50) COMMENT 'Update time'
);


/*initial data*/
-- Initialize the administrator account, account admin and password admin (after MD5 encryption）
INSERT INTO wvp_user
VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 1, '2021-04-13 14:14:57', '2021-04-13 14:14:57',
        '3e80d1762a324d5b0ff636e0bd16f1e3');
-- Initialize administrator role
INSERT INTO wvp_user_role
VALUES (1, 'admin', '0', '2021-04-13 14:14:57', '2021-04-13 14:14:57');

-- General grouping table to store industry or organizational structure
drop table IF EXISTS wvp_common_group;
create table IF NOT EXISTS wvp_common_group
(
    id               serial primary key COMMENT 'primary keyID',
    device_id        varchar(50)  NOT NULL COMMENT 'The platform or device corresponding to the groupID',
    name             varchar(255) NOT NULL COMMENT 'Group name',
    parent_id        int COMMENT 'Parent groupingID',
    parent_device_id varchar(50) DEFAULT NULL COMMENT 'The device corresponding to the parent groupID',
    business_group   varchar(50)  NOT NULL COMMENT 'Business group coding',
    create_time      varchar(50)  NOT NULL COMMENT 'creation time',
    update_time      varchar(50)  NOT NULL COMMENT 'Update time',
    civil_code       varchar(50) default null COMMENT 'Administrative division code',
    alias            varchar(255) default null COMMENT 'Alias',
    constraint uk_common_group_device_platform unique (device_id)
);

-- General administrative area table
drop table IF EXISTS wvp_common_region;
create table IF NOT EXISTS wvp_common_region
(
    id               serial primary key COMMENT 'primary keyID',
    device_id        varchar(50)  NOT NULL COMMENT 'Platform or device corresponding to the regionID',
    name             varchar(255) NOT NULL COMMENT 'area name',
    parent_id        int COMMENT 'parent regionID',
    parent_device_id varchar(50) DEFAULT NULL COMMENT 'Devices in the parent regionID',
    create_time      varchar(50)  NOT NULL COMMENT 'creation time',
    update_time      varchar(50)  NOT NULL COMMENT 'Update time',
    constraint uk_common_region_device_id unique (device_id)
);

-- Basic information of video recording plan
drop table IF EXISTS wvp_record_plan;
create table IF NOT EXISTS wvp_record_plan
(
    id              serial primary key COMMENT 'primary keyID',
    snap            bool default false COMMENT 'Whether to capture the plan',
    name            varchar(255) NOT NULL COMMENT 'Plan name',
    create_time     character varying(50) COMMENT 'creation time',
    update_time     character varying(50) COMMENT 'Update time'
);

-- Recording schedule entry form
drop table IF EXISTS wvp_record_plan_item;
create table IF NOT EXISTS wvp_record_plan_item
(
    id              serial primary key COMMENT 'primary keyID',
    start           int COMMENT 'Start time (minutes）',
    stop            int COMMENT 'end time (minutes）',
    week_day        int COMMENT 'week（0-6）',
    plan_id         int COMMENT 'Belonging video projectID',
    create_time     character varying(50) COMMENT 'creation time',
    update_time     character varying(50) COMMENT 'Update time'
);

-- Ministry of Transport JT/T 1076 Terminal information
drop table IF EXISTS wvp_jt_terminal;
create table IF NOT EXISTS wvp_jt_terminal (
                                 id serial primary key COMMENT 'primary keyID',
                                 phone_number character varying(50) COMMENT 'Terminal SIM card number',
                                 terminal_id character varying(50) COMMENT 'terminal equipmentID',
                                 province_id character varying(50) COMMENT 'ProvinceID',
                                 province_text character varying(100) COMMENT 'Province name',
                                 city_id character varying(50) COMMENT 'CityID',
                                 city_text character varying(100) COMMENT 'City name',
                                 maker_id character varying(50) COMMENT 'ManufacturerID',
                                 model character varying(50) COMMENT 'Terminal model',
                                 plate_color character varying(50) COMMENT 'license plate color',
                                 plate_no character varying(50) COMMENT 'license plate number',
                                 longitude double precision COMMENT 'longitude',
                                 latitude double precision COMMENT 'Latitude',
                                 status bool default false COMMENT 'online status',
                                 register_time character varying(50) default null COMMENT 'Registration time',
                                 update_time character varying(50) not null COMMENT 'Update time',
                                 create_time character varying(50) not null COMMENT 'creation time',
                                 geo_coord_sys character varying(50) COMMENT 'coordinate system',
                                 media_server_id character varying(50) default 'auto' COMMENT 'media serverID',
                                 sdp_ip character varying(50) COMMENT 'SDP IP',
                                 constraint uk_jt_device_id_device_id unique (id, phone_number)
);

-- Ministry of Transport JT/T 1076 Channel information
drop table IF EXISTS wvp_jt_channel;
create table IF NOT EXISTS wvp_jt_channel (
                               id serial primary key COMMENT 'primary keyID',
                               terminal_db_id integer COMMENT 'Belonging terminal recordID',
                               channel_id integer COMMENT 'Channel number',
                               has_audio bool default false COMMENT 'Is there audio',
                               name character varying(255) COMMENT 'Channel name',
                               update_time character varying(50) not null COMMENT 'Update time',
                               create_time character varying(50) not null COMMENT 'creation time',
                               constraint uk_jt_channel_id_device_id unique (terminal_db_id, channel_id)
);

-- Alarm information table, please refer to the alarm class for table structure.
drop table IF EXISTS wvp_alarm;
create table IF NOT EXISTS wvp_alarm (
                          id serial primary key COMMENT 'primary keyID',
                          channel_id integer COMMENT 'Database of associated channelsid',
                          description character varying(255) COMMENT 'Alarm description',
                          snap_path character varying(255) COMMENT 'Alarm snapshot path',
                          record_path character varying(255) COMMENT 'Alarm recording path',
                          longitude double precision COMMENT 'The longitude attached to the alarm',
                          latitude double precision COMMENT 'Latitude attached to alarm',
                          alarm_type integer COMMENT 'Alarm category',
                          alarm_time bigint COMMENT 'Alarm time'
);
