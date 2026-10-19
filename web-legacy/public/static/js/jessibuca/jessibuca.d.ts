declare namespace Jessibuca {

    /** Timeout information */
    enum TIMEOUT {
        /** Whenplay()time, if there is no data returned */
        loadingTimeout = 'loadingTimeout',
        /** During playback, if no data is rendered after timeout is exceeded, */
        delayTimeout = 'delayTimeout',
    }

    /** error message */
    enum ERROR {
        /** Playback error, when the url is empty, call the play method */
        playError = 'playError',
        /** http Request failed */
        fetchError = 'fetchError',
        /** websocket Request failed */
        websocketError = 'websocketError',
        /** webcodecs Decoding h265 failed */
        webcodecsH265NotSupport = 'webcodecsH265NotSupport',
        /** mediaSource Decoding h265 failed */
        mediaSourceH265NotSupport = 'mediaSourceH265NotSupport',
        /** wasm Decoding failed */
        wasmDecodeError = 'wasmDecodeError',
    }

    interface Config {
        /**
         * player container
         * *  If it is string , the underlying call is document.getElementById('id')
         * */
        container: HTMLElement | string;
        /**
         * Set the maximum buffering time in seconds. The player will automatically eliminate the delay.
         */
        videoBuffer?: number;
        /**
         * workeraddress
         * *  The default reference is the decoder.js file under the root directory. The decoder.js and decoder.wasm files must be placed in the same directory.。 */
        decoder?: string;
        /**
         * Whether to use off-screen mode (to improve rendering capabilities)）
         */
        forceNoOffscreen?: boolean;
        /**
         * Whether to enable the current page'visibilityState'become'hidden'automatically pauses playback when。
         */
        hiddenAutoPause?: boolean;
        /**
         * Whether there is audio. If set to `false`, the audio data will not be decoded to improve performance.。
         */
        hasAudio?: boolean;
        /**
         * Set the rotation angle, only supported，0(Default)，180，270 three values
         */
        rotate?: boolean;
        /**
         * 1. When it is `true`: After the video picture is scaled proportionally, the height or width is aligned with the canvas area, and the picture is not stretched, but there are black edges. Equivalent to `setScaleMode(1)`
         * 2. When it is `false`: the video image completely fills the canvas area and the image will be stretched. Equivalent to `setScaleMode(0)`
         */
        isResize?: boolean;
        /**
         * 1. When it is `true`: After the video screen is scaled proportionally, the canvas area is completely filled, the screen is not stretched, and there are no black edges, but the screen is not fully displayed. Equivalent to `setScaleMode(2)`
         */
        isFullResize?: boolean;
        /**
         * 1. When it is `true`: the ws protocol does not check whether it is based on .flv for protocol analysis.。
         */
        isFlv?: boolean;
        /**
         * Whether to enable console debugging
         */
        debug?: boolean;
        /**
         * 1. Set the timeout length, in seconds
         * 2. before the connection is successful(loading)and mid-play(heart),If no data is returned after the set time period, the timeout event will be called back.
         */
        timeout?: number;
        /**
         * 1. Set the timeout length, in seconds
         * 2. Before the connection is successful, if no data is returned for more than the set time, the timeout event will be called back.
         */
        heartTimeout?: number;
        /**
         * 1. Set the timeout length, in seconds
         * 2. Before the connection is successful, if no data is returned for more than the set time, the timeout event will be called back.
         */
        loadingTimeout?: number;
        /**
         * Whether to support the double-click event of the screen, trigger the full screen, and cancel the full screen event
         */
        supportDblclickFullscreen?: boolean;
        /**
         * Whether to display the network
         */
        showBandwidth?: boolean;
        /**
         * Configure action buttons
         */
        operateBtns?: {
            /** Whether to show the full screen button */
            fullscreen?: boolean;
            /** Whether to display the screenshot button */
            screenshot?: boolean;
            /** Whether to display the play pause button */
            play?: boolean;
            /** Whether to show the sound button */
            audio?: boolean;
            /** Whether to display the recording button */
            record?: boolean;
        };
        /**
         * Turn on the screen to always be on. On mobile browsers, the canvas tag rendering video will not keep the screen always on like the video tag does.
         */
        keepScreenOn?: boolean;
        /**
         * Whether to turn on the sound, the default is to turn off the sound playback
         */
        isNotMute?: boolean;
        /**
         * Copywriting during loading process
         */
        loadingText?: string;
        /**
         * background image
         */
        background?: string;
        /**
         * Whether to enable MediaSource hard decoding
         * * Video encoding only supports H.264 video (Safari on iOS does not support）
         * * Not supported forceNoOffscreen for false (Turn on off-screen rendering)
         */
        useMSE?: boolean;
        /**
         * Whether to enable Webcodecs hard decoding
         * *  Video encoding only supports H.264 video (Requires Chrome version 94 or above, requires https or localhost environment)
         * *  support forceNoOffscreen is false (enables off-screen rendering)
         * */
        useWCS?: boolean;
        /**
         * Whether to enable keyboard shortcuts
         * Currently supported keyboard shortcuts are：esc -> Exit full screen；arrowUp -> sound increases；arrowDown -> sound reduction；
         */
        hotKey?: boolean;
        /**
         *  When using MSE or Webcodecs to play H265, will it automatically downgrade to wasm mode?。
         *  If set to false, the playback will be closed directly and an Error exception will be thrown. If set to true, it will automatically switch to wasm mode for playback.。
         */
        autoWasm?: boolean;
        /**
         * heartTimeout Automatically play again after the heartbeat times out, no longer throwing exceptions, and directly replay the video address.。
         */
        heartTimeoutReplay?: boolean,
        /**
         * heartTimeoutReplay After the number of attempts is exceeded, it will no longer play automatically.
         */
        heartTimeoutReplayTimes?: number,
        /**
         * loadingTimeout loadingThen it will play again automatically, no exception will be thrown, and the video address will be played again directly.。
         */
        loadingTimeoutReplay?: boolean,
        /**
         * heartTimeoutReplay After the number of attempts is exceeded, it will no longer play automatically.
         */
        loadingTimeoutReplayTimes?: number
        /**
         * wasmAfter the decoding error is reported, the exception is no longer thrown, but the video address is directly replayed.。
         */
        wasmDecodeErrorReplay?: boolean,
        /**
         * https://github.com/langhuihui/jessibuca/issues/152 solution
         * For example: WebGL image preprocessing takes 4 bytes of data each time by default, but the U and V component widths at 540x960 resolution are540/2=270Not divisible by 4, resulting in a green screen。
         */
        openWebglAlignment?: boolean,

        /**
         * webcodecsWhether hard decoding is rendered via the video tag
         */
        wcsUseVideoRender?: boolean,

        /**
         * Whether the bottom console is automatically hidden
         */
        controlAutoHide?: boolean,

        /**
         * Recorded video format
         */
        recordType?: 'webm' | 'mp4',

        /**
         * Whether to use web full screen(Rotate 90 degrees)（Will only take effect on mobile devices）。
         */
        useWebFullScreen?: boolean,

        /**
         * Whether to automatically use system full screen
         */
        autoUseSystemFullScreen?: boolean,
    }
}


