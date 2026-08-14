<!-- Cloud recording -->

#cloudrecording

! [Cloud recording](_media/img_26.png) 
Cloud recording is the management of video files recorded under the zlm service. The file path of the video is by default under ZLM/www/record.

- Whether the national standard equipment records: You can set user-settings.record-sip to true in the WVP configuration, then every on-demand and video playback will be recorded;
- Whether the push device records: You can set user-settings.record-push-live to true in the WVP configuration;
- Whether the streaming agent records: You can specify it when adding and editing the streaming agent. Recording will be performed every time the video is requested.
- Recording file storage path configuration: You can modify media.record-path to modify the recording path, but if there are old recording files, please do not migrate because the database records the absolute path of each recording. Once modified, the file will be found and cannot be removed and played regularly.
- Video storage time: You can modify media.record-day to modify the video storage time, the unit is days;

