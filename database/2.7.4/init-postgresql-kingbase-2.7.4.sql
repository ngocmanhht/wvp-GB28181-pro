/*Create table*/
drop table IF EXISTS wvp_device;
create table IF NOT EXISTS wvp_device
(
    id                                  serial primary key,
    device_id                           character varying(50) not null,
    name                                character varying(255),
    manufacturer                        character varying(255),
    model                               character varying(255),
    firmware                            character varying(255),
    transport                           character varying(50),
    stream_mode                         character varying(50),
    on_line                             bool    default false,
    ip                                  character varying(50),
    create_time                         character varying(50),
    update_time                         character varying(50),
    port                                integer,
    expires                             integer,
    subscribe_cycle_for_catalog         integer DEFAULT 0,
    subscribe_cycle_for_mobile_position integer DEFAULT 0,
    mobile_position_submission_interval integer DEFAULT 5,
    subscribe_cycle_for_alarm           integer DEFAULT 0,
    host_address                        character varying(50),
    charset                             character varying(50),
    ssrc_check                          bool    default false,
    geo_coord_sys                       character varying(50),
    media_server_id                     character varying(50) default 'auto',
    custom_name                         character varying(255),
    sdp_ip                              character varying(50),
    local_ip                            character varying(50),
    password                            character varying(255),
    as_message_channel                  bool    default false,
    heart_beat_interval                 integer,
    heart_beat_count                    integer,
    position_capability                 integer,
    broadcast_push_after_ack            bool    default false,
    server_id                           character varying(50),
    constraint uk_device_device unique (device_id)
);
COMMENT ON TABLE wvp_device IS 'Store basic information and online status of national standard equipment';
COMMENT ON COLUMN wvp_device.id IS 'primary keyID';
COMMENT ON COLUMN wvp_device.device_id IS 'National standard equipment number';
COMMENT ON COLUMN wvp_device.name IS 'Device name';
COMMENT ON COLUMN wvp_device.manufacturer IS 'Equipment manufacturer';
COMMENT ON COLUMN wvp_device.model IS 'Device model';
COMMENT ON COLUMN wvp_device.firmware IS 'Firmware version number';
COMMENT ON COLUMN wvp_device.transport IS 'signaling transfer protocol（TCP/UDP）';
COMMENT ON COLUMN wvp_device.stream_mode IS 'Pull method (active/Passive）';
COMMENT ON COLUMN wvp_device.on_line IS 'online status';
COMMENT ON COLUMN wvp_device.ip IS 'Device IP address';
COMMENT ON COLUMN wvp_device.create_time IS 'creation time';
COMMENT ON COLUMN wvp_device.update_time IS 'Update time';
COMMENT ON COLUMN wvp_device.port IS 'Signaling port';
COMMENT ON COLUMN wvp_device.expires IS 'Registration validity period';
COMMENT ON COLUMN wvp_device.subscribe_cycle_for_catalog IS 'Directory subscription cycle';
COMMENT ON COLUMN wvp_device.subscribe_cycle_for_mobile_position IS 'Mobile location subscription period';
COMMENT ON COLUMN wvp_device.mobile_position_submission_interval IS 'Mobile location reporting interval';
COMMENT ON COLUMN wvp_device.subscribe_cycle_for_alarm IS 'Alarm subscription period';
COMMENT ON COLUMN wvp_device.host_address IS 'Device domain name/host address';
COMMENT ON COLUMN wvp_device.charset IS 'Signaling character set';
COMMENT ON COLUMN wvp_device.ssrc_check IS 'Whether to verifySSRC';
COMMENT ON COLUMN wvp_device.geo_coord_sys IS 'Coordinate system type';
COMMENT ON COLUMN wvp_device.media_server_id IS 'Bundled streaming servicesID';
COMMENT ON COLUMN wvp_device.custom_name IS 'Custom display name';
COMMENT ON COLUMN wvp_device.sdp_ip IS 'SDPcarried inIP';
COMMENT ON COLUMN wvp_device.local_ip IS 'local area networkIP';
COMMENT ON COLUMN wvp_device.password IS 'Device authentication password';
COMMENT ON COLUMN wvp_device.as_message_channel IS 'Whether to serve as a message channel';
COMMENT ON COLUMN wvp_device.heart_beat_interval IS 'heartbeat interval';
COMMENT ON COLUMN wvp_device.heart_beat_count IS 'The number of failed heartbeats';
COMMENT ON COLUMN wvp_device.position_capability IS 'Positioning capability identifier';
COMMENT ON COLUMN wvp_device.broadcast_push_after_ack IS 'ACKWhether to automatically push the stream after';
COMMENT ON COLUMN wvp_device.server_id IS 'Belonging signaling serverID';


drop table IF EXISTS wvp_device_alarm;
create table IF NOT EXISTS wvp_device_alarm
(
    id                serial primary key,
    device_id         character varying(50) not null,
    channel_id        character varying(50) not null,
    alarm_priority    character varying(50),
    alarm_method      character varying(50),
    alarm_time        character varying(50),
    alarm_description character varying(255),
    longitude         double precision,
    latitude          double precision,
    alarm_type        character varying(50),
    create_time       character varying(50) not null
);
COMMENT ON TABLE wvp_device_alarm IS 'Record alarm information reported by each device';
COMMENT ON COLUMN wvp_device_alarm.id IS 'primary keyID';
COMMENT ON COLUMN wvp_device_alarm.device_id IS 'National standard equipmentID';
COMMENT ON COLUMN wvp_device_alarm.channel_id IS 'Alarm associated channelID';
COMMENT ON COLUMN wvp_device_alarm.alarm_priority IS 'Alarm level';
COMMENT ON COLUMN wvp_device_alarm.alarm_method IS 'Alarm method (video/Voice, etc.）';
COMMENT ON COLUMN wvp_device_alarm.alarm_time IS 'Alarm occurrence time';
COMMENT ON COLUMN wvp_device_alarm.alarm_description IS 'Alarm description';
COMMENT ON COLUMN wvp_device_alarm.longitude IS 'Alarm longitude';
COMMENT ON COLUMN wvp_device_alarm.latitude IS 'Alarm latitude';
COMMENT ON COLUMN wvp_device_alarm.alarm_type IS 'Alarm type';
COMMENT ON COLUMN wvp_device_alarm.create_time IS 'Data storage time';