declare class Jessibuca {

    constructor(config?: Jessibuca.Config);

    /**
     * Whether to enable console debugging printing
     @example
     // turn on
     jessibuca.setDebug(true)
     // Close
     jessibuca.setDebug(false)
     */
    setDebug(flag: boolean): void;

    /**
     * mute
     @example
     jessibuca.mute()
     */
    mute(): void;

    /**
     * Unmute
     @example
     jessibuca.cancelMute()
     */
    cancelMute(): void;

    /**
     * A method left to upper-level user operations to trigger audio recovery。
     *
     * iPhone，chromeWhen automatic playback is required, the audio must be muted and needs to be restored by a real user interaction. Code cannot be used.。
     *
     * https://developers.google.com/web/updates/2017/09/autoplay-policy-changes
     */
    audioResume(): void;

    /**
     *
     * Set the timeout length, in seconds
     * Before the connection is successful and during playback, if no data is returned for more than the set time, the timeout event will be called back.

     @example
     jessibuca.setTimeout(10)

     jessibuca.on('timeout',function(){
     //
     });
     */
    setTimeout(): void;

    /**
     * @param mode
     *      0 The video picture completely fills the canvas area, and the picture will be stretched, which is equivalent to the parameter `isResize`.false
     *
     *      1 After the video picture is proportionally scaled, the height or width is aligned with the canvas area. The picture is not stretched, but there are black edges. This is equivalent to the parameter `isResize`.true
     *
     *      2 After the video picture is scaled proportionally, the canvas area is completely filled, the picture is not stretched, and there are no black edges, but the picture is not fully displayed, which is equivalent to the parameters. `isFullResize` fortrue
     @example
     jessibuca.setScaleMode(0)

     jessibuca.setScaleMode(1)

     jessibuca.setScaleMode(2)
     */
    setScaleMode(mode: number): void;

