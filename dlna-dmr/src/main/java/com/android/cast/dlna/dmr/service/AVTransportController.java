package com.android.cast.dlna.dmr.service;


import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.android.cast.dlna.core.Utils;
import com.android.cast.dlna.dmr.DLNARendererActivity;
import com.android.cast.dlna.dmr.IDLNARenderControl;

import org.fourthline.cling.model.types.ErrorCode;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.AVTransportErrorCode;
import org.fourthline.cling.support.avtransport.AVTransportException;
import org.fourthline.cling.support.model.DeviceCapabilities;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.StorageMedium;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportSettings;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

public class AVTransportController implements IRendererInterface.IAVTransportControl {
    private final String TAG = "AVTransportController";
    private static final TransportAction[] TRANSPORT_ACTION_STOPPED = new TransportAction[]{TransportAction.Play};
    private static final TransportAction[] TRANSPORT_ACTION_PLAYING = new TransportAction[]{TransportAction.Stop, TransportAction.Pause, TransportAction.Seek};
    private static final TransportAction[] TRANSPORT_ACTION_PAUSE_PLAYBACK = new TransportAction[]{TransportAction.Play, TransportAction.Seek, TransportAction.Stop};

    private final UnsignedIntegerFourBytes mInstanceId;
    private final Context mApplicationContext;
    private TransportInfo mTransportInfo = new TransportInfo();
    private final TransportSettings mTransportSettings = new TransportSettings();
    private PositionInfo mOriginPositionInfo = new PositionInfo();
    private MediaInfo mMediaInfo = new MediaInfo();
    private final IDLNARenderControl mMediaControl;

    public AVTransportController(Context context, IDLNARenderControl control) {
        this(context, new UnsignedIntegerFourBytes(0), control);
    }

    public AVTransportController(Context context, UnsignedIntegerFourBytes instanceId, IDLNARenderControl control) {
        mApplicationContext = context.getApplicationContext();
        mInstanceId = instanceId;
        mMediaControl = control;
    }

    public UnsignedIntegerFourBytes getInstanceId() {
        return mInstanceId;
    }

    public synchronized TransportAction[] getCurrentTransportActions() {
        switch (mTransportInfo.getCurrentTransportState()) {
            case PLAYING:
                return TRANSPORT_ACTION_PLAYING;
            case PAUSED_PLAYBACK:
                return TRANSPORT_ACTION_PAUSE_PLAYBACK;
            default:
                return TRANSPORT_ACTION_STOPPED;
        }
    }

    @Override
    public DeviceCapabilities getDeviceCapabilities() {
        return new DeviceCapabilities(new StorageMedium[]{StorageMedium.NETWORK});
    }

    @Override
    public MediaInfo getMediaInfo() {
        return mMediaInfo;
    }

    @Override
    public PositionInfo getPositionInfo() {
        return new PositionInfo(mOriginPositionInfo, mMediaControl.getPosition() / 1000, mMediaControl.getDuration() / 1000);
    }

    @Override
    public TransportInfo getTransportInfo() {
        return mTransportInfo;
    }

    /**设置播放状态(DMC会获取)*/
    public void setTransportInfo(TransportInfo transportInfo){
        mTransportInfo = transportInfo;
    }

    @Override
    public TransportSettings getTransportSettings() {
        return mTransportSettings;
    }

    @Override
    public void setAVTransportURI(String currentURI, String currentURIMetaData) throws AVTransportException {
        try {
            new URI(currentURI);
        } catch (Exception ex) {
            throw new AVTransportException(ErrorCode.INVALID_ARGS, "CurrentURI can not be null or malformed");
        }
        Log.e("setAVTransportURI", "url=" + currentURI);
        mMediaInfo = new MediaInfo(currentURI, currentURIMetaData, new UnsignedIntegerFourBytes(1), "", StorageMedium.NETWORK);
        mOriginPositionInfo = new PositionInfo(1, currentURIMetaData, currentURI);

        if(mMediaControl != null && mMediaControl.hasPlayer()){
            //通知Player页面关闭或者换源播放
        }
        //logXmlData(currentURIMetaData);
        DLNARendererActivity.startActivity(mApplicationContext, currentURI);
    }

    private void logXmlData(String xmlStr) {
        try {
            XmlPullParser xmlParser = Xml.newPullParser();
            xmlParser.setInput(new StringReader(xmlStr));
            int event = xmlParser.getEventType();   //先获取当前解析器光标在哪
            while (event != XmlPullParser.END_DOCUMENT){    //如果还没到文档的结束标志，那么就继续往下处理
                switch (event){
                    case XmlPullParser.START_DOCUMENT:
                        Log.e(TAG, "xml解析开始");
                        break;
                    case XmlPullParser.START_TAG:
                        //一般都是获取标签的属性值，所以在这里数据你需要的数据
                        Log.e(TAG, "当前标签是：" + xmlParser.getName());
                        if (xmlParser.getName().equals("title")){
                            Log.e(TAG, "当前标签是：" + xmlParser.getName() + ",videoTitle" + xmlParser.nextText());
                            //两种方法获取属性值
                            //Log.e(TAG, "第一个属性：" + xmlParser.getAttributeName(0)
                            //        + ": " + xmlParser.getAttributeValue(0));
                            //Log.e(TAG, "第二个属性：" + xmlParser.getAttributeName(1)+": "
                            //        + xmlParser.getAttributeValue(null,"att2"));
                        }else{
                        }
                        break;
                    case XmlPullParser.TEXT:
                        Log.e(TAG, "Text:" + xmlParser.getText());
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        break;
                }
                event = xmlParser.next();   //将当前解析器光标往下一步移
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setNextAVTransportURI(String nextURI, String nextURIMetaData) {
    }

    @Override
    public void play(String speed) {
        mMediaControl.play();
    }

    public void pause() {
        mMediaControl.pause();
    }

    @Override
    public void seek(String unit, String target) throws AVTransportException {
        SeekMode seekMode = SeekMode.valueOrExceptionOf(unit);
        if (!seekMode.equals(SeekMode.REL_TIME)) {
            throw new AVTransportException(AVTransportErrorCode.SEEKMODE_NOT_SUPPORTED, "Unsupported seek mode: " + unit);
        }
        long position = Utils.getIntTime(target);
        mMediaControl.seek(position);
    }

    synchronized public void stop() {
        mMediaControl.stop();
    }

    @Override
    public void previous() {
    }

    @Override
    public void next() {
    }

    @Override
    public void record() {
    }

    @Override
    public void setPlayMode(String newPlayMode) {
    }

    @Override
    public void setRecordQualityMode(String newRecordQualityMode) {
    }
}