drop table IF EXISTS wvp_mobile_position;
create table IF NOT EXISTS wvp_mobile_position
(
    id              serial primary key,
    channel_id      character varying(50) not null,
    timestamp       int8,
    longitude       double precision,
    latitude        double precision,
    altitude        double precision,
    speed           double precision,
    direction       double precision,
    create_time     character varying(50)
);
COMMENT ON TABLE wvp_mobile_position IS 'Store data reported by mobile location subscription';
COMMENT ON COLUMN wvp_mobile_position.id IS 'primary keyID';
COMMENT ON COLUMN wvp_mobile_position.channel_id IS 'channelID';
COMMENT ON COLUMN wvp_mobile_position.timestamp IS 'Reporting time';
COMMENT ON COLUMN wvp_mobile_position.longitude IS 'longitude';
COMMENT ON COLUMN wvp_mobile_position.latitude IS 'Latitude';
COMMENT ON COLUMN wvp_mobile_position.altitude IS 'altitude';
COMMENT ON COLUMN wvp_mobile_position.speed IS 'speed';
COMMENT ON COLUMN wvp_mobile_position.direction IS 'direction angle';
COMMENT ON COLUMN wvp_mobile_position.create_time IS 'Storage time';


drop table IF EXISTS wvp_device_channel;
create table IF NOT EXISTS wvp_device_channel
(
    id                           serial primary key,
    device_id                    character varying(50),
    name                         character varying(255),
    manufacturer                 character varying(50),
    model                        character varying(50),
    owner                        character varying(50),
    civil_code                   character varying(50),
    block                        character varying(50),
    address                      character varying(50),
    parental                     integer,
    parent_id                    character varying(50),
    safety_way                   integer,
    register_way                 integer,
    cert_num                     character varying(50),
    certifiable                  integer,
    err_code                     integer,
    end_time                     character varying(50),
    secrecy                      integer,
    ip_address                   character varying(50),
    port                         integer,
    password                     character varying(255),
    status                       character varying(50),
    longitude                    double precision,
    latitude                     double precision,
    ptz_type                     integer,
    position_type                integer,
    room_type                    integer,
    use_type                     integer,
    supply_light_type            integer,
    direction_type               integer,
    resolution                   character varying(255),
    business_group_id            character varying(255),
    download_speed               character varying(255),
    svc_space_support_mod        integer,
    svc_time_support_mode        integer,
    create_time                  character varying(50) not null,
    update_time                  character varying(50) not null,
    sub_count                    integer,
    stream_id                    character varying(255),
    has_audio                    bool default false,
    gps_time                     character varying(50),
    stream_identification        character varying(50),
    channel_type                 int  default 0  not null,
    map_level                    int  default 0,
    gb_device_id                 character varying(50),
    gb_name                      character varying(255),
    gb_manufacturer              character varying(255),
    gb_model                     character varying(255),
    gb_owner                     character varying(255),
    gb_civil_code                character varying(255),
    gb_block                     character varying(255),
    gb_address                   character varying(255),
    gb_parental                  integer,
    gb_parent_id                 character varying(255),
    gb_safety_way                integer,
    gb_register_way              integer,
    gb_cert_num                  character varying(50),
    gb_certifiable               integer,
    gb_err_code                  integer,
    gb_end_time                  character varying(50),
    gb_secrecy                   integer,
    gb_ip_address                character varying(50),
    gb_port                      integer,
    gb_password                  character varying(50),
    gb_status                    character varying(50),
    gb_longitude                 double precision,
    gb_latitude                  double precision,
    gb_business_group_id         character varying(50),
    gb_ptz_type                  integer,
    gb_position_type             integer,
    gb_room_type                 integer,
    gb_use_type                  integer,
    gb_supply_light_type         integer,
    gb_direction_type            integer,
    gb_resolution                character varying(255),
    gb_download_speed            character varying(255),
    gb_svc_space_support_mod     integer,
    gb_svc_time_support_mode     integer,
    record_plan_id               integer,
    data_type                    integer not null,
    data_device_id               integer not null,
    gps_speed                    double precision,
    gps_altitude                 double precision,
    gps_direction                double precision,
    enable_broadcast             integer default 0,
    constraint uk_wvp_unique_channel unique (gb_device_id),
    constraint uk_device_channel_source unique (data_device_id, device_id)
);
COMMENT ON TABLE wvp_device_channel IS 'Save channel information and extended attributes under the device';
COMMENT ON COLUMN wvp_device_channel.id IS 'primary keyID';
COMMENT ON COLUMN wvp_device_channel.device_id IS 'Owned equipmentID';
COMMENT ON COLUMN wvp_device_channel.name IS 'Channel name';
COMMENT ON COLUMN wvp_device_channel.manufacturer IS 'Manufacturer';
COMMENT ON COLUMN wvp_device_channel.model IS 'Model';
COMMENT ON COLUMN wvp_device_channel.owner IS 'Belonging unit';
COMMENT ON COLUMN wvp_device_channel.civil_code IS 'Administrative division code';
COMMENT ON COLUMN wvp_device_channel.block IS 'area/Community number';
COMMENT ON COLUMN wvp_device_channel.address IS 'Installation address';
COMMENT ON COLUMN wvp_device_channel.parental IS 'Whether there are child nodes';
COMMENT ON COLUMN wvp_device_channel.parent_id IS 'parent channelID';
COMMENT ON COLUMN wvp_device_channel.safety_way IS 'Security level';
COMMENT ON COLUMN wvp_device_channel.register_way IS 'Registration method';
COMMENT ON COLUMN wvp_device_channel.cert_num IS 'Certificate number';
COMMENT ON COLUMN wvp_device_channel.certifiable IS 'Is it certifiable?';
COMMENT ON COLUMN wvp_device_channel.err_code IS 'Fault status code';
COMMENT ON COLUMN wvp_device_channel.end_time IS 'Service deadline';
COMMENT ON COLUMN wvp_device_channel.secrecy IS 'Confidentiality level';
COMMENT ON COLUMN wvp_device_channel.ip_address IS 'Device IP address';
COMMENT ON COLUMN wvp_device_channel.port IS 'Device port';
COMMENT ON COLUMN wvp_device_channel.password IS 'access password';
COMMENT ON COLUMN wvp_device_channel.status IS 'online status';
COMMENT ON COLUMN wvp_device_channel.longitude IS 'longitude';
COMMENT ON COLUMN wvp_device_channel.latitude IS 'Latitude';
COMMENT ON COLUMN wvp_device_channel.ptz_type IS 'PTZ type';
COMMENT ON COLUMN wvp_device_channel.position_type IS 'Point type';
COMMENT ON COLUMN wvp_device_channel.room_type IS 'room type';
COMMENT ON COLUMN wvp_device_channel.use_type IS 'Nature of use';
COMMENT ON COLUMN wvp_device_channel.supply_light_type IS 'Fill light method';
COMMENT ON COLUMN wvp_device_channel.direction_type IS 'towards';
COMMENT ON COLUMN wvp_device_channel.resolution IS 'resolution';
COMMENT ON COLUMN wvp_device_channel.business_group_id IS 'business groupingID';
COMMENT ON COLUMN wvp_device_channel.download_speed IS 'Download/Stream rate';
COMMENT ON COLUMN wvp_device_channel.svc_space_support_mod IS 'Airspace SVC capabilities';
COMMENT ON COLUMN wvp_device_channel.svc_time_support_mode IS 'Time domain SVC capability';
COMMENT ON COLUMN wvp_device_channel.create_time IS 'creation time';
COMMENT ON COLUMN wvp_device_channel.update_time IS 'Update time';
COMMENT ON COLUMN wvp_device_channel.sub_count IS 'Number of child nodes';
COMMENT ON COLUMN wvp_device_channel.stream_id IS 'bound streamID';
COMMENT ON COLUMN wvp_device_channel.has_audio IS 'Is there audio';
COMMENT ON COLUMN wvp_device_channel.gps_time IS 'GPSPositioning time';
COMMENT ON COLUMN wvp_device_channel.stream_identification IS 'stream identifier';
COMMENT ON COLUMN wvp_device_channel.channel_type IS 'Channel type';
COMMENT ON COLUMN wvp_device_channel.map_level IS 'Map level';
COMMENT ON COLUMN wvp_device_channel.gb_device_id IS 'GBequipment withinID';
COMMENT ON COLUMN wvp_device_channel.gb_name IS 'GBReported name';
COMMENT ON COLUMN wvp_device_channel.gb_manufacturer IS 'GBManufacturer';
COMMENT ON COLUMN wvp_device_channel.gb_model IS 'GBModel';
COMMENT ON COLUMN wvp_device_channel.gb_owner IS 'GBBelong';
COMMENT ON COLUMN wvp_device_channel.gb_civil_code IS 'GBAdministrative division';
COMMENT ON COLUMN wvp_device_channel.gb_block IS 'GBarea';
COMMENT ON COLUMN wvp_device_channel.gb_address IS 'GBaddress';
COMMENT ON COLUMN wvp_device_channel.gb_parental IS 'GBchild node identifier';
COMMENT ON COLUMN wvp_device_channel.gb_parent_id IS 'GBparent channel';
COMMENT ON COLUMN wvp_device_channel.gb_safety_way IS 'GBSecurity precautions';
COMMENT ON COLUMN wvp_device_channel.gb_register_way IS 'GBRegistration method';
COMMENT ON COLUMN wvp_device_channel.gb_cert_num IS 'GBCertificate number';
COMMENT ON COLUMN wvp_device_channel.gb_certifiable IS 'GBCertification mark';
COMMENT ON COLUMN wvp_device_channel.gb_err_code IS 'GBerror code';
COMMENT ON COLUMN wvp_device_channel.gb_end_time IS 'GBDeadline';
COMMENT ON COLUMN wvp_device_channel.gb_secrecy IS 'GBConfidentiality level';
COMMENT ON COLUMN wvp_device_channel.gb_ip_address IS 'GB IP';
COMMENT ON COLUMN wvp_device_channel.gb_port IS 'GBport';
COMMENT ON COLUMN wvp_device_channel.gb_password IS 'GBAccess password';
COMMENT ON COLUMN wvp_device_channel.gb_status IS 'GBStatus';
COMMENT ON COLUMN wvp_device_channel.gb_longitude IS 'GBlongitude';
COMMENT ON COLUMN wvp_device_channel.gb_latitude IS 'GBLatitude';
COMMENT ON COLUMN wvp_device_channel.gb_business_group_id IS 'GBbusiness grouping';
COMMENT ON COLUMN wvp_device_channel.gb_ptz_type IS 'GBPTZ type';
COMMENT ON COLUMN wvp_device_channel.gb_position_type IS 'GBPoint type';
COMMENT ON COLUMN wvp_device_channel.gb_room_type IS 'GBroom type';
COMMENT ON COLUMN wvp_device_channel.gb_use_type IS 'GBPurpose';
COMMENT ON COLUMN wvp_device_channel.gb_supply_light_type IS 'GBfill light';
COMMENT ON COLUMN wvp_device_channel.gb_direction_type IS 'GBtowards';
COMMENT ON COLUMN wvp_device_channel.gb_resolution IS 'GBresolution';
COMMENT ON COLUMN wvp_device_channel.gb_download_speed IS 'GBStream rate';
COMMENT ON COLUMN wvp_device_channel.gb_svc_space_support_mod IS 'GBairspaceSVC';
COMMENT ON COLUMN wvp_device_channel.gb_svc_time_support_mode IS 'GBtime domainSVC';
COMMENT ON COLUMN wvp_device_channel.record_plan_id IS 'Bind recording planID';
COMMENT ON COLUMN wvp_device_channel.data_type IS 'data type identifier';
COMMENT ON COLUMN wvp_device_channel.data_device_id IS 'Data source device primary key';
COMMENT ON COLUMN wvp_device_channel.gps_speed IS 'GPSspeed';
COMMENT ON COLUMN wvp_device_channel.gps_altitude IS 'GPSaltitude';
COMMENT ON COLUMN wvp_device_channel.gps_direction IS 'GPSdirection';
COMMENT ON COLUMN wvp_device_channel.enable_broadcast IS 'Whether to support broadcast';