    /**
     * Pause playback
     *
     * Can be called after pause `play()`Method to continue playing the previous stream。
     @example
     jessibuca.pause().then(()=>{
     console.log('pause success')

     jessibuca.play().then(()=>{

     }).catch((e)=>{

     })

     }).catch((e)=>{
     console.log('pause error',e);
     })
     */
    pause(): Promise<void>;

    /**
     * Close the video without releasing underlying resources
     @example
     jessibuca.close();
     */
    close(): void;

    /**
     * Close the video and release underlying resources
     @example
     jessibuca.destroy()
     */
    destroy(): void;

    /**
     * Clean canvas to black background
     @example
     jessibuca.clearView()
     */
    clearView(): void;

    /**
     * play video
     @example

     jessibuca.play('url').then(()=>{
     console.log('play success')
     }).catch((e)=>{
     console.log('play error',e)
     })
     // Add request header
     jessibuca.play('url',{headers:{'Authorization':'test111'}}).then(()=>{
     console.log('play success')
     }).catch((e)=>{
     console.log('play error',e)
     })
     */
    play(url?: string, options?: {
        headers: Object
    }): Promise<void>;

    /**
     * Resize the view
     */
    resize(): void;

    /**
     * Set the maximum buffering time in seconds. The player will automatically eliminate the delay.。
     *
     * Equivalent to the `videoBuffer` parameter。
     *
     @example
     // Set 200ms buffer
     jessibuca.setBufferTime(0.2)
     */
    setBufferTime(time: number): void;

    /**
     * Set the rotation angle, only supported，0(Default) ，180，270 three values。
     *
     * > It can be used to achieve small window and full-screen effects on the monitoring screen. Since iOS does not have a full-screen API, this method can simulate the full-screen effect within the page and the effect is consistent across multiple terminals.。   *
     @example
     jessibuca.setRotate(0)

     jessibuca.setRotate(90)

     jessibuca.setRotate(270)
     */
    setRotate(deg: number): void;

    /**
     *
     * Set the volume size and take the value0 — 1
     *
     * > Different from mute and cancelMute methods, although settingsetVolume(0) The mute method can also be reached, but the mute method does not call the underlying audio playback, which can improve performance. AndsetVolume(0)Just set the sound to 0 to achieve the effect。
     * @param volume When 0, it is completely silent;When 1, maximum volume, default value
     @example
     jessibuca.setVolume(0.2)

     jessibuca.setVolume(0)

     jessibuca.setVolume(1)
     */
    setVolume(volume: number): void;

    /**
     * Returns whether loading is complete
     @example
     var result = jessibuca.hasLoaded()
     console.log(result) // true
     */
    hasLoaded(): boolean;

    /**
     * Turn on the screen to always be on. On mobile browsers, the canvas tag rendering video will not keep the screen always on like the video tag does.。
     * H5Currently inchrome\edge 84, android chrome 84and above have native bright screen API, which needs to be an https page
     * The remaining platforms are simulated implementations and are compatible implementations at this time. There is no guarantee that all browsers will support them.
     @example
     jessibuca.setKeepScreenOn()
     */
    setKeepScreenOn(): boolean;

    /**
     * full screen(Cancel full screen)play video
     @example
     jessibuca.setFullscreen(true)
     //
     jessibuca.setFullscreen(false)
     */
    setFullscreen(flag: boolean): void;

