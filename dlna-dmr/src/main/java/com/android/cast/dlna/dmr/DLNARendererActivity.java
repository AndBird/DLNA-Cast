package com.android.cast.dlna.dmr;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.cast.dlna.dmr.DLNARendererService.RendererServiceBinder;

import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelVolume;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlVariable;

import androidx.annotation.Nullable;

/**
 *
 */
public class DLNARendererActivity extends Activity {
    private final String TAG = DLNARendererActivity.class.getSimpleName();

    private static final String KEY_EXTRA_CURRENT_URI = "Renderer.KeyExtra.CurrentUri";

    public static void startActivity(Context context, String currentURI) {
        Intent intent = new Intent(context, DLNARendererActivity.class);
        intent.putExtra(KEY_EXTRA_CURRENT_URI, currentURI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // start from service content,should add 'FLAG_ACTIVITY_NEW_TASK' flag.
        context.startActivity(intent);
    }

    private VideoView mVideoView;
    private ProgressBar mProgressBar;

    private final UnsignedIntegerFourBytes INSTANCE_ID = new UnsignedIntegerFourBytes(0);
    private DLNARendererService mRendererService;

    private IDLNARenderControl.VideoViewRenderControl renderControl;

    private VideoViewControlListener videoViewControlListener = new VideoViewControlListener() {
        @Override
        public void play() {
            Log.e(TAG, "dlna start:");
            mVideoView.start();
        }

        @Override
        public void pause() {
            Log.e(TAG, "dlna pause:");
            mVideoView.pause();
        }

        @Override
        public void seek(long position) {
            Log.e(TAG, "dlna seek:" + position);
            mVideoView.seekTo((int)position);
        }

        @Override
        public void stop() {
            Log.e(TAG, "dlna stop:");
            mVideoView.stopPlayback();
        }

        @Override
        public long getPosition() {
            Log.e(TAG, "dlna getCurrentPosition:");
            return mVideoView.getCurrentPosition();
        }

        @Override
        public long getDuration() {
            Log.e(TAG, "dlna getDuration:");
            if(mVideoView != null) {
                return mVideoView.getDuration();
            }
            return 0;
        }

        @Override
        public boolean hasPlayer() {
            if(mVideoView != null){
                return true;
            }
            return false;
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRendererService = ((RendererServiceBinder) service).getRendererService();
            renderControl = new IDLNARenderControl.VideoViewRenderControl(videoViewControlListener);
            mRendererService.setRenderControl(renderControl);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if(renderControl != null) {
                renderControl.realse();
            }
            renderControl = null;
            mRendererService = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dlna_renderer);
        mVideoView = findViewById(R.id.video_view);
        mProgressBar = findViewById(R.id.video_progress);
        bindService(new Intent(this, DLNARendererService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
        openMedia(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        openMedia(intent);
    }

    private void openMedia(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mProgressBar.setVisibility(View.VISIBLE);
            String currentUri = bundle.getString(KEY_EXTRA_CURRENT_URI);
            mVideoView.setVideoURI(Uri.parse(currentUri));
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mProgressBar.setVisibility(View.INVISIBLE);
                    notifyTransportStateChanged(TransportState.PLAYING);
                }
            });
            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    notifyTransportStateChanged(TransportState.STOPPED);
                    finish();
                    return true;
                }
            });
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    notifyTransportStateChanged(TransportState.STOPPED);
                    finish();
                }
            });
        } else {
            Toast.makeText(this, "没有找到有效的视频地址，请检查...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mVideoView != null) mVideoView.stopPlayback();
        notifyTransportStateChanged(TransportState.STOPPED);
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = super.onKeyDown(keyCode, event);
        if (mRendererService != null) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                int volume = ((AudioManager) getApplication().getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC);
                notifyRenderVolumeChanged(volume);
            } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (mVideoView != null && mVideoView.isPlaying()) {
                    mVideoView.pause();
                    notifyTransportStateChanged(TransportState.PAUSED_PLAYBACK);
                } else if (mVideoView != null) {
                    mVideoView.resume();
                    notifyTransportStateChanged(TransportState.PLAYING);
                }
            }
        }
        return handled;
    }

    private void notifyTransportStateChanged(TransportState transportState) {
        if (mRendererService != null) {
            mRendererService.getAvTransportLastChange()
                    .setEventedValue(INSTANCE_ID, new AVTransportVariable.TransportState(transportState));

            //更新播放状态
            if(mRendererService.getAVTransportController() != null) {
                switch (transportState) {
                    case PLAYING:
                        mRendererService.getAVTransportController().setTransportInfo(new TransportInfo(TransportState.PLAYING));
                        break;
                    case PAUSED_PLAYBACK:
                        mRendererService.getAVTransportController().setTransportInfo(new TransportInfo(TransportState.PAUSED_PLAYBACK));
                        break;
                    default:
                        mRendererService.getAVTransportController().setTransportInfo(new TransportInfo(TransportState.STOPPED));
                        break;
                }
            }
        }
    }

    private void notifyRenderVolumeChanged(int volume) {
        if (mRendererService != null) {
            mRendererService.getAudioControlLastChange()
                    .setEventedValue(INSTANCE_ID, new RenderingControlVariable.Volume(new ChannelVolume(Channel.Master, volume)));
        }
    }
}