CREATE INDEX idx_data_type ON wvp_device_channel (data_type);
CREATE INDEX idx_data_device_id ON wvp_device_channel (data_device_id);

drop table IF EXISTS wvp_media_server;
create table IF NOT EXISTS wvp_media_server
(
    id                  character varying(255) primary key,
    ip                  character varying(50),
    hook_ip             character varying(50),
    sdp_ip              character varying(50),
    stream_ip           character varying(50),
    http_port           integer,
    http_ssl_port       integer,
    rtmp_port           integer,
    rtmp_ssl_port       integer,
    rtp_proxy_port      integer,
    rtsp_port           integer,
    rtsp_ssl_port       integer,
    flv_port            integer,
    flv_ssl_port        integer,
    mp4_port            integer,
    mp4_ssl_port        integer,
    ws_flv_port         integer,
    ws_flv_ssl_port     integer,
    jtt_proxy_port      integer,
    auto_config         bool                  default false,
    secret              character varying(50),
    type                character varying(50) default 'zlm',
    rtp_enable          bool                  default false,
    rtp_port_range      character varying(50),
    send_rtp_port_range character varying(50),
    record_assist_port  integer,
    default_server      bool                  default false,
    create_time         character varying(50),
    update_time         character varying(50),
    hook_alive_interval integer,
    record_path         character varying(255),
    record_day          integer               default 7,
    transcode_suffix    character varying(255),
    server_id           character varying(50)
);
COMMENT ON TABLE wvp_media_server IS 'Media server (such as ZLM) node information';
COMMENT ON COLUMN wvp_media_server.id IS 'media serverID';
COMMENT ON COLUMN wvp_media_server.ip IS 'serverIP';
COMMENT ON COLUMN wvp_media_server.hook_ip IS 'hookcallbackIP';
COMMENT ON COLUMN wvp_media_server.sdp_ip IS 'SDPused inIP';
COMMENT ON COLUMN wvp_media_server.stream_ip IS 'Used for push streamingIP';
COMMENT ON COLUMN wvp_media_server.http_port IS 'HTTPport';
COMMENT ON COLUMN wvp_media_server.http_ssl_port IS 'HTTPSport';
COMMENT ON COLUMN wvp_media_server.rtmp_port IS 'RTMPport';
COMMENT ON COLUMN wvp_media_server.rtmp_ssl_port IS 'RTMPSport';
COMMENT ON COLUMN wvp_media_server.rtp_proxy_port IS 'RTPproxy port';
COMMENT ON COLUMN wvp_media_server.rtsp_port IS 'RTSPport';
COMMENT ON COLUMN wvp_media_server.rtsp_ssl_port IS 'RTSPSport';
COMMENT ON COLUMN wvp_media_server.flv_port IS 'FLVport';
COMMENT ON COLUMN wvp_media_server.flv_ssl_port IS 'FLV HTTPSport';
COMMENT ON COLUMN wvp_media_server.mp4_port IS 'MP4On-demand port';
COMMENT ON COLUMN wvp_media_server.mp4_ssl_port IS 'MP4 HTTPSport';
COMMENT ON COLUMN wvp_media_server.ws_flv_port IS 'WS-FLVport';
COMMENT ON COLUMN wvp_media_server.ws_flv_ssl_port IS 'WS-FLV HTTPSport';
COMMENT ON COLUMN wvp_media_server.jtt_proxy_port IS 'JT/Tproxy port';
COMMENT ON COLUMN wvp_media_server.auto_config IS 'Whether to automatically configure';
COMMENT ON COLUMN wvp_media_server.secret IS 'ZLMVerification key';
COMMENT ON COLUMN wvp_media_server.type IS 'Node type';
COMMENT ON COLUMN wvp_media_server.rtp_enable IS 'Whether to turn onRTP';
COMMENT ON COLUMN wvp_media_server.rtp_port_range IS 'RTPport range';
COMMENT ON COLUMN wvp_media_server.send_rtp_port_range IS 'Send RTP port range';
COMMENT ON COLUMN wvp_media_server.record_assist_port IS 'Video auxiliary port';
COMMENT ON COLUMN wvp_media_server.default_server IS 'Whether the default node';
COMMENT ON COLUMN wvp_media_server.create_time IS 'creation time';
COMMENT ON COLUMN wvp_media_server.update_time IS 'Update time';
COMMENT ON COLUMN wvp_media_server.hook_alive_interval IS 'hookheartbeat interval';
COMMENT ON COLUMN wvp_media_server.record_path IS 'Video directory';
COMMENT ON COLUMN wvp_media_server.record_day IS 'Video retention days';
COMMENT ON COLUMN wvp_media_server.transcode_suffix IS 'Transcoding command suffix';
COMMENT ON COLUMN wvp_media_server.server_id IS 'Corresponding signaling serverID';