    /**
     *
     * Take a screenshot. After calling, a download box will pop up to save the screenshot.
     * @param filename Optional parameters, saved file name, default `timestamp`
     * @param format   Optional parameter, screenshot format, optional png or jpeg or webp, default `png`
     * @param quality  Optional parameter, when the format is jpeg or webp, compression quality, value 0 ~ 1, default `0.92`
     * @param type Optional parameter, optional download or base64 or blob, default`download`

     @example

     jessibuca.screenshot("test","png",0.5)

     const base64 = jessibuca.screenshot("test","png",0.5,'base64')

     const fileBlob = jessibuca.screenshot("test",'blob')
     */
    screenshot(filename?: string, format?: string, quality?: number, type?: string): void;

    /**
     * Start recording。
     * @param fileName Optional, default timestamp
     * @param fileType Optional, default webm, supports webm and mp4 formats

     @example
     jessibuca.startRecord('xxx','webm')
     */
    startRecord(fileName: string, fileType: string): void;

    /**
     * Pause recording and download。
     @example
     jessibuca.stopRecordAndSave()
     */
    stopRecordAndSave(): void;

    /**
     * Returns whether it is playing or not。
     @example
     var result = jessibuca.isPlaying()
     console.log(result) // true
     */
    isPlaying(): boolean;

    /**
     *   Returns whether to mute。
     @example
     var result = jessibuca.isMute()
     console.log(result) // true
     */
    isMute(): boolean;

    /**
     * Returns whether recording is in progress。
     @example
     var result = jessibuca.isRecording()
     console.log(result) // true
     */
    isRecording(): boolean;

    /**
     * Toggle bottom control bar Hide/show
     * @param isShow
     *
     * @example
     * jessibuca.toggleControlBar(true) // show
     * jessibuca.toggleControlBar(false)  // hide
     * jessibuca.toggleControlBar() // Toggle Hide/show
     */
    toggleControlBar(isShow:boolean): void;

    /**
     * Get whether the bottom control bar is displayed
     */
    getControlBarShow(): boolean;

    /**
     * Listen to jessibuca initialization event
     * @example
     * jessibuca.on("load",function(){console.log('load')})
     */
    on(event: 'load', callback: () => void): void;

    /**
     * Video playback duration, unitms
     * @example
     * jessibuca.on('timeUpdate',function (ts) {console.log('timeUpdate',ts);})
     */
    on(event: 'timeUpdate', callback: () => void): void;

    /**
     * Callback when the video information is parsed, 2 callback parameters
     * @example
     * jessibuca.on("videoInfo",function(data){console.log('width:',data.width,'height:',data.width)})
     */
    on(event: 'videoInfo', callback: (data: {
        /** video width */
        width: number;
        /** video high */
        height: number;
    }) => void): void;

    /**
     * Callback when the audio information is parsed, 2 callback parameters
     * @example
     * jessibuca.on("audioInfo",function(data){console.log('numOfChannels:',data.numOfChannels,'sampleRate',data.sampleRate)})
     */
    on(event: 'audioInfo', callback: (data: {
        /** audio channel */
        numOfChannels: number;
        /** Sampling rate */
        sampleRate: number;
    }) => void): void;

    /**
     * information, including error messages
     * @example
     * jessibuca.on("log",function(data){console.log('data:',data)})
     */
    on(event: 'log', callback: () => void): void;

    /**
     * error message
     * @example
     * jessibuca.on("error",function(error){
     if(error === Jessibuca.ERROR.fetchError){
     //
     }
     else if(error === Jessibuca.ERROR.webcodecsH265NotSupport){
     //
     }
     console.log('error:',error)
     })
     */
    on(event: 'error', callback: (err: Jessibuca.ERROR) => void): void;

    /**
     * Current network speed, unit KB 1 time per second,
     * @example
     * jessibuca.on("kBps",function(data){console.log('kBps:',data)})
     */
    on(event: 'kBps', callback: (value: number) => void): void;

    /**
     * Rendering starts
     * @example
     * jessibuca.on("start",function(){console.log('start render')})
     */
    on(event: 'start', callback: () => void): void;

