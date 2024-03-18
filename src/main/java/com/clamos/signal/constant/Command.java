package com.clamos.signal.constant;

public final class Command {

    /*command*/
    public static final String AUDIO = "audio";
    public static final String DRAW_PEN = "drawPen";
    public static final String PTZ_STAT = "ptzStat";
    public static final String START_CAM = "startCam";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String CONFIG = "config";
    public static final String MEDIA_LIST = "mediaList";
    public static final String PLAY_CAM = "playCam";
    public static final String LOGIN = "login";
    public static final String SESSION_LIST = "sessionList";
    public static final String CANDIDATE = "candidate";

    /* Redis Key Web*/
    public static final String WEB_START_CAM = "webStartCam";
    public static final String WEB_STOP_CAM = "webStopCam";
    public static final String WEB_AUDIO = "webAudio";
    public static final String WEB_DRAW_PEN = "webDrawPen";
    public static final String SHARE_CAM = "shareCam";
    public static final String SHARE_IMG = "shareImg";
    public static final String TOGGLE_CAM = "toggleCam";
    public static final String PTZ = "ptz";
    public static final String WEB_PTZ_STAT = "webPtzStat";
    public static final String LOGIN_FORCE = "force";
    public static final String WEB_RELOAD = "reload";
    public static final String WEB_ORI = "ori";


    /*Redis Key Deive*/
    public static final String DEVICE_PTZ_STAT = "devicePtzStat";
    public static final String DEVICE_STATUS = "deviceStatus";
    public static final String DEVICE_DRAW_PEN = "deviceDrawPen";
    public static final String SIGNAL_STATUS = "signalStatus";

    public static final String DEVICE_ORI = "ori";

    /* RedisKey media */
    public static final String MEDIA_STATUS = "mediaStatus";

    /* RedisKey manager */
    public static final String EVENT_START = "eventStart";
    public static final String EVENT_STOP = "eventStop";
    public static final String DEVICE_CONFIG = "deviceConfig";

    public static final String EMPOWERMENT_START = "empowermentStart";
    public static final String EMPOWERMENT_STOP = "empowermentStop";
    public static final String ALCHERA = "alchera";
    public static final String REMOTE_PLAY_START  = "remotePlayStart";
    public static final String REMOTE_PLAY_STOP  = "remotePlayStop";



}