drop table IF EXISTS wvp_platform;
create table IF NOT EXISTS wvp_platform
(
    id                    serial primary key,
    enable                bool default false,
    name                  character varying(255),
    server_gb_id          character varying(50),
    server_gb_domain      character varying(50),
    server_ip             character varying(50),
    server_port           integer,
    device_gb_id          character varying(50),
    device_ip             character varying(50),
    device_port           character varying(50),
    username              character varying(255),
    password              character varying(50),
    expires               character varying(50),
    keep_timeout          character varying(50),
    transport             character varying(50),
    civil_code            character varying(50),
    manufacturer          character varying(255),
    model                 character varying(255),
    address               character varying(255),
    character_set         character varying(50),
    ptz                   bool default false,
    rtcp                  bool default false,
    status                bool default false,
    catalog_group         integer,
    register_way          integer,
    secrecy               integer,
    create_time           character varying(50),
    update_time           character varying(50),
    as_message_channel    bool default false,
    catalog_with_platform integer default 1,
    catalog_with_group    integer default 1,
    catalog_with_region   integer default 1,
    auto_push_channel     bool default true,
    send_stream_ip        character varying(50),
    server_id             character varying(50),
    constraint uk_platform_unique_server_gb_id unique (server_gb_id)
);
COMMENT ON TABLE wvp_platform IS 'Registration information of superior national standard platform';
COMMENT ON COLUMN wvp_platform.id IS 'primary keyID';
COMMENT ON COLUMN wvp_platform.enable IS 'Whether to enable registration for this platform';
COMMENT ON COLUMN wvp_platform.name IS 'Platform name';
COMMENT ON COLUMN wvp_platform.server_gb_id IS 'National standard code of superior platform';
COMMENT ON COLUMN wvp_platform.server_gb_domain IS 'Upper level platform domain encoding';
COMMENT ON COLUMN wvp_platform.server_ip IS 'Superior platformIP';
COMMENT ON COLUMN wvp_platform.server_port IS 'Upper level platform registration port';
COMMENT ON COLUMN wvp_platform.device_gb_id IS 'The national standard code registered on this platform';
COMMENT ON COLUMN wvp_platform.device_ip IS 'Signaling on this platformIP';
COMMENT ON COLUMN wvp_platform.device_port IS 'Signaling port of this platform';
COMMENT ON COLUMN wvp_platform.username IS 'Register username';
COMMENT ON COLUMN wvp_platform.password IS 'Registration password';
COMMENT ON COLUMN wvp_platform.expires IS 'Registration validity period';
COMMENT ON COLUMN wvp_platform.keep_timeout IS 'Heartbeat timeout';
COMMENT ON COLUMN wvp_platform.transport IS 'transport protocol（UDP/TCP）';
COMMENT ON COLUMN wvp_platform.civil_code IS 'Administrative division code';
COMMENT ON COLUMN wvp_platform.manufacturer IS 'Manufacturer';
COMMENT ON COLUMN wvp_platform.model IS 'Model';
COMMENT ON COLUMN wvp_platform.address IS 'address';
COMMENT ON COLUMN wvp_platform.character_set IS 'character set';
COMMENT ON COLUMN wvp_platform.ptz IS 'Whether to supportPTZ';
COMMENT ON COLUMN wvp_platform.rtcp IS 'Whether to turn onRTCP';
COMMENT ON COLUMN wvp_platform.status IS 'Registration status';
COMMENT ON COLUMN wvp_platform.catalog_group IS 'Directory grouping method';
COMMENT ON COLUMN wvp_platform.register_way IS 'Registration method';
COMMENT ON COLUMN wvp_platform.secrecy IS 'Confidentiality level';
COMMENT ON COLUMN wvp_platform.create_time IS 'creation time';
COMMENT ON COLUMN wvp_platform.update_time IS 'Update time';
COMMENT ON COLUMN wvp_platform.as_message_channel IS 'Whether to serve as a message channel';
COMMENT ON COLUMN wvp_platform.catalog_with_platform IS 'Whether to push the platform directory';
COMMENT ON COLUMN wvp_platform.catalog_with_group IS 'Whether to push the group directory';
COMMENT ON COLUMN wvp_platform.catalog_with_region IS 'Whether to push the regional directory';
COMMENT ON COLUMN wvp_platform.auto_push_channel IS 'Whether to automatically push channels';
COMMENT ON COLUMN wvp_platform.send_stream_ip IS 'Used when pushingIP';
COMMENT ON COLUMN wvp_platform.server_id IS 'Corresponding signaling serverID';