    /**
     * When no data is returned within the set timeout period, the callback
     * @example
     * jessibuca.on("timeout",function(error){console.log('timeout:',error)})
     */
    on(event: 'timeout', callback: (error: Jessibuca.TIMEOUT) => void): void;

    /**
     * Whenplay()When, if no data is returned, callback
     * @example
     * jessibuca.on("loadingTimeout",function(){console.log('timeout')})
     */
    on(event: 'loadingTimeout', callback: () => void): void;

    /**
     * During playback, if no data is rendered after timeout is exceeded, an exception will be thrown.。
     * @example
     * jessibuca.on("delayTimeout",function(){console.log('timeout')})
     */
    on(event: 'delayTimeout', callback: () => void): void;

    /**
     * Whether it is currently full screen
     * @example
     * jessibuca.on("fullscreen",function(flag){console.log('is fullscreen',flag)})
     */
    on(event: 'fullscreen', callback: () => void): void;

    /**
     * Trigger play event
     * @example
     * jessibuca.on("play",function(flag){console.log('play')})
     */
    on(event: 'play', callback: () => void): void;

    /**
     * trigger pause event
     * @example
     * jessibuca.on("pause",function(flag){console.log('pause')})
     */
    on(event: 'pause', callback: () => void): void;

    /**
     * Trigger a sound event and return a boolean value
     * @example
     * jessibuca.on("mute",function(flag){console.log('is mute',flag)})
     */
    on(event: 'mute', callback: () => void): void;

    /**
     * Stream status statistics, callback after the stream starts playing, once per second。
     * @example
     * jessibuca.on("stats",function(s){console.log("stats is",s)})
     */
    on(event: 'stats', callback: (stats: {
        /** Current buffer duration in milliseconds */
        buf: number;
        /** Current video frame rate */
        fps: number;
        /** Current audio bitrate, unitbyte */
        abps: number;
        /** Current video bitrate, unitbyte */
        vbps: number;
        /** Current video frame pts, unit milliseconds */
        ts: number;
    }) => void): void;

    /**
     * Rendering performance statistics, callback after the stream starts playing, once per second。
     * @param performance 0: It means stuck, 1: means smooth, 2: means very smooth.
     * @example
     * jessibuca.on("performance",function(performance){console.log("performance is",performance)})
     */
    on(event: 'performance', callback: (performance: 0 | 1 | 2) => void): void;

    /**
     * Recording start event

     * @example
     * jessibuca.on("recordStart",function(){console.log("record start")})
     */
    on(event: 'recordStart', callback: () => void): void;

    /**
     * Recording end event

     * @example
     * jessibuca.on("recordEnd",function(){console.log("record end")})
     */
    on(event: 'recordEnd', callback: () => void): void;

    /**
     * When recording, the recording duration is returned, once every 1s.

     * @example
     * jessibuca.on("recordingTimestamp",function(timestamp){console.log("recordingTimestamp is",timestamp)})
     */
    on(event: 'recordingTimestamp', callback: (timestamp: number) => void): void;

    /**
     * Listen to call the play method after initialization-> network request-> Decapsulation -> Decode -> Rendering time consumption of a series of processes
     * @param event
     * @param callback
     */
    on(event: 'playToRenderTimes', callback: (times: {
        playInitStart: number, // 1 initialization
        playStart: number, // 2 initialization
        streamStart: number, // 3 network request
        streamResponse: number, // 4 network request
        demuxStart: number, // 5 Decapsulation
        decodeStart: number, // 6 Decode
        videoStart: number, // 7 rendering
        playTimestamp: number,// playStart- playInitStart
        streamTimestamp: number,// streamStart - playStart
        streamResponseTimestamp: number,// streamResponse - streamStart
        demuxTimestamp: number, // demuxStart - streamResponse
        decodeTimestamp: number, // decodeStart - demuxStart
        videoTimestamp: number,// videoStart - decodeStart
        allTimestamp: number // videoStart - playInitStart
    }) => void): void

    /**
     * Listening method
     *
     @example

     jessibuca.on("load",function(){console.log('load')})
     */
    on(event: string, callback: Function): void;

}

export default Jessibuca;
