<!-- 7*24 uninterrupted recording -->

# 7*24 uninterrupted video recording

At present, if you want to achieve uninterrupted recording, it is not enough to just turn off no one to watch and stop streaming. The device may experience network disconnection and restart, which will cause the interruption of recording. We currently provide you with an available temporary solution.

**Principle:** wvp supports automatic on-demand playback using the stream address, that is, you get a stream address and play it directly. Even if the device is not on-demand, wvp will automatically help you on-demand playback; ZLM
The streaming agent will retry infinitely after success. It can be pulled up as soon as the streaming is restored, based on these two principles.
**The plan is as follows:**

1. In the wvp configuration, user-settings->auto-apply-play is set to true to enable automatic on-demand play;
2. Click the channel you want to record, click "More Addresses" in the lower left corner of the playback page, click rtsp, and copy the rtsp address to the clipboard;
3. Add a stream to the streaming agent, fill in the address you copied, and enable it successfully.
**premise:**
1. WVP uses multiple ports to collect streams, otherwise you cannot get a fixed stream address and cannot achieve automatic on-demand.