drop table IF EXISTS wvp_platform_channel;
create table IF NOT EXISTS wvp_platform_channel
(
    id                           serial primary key,
    platform_id                  integer,
    device_channel_id            integer,
    custom_device_id             character varying(50),
    custom_name                  character varying(255),
    custom_manufacturer          character varying(50),
    custom_model                 character varying(50),
    custom_owner                 character varying(50),
    custom_civil_code            character varying(50),
    custom_block                 character varying(50),
    custom_address               character varying(50),
    custom_parental              integer,
    custom_parent_id             character varying(50),
    custom_safety_way            integer,
    custom_register_way          integer,
    custom_cert_num              character varying(50),
    custom_certifiable           integer,
    custom_err_code              integer,
    custom_end_time              character varying(50),
    custom_secrecy               integer,
    custom_ip_address            character varying(50),
    custom_port                  integer,
    custom_password              character varying(255),
    custom_status                character varying(50),
    custom_longitude             double precision,
    custom_latitude              double precision,
    custom_ptz_type              integer,
    custom_position_type         integer,
    custom_room_type             integer,
    custom_use_type              integer,
    custom_supply_light_type     integer,
    custom_direction_type        integer,
    custom_resolution            character varying(255),
    custom_business_group_id     character varying(255),
    custom_download_speed        character varying(255),
    custom_svc_space_support_mod integer,
    custom_svc_time_support_mode integer,
    constraint uk_platform_gb_channel_platform_id_catalog_id_device_channel_id unique (platform_id, device_channel_id),
    constraint uk_platform_gb_channel_device_id unique (custom_device_id)
);
COMMENT ON TABLE wvp_platform_channel IS 'Channel mapping relationship issued by the national standard platform';
COMMENT ON COLUMN wvp_platform_channel.id IS 'primary keyID';
COMMENT ON COLUMN wvp_platform_channel.platform_id IS 'platformID';
COMMENT ON COLUMN wvp_platform_channel.device_channel_id IS 'Local channel table primary key';
COMMENT ON COLUMN wvp_platform_channel.custom_device_id IS 'Customized national standard coding';
COMMENT ON COLUMN wvp_platform_channel.custom_name IS 'custom name';
COMMENT ON COLUMN wvp_platform_channel.custom_manufacturer IS 'Custom manufacturer';
COMMENT ON COLUMN wvp_platform_channel.custom_model IS 'Custom model';
COMMENT ON COLUMN wvp_platform_channel.custom_owner IS 'Custom attribution';
COMMENT ON COLUMN wvp_platform_channel.custom_civil_code IS 'Custom administrative divisions';
COMMENT ON COLUMN wvp_platform_channel.custom_block IS 'Custom area';
COMMENT ON COLUMN wvp_platform_channel.custom_address IS 'Custom address';
COMMENT ON COLUMN wvp_platform_channel.custom_parental IS 'Custom parent/sub-id';
COMMENT ON COLUMN wvp_platform_channel.custom_parent_id IS 'Custom parent node';
COMMENT ON COLUMN wvp_platform_channel.custom_safety_way IS 'Custom security protection';
COMMENT ON COLUMN wvp_platform_channel.custom_register_way IS 'Custom registration method';
COMMENT ON COLUMN wvp_platform_channel.custom_cert_num IS 'Custom certificate number';
COMMENT ON COLUMN wvp_platform_channel.custom_certifiable IS 'Custom certifiable mark';
COMMENT ON COLUMN wvp_platform_channel.custom_err_code IS 'Custom error code';
COMMENT ON COLUMN wvp_platform_channel.custom_end_time IS 'Custom deadline';
COMMENT ON COLUMN wvp_platform_channel.custom_secrecy IS 'Customized confidentiality level';
COMMENT ON COLUMN wvp_platform_channel.custom_ip_address IS 'CustomizeIP';
COMMENT ON COLUMN wvp_platform_channel.custom_port IS 'Custom port';
COMMENT ON COLUMN wvp_platform_channel.custom_password IS 'Custom password';
COMMENT ON COLUMN wvp_platform_channel.custom_status IS 'Custom status';
COMMENT ON COLUMN wvp_platform_channel.custom_longitude IS 'Custom longitude';
COMMENT ON COLUMN wvp_platform_channel.custom_latitude IS 'Custom latitude';
COMMENT ON COLUMN wvp_platform_channel.custom_ptz_type IS 'Custom PTZ type';
COMMENT ON COLUMN wvp_platform_channel.custom_position_type IS 'Custom point type';
COMMENT ON COLUMN wvp_platform_channel.custom_room_type IS 'Custom room type';
COMMENT ON COLUMN wvp_platform_channel.custom_use_type IS 'Custom use';
COMMENT ON COLUMN wvp_platform_channel.custom_supply_light_type IS 'Custom fill light';
COMMENT ON COLUMN wvp_platform_channel.custom_direction_type IS 'Custom orientation';
COMMENT ON COLUMN wvp_platform_channel.custom_resolution IS 'Custom resolution';
COMMENT ON COLUMN wvp_platform_channel.custom_business_group_id IS 'Custom business grouping';
COMMENT ON COLUMN wvp_platform_channel.custom_download_speed IS 'Custom stream rate';
COMMENT ON COLUMN wvp_platform_channel.custom_svc_space_support_mod IS 'Custom airspaceSVC';
COMMENT ON COLUMN wvp_platform_channel.custom_svc_time_support_mode IS 'Custom time domainSVC';


drop table IF EXISTS wvp_platform_group;
create table IF NOT EXISTS wvp_platform_group
(
    id          serial primary key,
    platform_id integer,
    group_id    integer,
    constraint uk_wvp_platform_group_platform_id_group_id unique (platform_id, group_id)
);
COMMENT ON TABLE wvp_platform_group IS 'Platforms and Groups (Administrative Divisions/organization) relationship';
COMMENT ON COLUMN wvp_platform_group.id IS 'primary keyID';
COMMENT ON COLUMN wvp_platform_group.platform_id IS 'platformID';
COMMENT ON COLUMN wvp_platform_group.group_id IS 'GroupID';


drop table IF EXISTS wvp_platform_region;
create table IF NOT EXISTS wvp_platform_region
(
    id          serial primary key,
    platform_id integer,
    region_id   integer,
    constraint uk_wvp_platform_region_platform_id_group_id unique (platform_id, region_id)
);
COMMENT ON TABLE wvp_platform_region IS 'Platform and regional relations';
COMMENT ON COLUMN wvp_platform_region.id IS 'primary keyID';
COMMENT ON COLUMN wvp_platform_region.platform_id IS 'platformID';
COMMENT ON COLUMN wvp_platform_region.region_id IS 'areaID';


drop table IF EXISTS wvp_stream_proxy;
create table IF NOT EXISTS wvp_stream_proxy
(
    id                         serial primary key,
    type                       character varying(50),
    app                        character varying(255),
    stream                     character varying(255),
    src_url                    character varying(255),
    timeout                    integer,
    ffmpeg_cmd_key             character varying(255),
    rtsp_type                  character varying(50),
    media_server_id            character varying(50),
    enable_audio               bool default false,
    enable_mp4                 bool default false,
    pulling                    bool default false,
    enable                     bool default false,
    create_time                character varying(50),
    name                       character varying(255),
    update_time                character varying(50),
    stream_key                 character varying(255),
    server_id                  character varying(50),
    enable_disable_none_reader bool default false,
    relates_media_server_id    character varying(50),
    constraint uk_stream_proxy_app_stream unique (app, stream)
);
COMMENT ON TABLE wvp_stream_proxy IS 'Streaming agent/Retweet configuration';
COMMENT ON COLUMN wvp_stream_proxy.id IS 'primary keyID';
COMMENT ON COLUMN wvp_stream_proxy.type IS 'Agent type (pull flow/Push streaming）';
COMMENT ON COLUMN wvp_stream_proxy.app IS 'Application name';
COMMENT ON COLUMN wvp_stream_proxy.stream IS 'flowID';
COMMENT ON COLUMN wvp_stream_proxy.src_url IS 'Source address';
COMMENT ON COLUMN wvp_stream_proxy.timeout IS 'Pull timeout';
COMMENT ON COLUMN wvp_stream_proxy.ffmpeg_cmd_key IS 'FFmpegcommand template key';
COMMENT ON COLUMN wvp_stream_proxy.rtsp_type IS 'RTSPPull method';
COMMENT ON COLUMN wvp_stream_proxy.media_server_id IS 'Specify media serverID';
COMMENT ON COLUMN wvp_stream_proxy.enable_audio IS 'Whether to enable audio';
COMMENT ON COLUMN wvp_stream_proxy.enable_mp4 IS 'Record or notMP4';
COMMENT ON COLUMN wvp_stream_proxy.pulling IS 'Is the stream currently being pulled?';
COMMENT ON COLUMN wvp_stream_proxy.enable IS 'Whether to enable the proxy';
COMMENT ON COLUMN wvp_stream_proxy.create_time IS 'creation time';
COMMENT ON COLUMN wvp_stream_proxy.name IS 'Agent name';
COMMENT ON COLUMN wvp_stream_proxy.update_time IS 'Update time';
COMMENT ON COLUMN wvp_stream_proxy.stream_key IS 'Unique stream identifier';
COMMENT ON COLUMN wvp_stream_proxy.server_id IS 'signaling serverID';
COMMENT ON COLUMN wvp_stream_proxy.enable_disable_none_reader IS 'Whether to automatically stop streaming when no one is watching';
COMMENT ON COLUMN wvp_stream_proxy.relates_media_server_id IS 'Associated media serverID';


drop table IF EXISTS wvp_stream_push;
create table IF NOT EXISTS wvp_stream_push
(
    id                 serial primary key,
    app                character varying(255),
    stream             character varying(255),
    create_time        character varying(50),
    media_server_id    character varying(50),
    server_id          character varying(50),
    push_time          character varying(50),
    status             bool default false,
    update_time        character varying(50),
    pushing            bool default false,
    self               bool default false,
    start_offline_push bool default true,
    constraint uk_stream_push_app_stream unique (app, stream)
);
COMMENT ON TABLE wvp_stream_push IS 'Push session record';
COMMENT ON COLUMN wvp_stream_push.id IS 'primary keyID';
COMMENT ON COLUMN wvp_stream_push.app IS 'Application name';
COMMENT ON COLUMN wvp_stream_push.stream IS 'flowID';
COMMENT ON COLUMN wvp_stream_push.create_time IS 'creation time';
COMMENT ON COLUMN wvp_stream_push.media_server_id IS 'The media server where the stream is located';
COMMENT ON COLUMN wvp_stream_push.server_id IS 'signaling serverID';
COMMENT ON COLUMN wvp_stream_push.push_time IS 'Push start time';
COMMENT ON COLUMN wvp_stream_push.status IS 'Push status';
COMMENT ON COLUMN wvp_stream_push.update_time IS 'Update time';
COMMENT ON COLUMN wvp_stream_push.pushing IS 'Whether streaming is being pushed';
COMMENT ON COLUMN wvp_stream_push.self IS 'Whether to initiate locally';
COMMENT ON COLUMN wvp_stream_push.start_offline_push IS 'Whether to automatically re-push after offline';


drop table IF EXISTS wvp_cloud_record;
create table IF NOT EXISTS wvp_cloud_record
(
    id              serial primary key,
    app             character varying(255),
    stream          character varying(255),
    call_id         character varying(255),
    start_time      int8,
    end_time        int8,
    media_server_id character varying(50),
    server_id       character varying(50),
    file_name       character varying(255),
    folder          character varying(500),
    file_path       character varying(500),
    collect         bool default false,
    file_size       int8,
    time_len        double precision
);
COMMENT ON TABLE wvp_cloud_record IS 'Cloud video recording';
COMMENT ON COLUMN wvp_cloud_record.id IS 'primary keyID';
COMMENT ON COLUMN wvp_cloud_record.app IS 'Application name';
COMMENT ON COLUMN wvp_cloud_record.stream IS 'flowID';
COMMENT ON COLUMN wvp_cloud_record.call_id IS 'sessionID';
COMMENT ON COLUMN wvp_cloud_record.start_time IS 'Recording start time';
COMMENT ON COLUMN wvp_cloud_record.end_time IS 'Recording end time';
COMMENT ON COLUMN wvp_cloud_record.media_server_id IS 'media serverID';
COMMENT ON COLUMN wvp_cloud_record.server_id IS 'signaling serverID';
COMMENT ON COLUMN wvp_cloud_record.file_name IS 'file name';
COMMENT ON COLUMN wvp_cloud_record.folder IS 'Directory';
COMMENT ON COLUMN wvp_cloud_record.file_path IS 'full path';
COMMENT ON COLUMN wvp_cloud_record.collect IS 'Whether to collect';
COMMENT ON COLUMN wvp_cloud_record.file_size IS 'file size';
COMMENT ON COLUMN wvp_cloud_record.time_len IS 'duration';


drop table IF EXISTS wvp_user;
create table IF NOT EXISTS wvp_user
(
    id          serial primary key,
    username    character varying(255),
    password    character varying(255),
    role_id     integer,
    create_time character varying(50),
    update_time character varying(50),
    push_key    character varying(50),
    constraint uk_user_username unique (username)
);
COMMENT ON TABLE wvp_user IS 'Platform user information';
COMMENT ON COLUMN wvp_user.id IS 'primary keyID';
COMMENT ON COLUMN wvp_user.username IS 'Username';
COMMENT ON COLUMN wvp_user.password IS 'Password（MD5）';
COMMENT ON COLUMN wvp_user.role_id IS 'roleID';
COMMENT ON COLUMN wvp_user.create_time IS 'creation time';
COMMENT ON COLUMN wvp_user.update_time IS 'Update time';
COMMENT ON COLUMN wvp_user.push_key IS 'push key';


drop table IF EXISTS wvp_user_role;
create table IF NOT EXISTS wvp_user_role
(
    id          serial primary key,
    name        character varying(50),
    authority   character varying(50),
    create_time character varying(50),
    update_time character varying(50)
);
COMMENT ON TABLE wvp_user_role IS 'User role information';
COMMENT ON COLUMN wvp_user_role.id IS 'primary keyID';
COMMENT ON COLUMN wvp_user_role.name IS 'Character name';
COMMENT ON COLUMN wvp_user_role.authority IS 'Permission ID';
COMMENT ON COLUMN wvp_user_role.create_time IS 'creation time';
COMMENT ON COLUMN wvp_user_role.update_time IS 'Update time';



drop table IF EXISTS wvp_user_api_key;
create table IF NOT EXISTS wvp_user_api_key
(
    id          serial primary key,
    user_id     int8,
    app         character varying(255),
    api_key     text,
    expired_at  int8,
    remark      character varying(255),
    enable      bool default true,
    create_time character varying(50),
    update_time character varying(50)
);
COMMENT ON COLUMN wvp_user_api_key.id IS 'primary keyID';
COMMENT ON COLUMN wvp_user_api_key.user_id IS 'Associated usersID';
COMMENT ON COLUMN wvp_user_api_key.app IS 'Application ID';
COMMENT ON COLUMN wvp_user_api_key.api_key IS 'API Key';
COMMENT ON COLUMN wvp_user_api_key.expired_at IS 'Expiration timestamp';
COMMENT ON COLUMN wvp_user_api_key.remark IS 'Remarks';
COMMENT ON COLUMN wvp_user_api_key.enable IS 'Whether to enable';
COMMENT ON COLUMN wvp_user_api_key.create_time IS 'creation time';
COMMENT ON COLUMN wvp_user_api_key.update_time IS 'Update time';



/*initial data*/
INSERT INTO wvp_user
VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 1, '2021-04-13 14:14:57', '2021-04-13 14:14:57',
        '3e80d1762a324d5b0ff636e0bd16f1e3');
INSERT INTO wvp_user_role
VALUES (1, 'admin', '0', '2021-04-13 14:14:57', '2021-04-13 14:14:57');

drop table IF EXISTS wvp_common_group;
create table IF NOT EXISTS wvp_common_group
(
    id               serial primary key,
    device_id        varchar(50)  NOT NULL,
    name             varchar(255) NOT NULL,
    parent_id        int,
    parent_device_id varchar(50) DEFAULT NULL,
    business_group   varchar(50)  NOT NULL,
    create_time      varchar(50)  NOT NULL,
    update_time      varchar(50)  NOT NULL,
    civil_code       varchar(50) default null,
    alias            varchar(255) default null,
    constraint uk_common_group_device_platform unique (device_id)
);
COMMENT ON TABLE wvp_common_group IS 'General grouping table to store industry or organizational structure';
COMMENT ON COLUMN wvp_common_group.id IS 'primary keyID';
COMMENT ON COLUMN wvp_common_group.device_id IS 'The platform or device corresponding to the groupID';
COMMENT ON COLUMN wvp_common_group.name IS 'Group name';
COMMENT ON COLUMN wvp_common_group.parent_id IS 'Parent groupingID';
COMMENT ON COLUMN wvp_common_group.parent_device_id IS 'The device corresponding to the parent groupID';
COMMENT ON COLUMN wvp_common_group.business_group IS 'Business group coding';
COMMENT ON COLUMN wvp_common_group.create_time IS 'creation time';
COMMENT ON COLUMN wvp_common_group.update_time IS 'Update time';
COMMENT ON COLUMN wvp_common_group.civil_code IS 'Administrative division code';
COMMENT ON COLUMN wvp_common_group.alias IS 'Alias';


drop table IF EXISTS wvp_common_region;
create table IF NOT EXISTS wvp_common_region
(
    id               serial primary key,
    device_id        varchar(50)  NOT NULL,
    name             varchar(255) NOT NULL,
    parent_id        int,
    parent_device_id varchar(50) DEFAULT NULL,
    create_time      varchar(50)  NOT NULL,
    update_time      varchar(50)  NOT NULL,
    constraint uk_common_region_device_id unique (device_id)
);
COMMENT ON TABLE wvp_common_region IS 'General administrative area table';
COMMENT ON COLUMN wvp_common_region.id IS 'primary keyID';
COMMENT ON COLUMN wvp_common_region.device_id IS 'Platform or device corresponding to the regionID';
COMMENT ON COLUMN wvp_common_region.name IS 'area name';
COMMENT ON COLUMN wvp_common_region.parent_id IS 'parent regionID';
COMMENT ON COLUMN wvp_common_region.parent_device_id IS 'Devices in the parent regionID';
COMMENT ON COLUMN wvp_common_region.create_time IS 'creation time';
COMMENT ON COLUMN wvp_common_region.update_time IS 'Update time';


drop table IF EXISTS wvp_record_plan;
create table IF NOT EXISTS wvp_record_plan
(
    id              serial primary key,
    snap            bool default false,
    name            varchar(255) NOT NULL,
    create_time     character varying(50),
    update_time     character varying(50)
);
COMMENT ON TABLE wvp_record_plan IS 'Basic information of video recording plan';
COMMENT ON COLUMN wvp_record_plan.id IS 'primary keyID';
COMMENT ON COLUMN wvp_record_plan.snap IS 'Whether to capture the plan';
COMMENT ON COLUMN wvp_record_plan.name IS 'Plan name';
COMMENT ON COLUMN wvp_record_plan.create_time IS 'creation time';
COMMENT ON COLUMN wvp_record_plan.update_time IS 'Update time';


drop table IF EXISTS wvp_record_plan_item;
create table IF NOT EXISTS wvp_record_plan_item
(
    id              serial primary key,
    "start"           int,
    stop            int,
    week_day        int,
    plan_id        int,
    create_time     character varying(50),
    update_time     character varying(50)
);
COMMENT ON TABLE wvp_record_plan_item IS 'Recording schedule entry form';
COMMENT ON COLUMN wvp_record_plan_item.id IS 'primary keyID';
COMMENT ON COLUMN wvp_record_plan_item."start" IS 'Start time (minutes）';
COMMENT ON COLUMN wvp_record_plan_item.stop IS 'end time (minutes）';
COMMENT ON COLUMN wvp_record_plan_item.week_day IS 'week（0-6）';
COMMENT ON COLUMN wvp_record_plan_item.plan_id IS 'Belonging video projectID';
COMMENT ON COLUMN wvp_record_plan_item.create_time IS 'creation time';
COMMENT ON COLUMN wvp_record_plan_item.update_time IS 'Update time';


drop table IF EXISTS wvp_jt_terminal;
create table IF NOT EXISTS wvp_jt_terminal (
                                 id serial primary key,
                                 phone_number character varying(50),
                                 terminal_id character varying(50),
                                 province_id character varying(50),
                                 province_text character varying(100),
                                 city_id character varying(50),
                                 city_text character varying(100),
                                 maker_id character varying(50),
                                 model character varying(50),
                                 plate_color character varying(50),
                                 plate_no character varying(50),
                                 longitude double precision,
                                 latitude double precision,
                                 status bool default false,
                                 register_time character varying(50) default null,
                                 update_time character varying(50) not null,
                                 create_time character varying(50) not null,
                                 geo_coord_sys character varying(50),
                                 media_server_id character varying(50) default 'auto',
                                 sdp_ip character varying(50),
                                 constraint uk_jt_device_id_device_id unique (id, phone_number)
);
COMMENT ON TABLE wvp_jt_terminal IS 'Ministry of Transport JT/T 1076 Terminal information';
COMMENT ON COLUMN wvp_jt_terminal.id IS 'primary keyID';
COMMENT ON COLUMN wvp_jt_terminal.phone_number IS 'Terminal SIM card number';
COMMENT ON COLUMN wvp_jt_terminal.terminal_id IS 'terminal equipmentID';
COMMENT ON COLUMN wvp_jt_terminal.province_id IS 'ProvinceID';
COMMENT ON COLUMN wvp_jt_terminal.province_text IS 'Province name';
COMMENT ON COLUMN wvp_jt_terminal.city_id IS 'CityID';
COMMENT ON COLUMN wvp_jt_terminal.city_text IS 'City name';
COMMENT ON COLUMN wvp_jt_terminal.maker_id IS 'ManufacturerID';
COMMENT ON COLUMN wvp_jt_terminal.model IS 'Terminal model';
COMMENT ON COLUMN wvp_jt_terminal.plate_color IS 'license plate color';
COMMENT ON COLUMN wvp_jt_terminal.plate_no IS 'license plate number';
COMMENT ON COLUMN wvp_jt_terminal.longitude IS 'longitude';
COMMENT ON COLUMN wvp_jt_terminal.latitude IS 'Latitude';
COMMENT ON COLUMN wvp_jt_terminal.status IS 'online status';
COMMENT ON COLUMN wvp_jt_terminal.register_time IS 'Registration time';
COMMENT ON COLUMN wvp_jt_terminal.update_time IS 'Update time';
COMMENT ON COLUMN wvp_jt_terminal.create_time IS 'creation time';
COMMENT ON COLUMN wvp_jt_terminal.geo_coord_sys IS 'coordinate system';
COMMENT ON COLUMN wvp_jt_terminal.media_server_id IS 'media serverID';
COMMENT ON COLUMN wvp_jt_terminal.sdp_ip IS 'SDP IP';

drop table IF EXISTS wvp_jt_channel;
create table IF NOT EXISTS wvp_jt_channel (
                                id serial primary key,
                                terminal_db_id integer,
                                channel_id integer,
                                has_audio bool default false,
                                name character varying(255),
                                update_time character varying(50) not null,
                                create_time character varying(50) not null,
                                constraint uk_jt_channel_id_device_id unique (terminal_db_id, channel_id)
);
COMMENT ON TABLE wvp_jt_channel IS 'Ministry of Transport JT/T 1076 Channel information';
COMMENT ON COLUMN wvp_jt_channel.id IS 'primary keyID';
COMMENT ON COLUMN wvp_jt_channel.terminal_db_id IS 'Belonging terminal recordID';
COMMENT ON COLUMN wvp_jt_channel.channel_id IS 'Channel number';
COMMENT ON COLUMN wvp_jt_channel.has_audio IS 'Is there audio';
COMMENT ON COLUMN wvp_jt_channel.name IS 'Channel name';
COMMENT ON COLUMN wvp_jt_channel.update_time IS 'Update time';
COMMENT ON COLUMN wvp_jt_channel.create_time IS 'creation time';


drop table IF EXISTS wvp_alarm;
create table IF NOT EXISTS wvp_alarm (
        id serial primary key,
        channel_id integer,
        description character varying(255),
        snap_path character varying(255),
        record_path character varying(255),
        longitude double precision,
        latitude double precision,
        alarm_type integer,
        alarm_time bigint
);
COMMENT ON COLUMN wvp_alarm.id IS 'primary keyID';
COMMENT ON COLUMN wvp_alarm.channel_id IS 'Database of associated channelsid';
COMMENT ON COLUMN wvp_alarm.description IS 'Alarm description';
COMMENT ON COLUMN wvp_alarm.snap_path IS 'Alarm snapshot path';
COMMENT ON COLUMN wvp_alarm.record_path IS 'Alarm recording path';
COMMENT ON COLUMN wvp_alarm.longitude IS 'The longitude attached to the alarm';
COMMENT ON COLUMN wvp_alarm.latitude IS 'Latitude attached to alarm';
COMMENT ON COLUMN wvp_alarm.alarm_type IS 'Alarm category';
COMMENT ON COLUMN wvp_alarm.alarm_time IS 'Alarm time';


