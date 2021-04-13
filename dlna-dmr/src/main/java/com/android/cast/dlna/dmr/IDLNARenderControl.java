package com.android.cast.dlna.dmr;


/**
 *
 */
public interface IDLNARenderControl {
    void play();

    void pause();

    void seek(long position);

    void stop();

    long getPosition();

    long getDuration();
    /**是否有播放器(已在播放界面)*/
    boolean hasPlayer();

    // -------------------------------------------------------------------------------------------
    // - VideoView impl
    // -------------------------------------------------------------------------------------------
    final class VideoViewRenderControl implements IDLNARenderControl {

        private VideoViewControlListener videoViewControlListener;

        public VideoViewRenderControl(VideoViewControlListener videoViewControlListener) {
            this.videoViewControlListener = videoViewControlListener;
        }

        @Override
        public void play() {
            if(videoViewControlListener != null) {
                videoViewControlListener.play();
            }
        }

        @Override
        public void pause() {
            if(videoViewControlListener != null) {
                videoViewControlListener.pause();
            }
        }

        @Override
        public void seek(long position) {
            if(videoViewControlListener != null) {
                videoViewControlListener.seek(position);
            }
        }

        @Override
        public void stop() {
            if(videoViewControlListener != null) {
                videoViewControlListener.stop();
            }
        }

        @Override
        public long getPosition() {
            if(videoViewControlListener != null) {
                return videoViewControlListener.getPosition();
            }
            return 0;
        }

        @Override
        public long getDuration() {
            if(videoViewControlListener != null) {
                return videoViewControlListener.getDuration();
            }
            return 0;
        }

        /**是否有播放器(已在播放界面)*/
        @Override
        public boolean hasPlayer(){
            if(videoViewControlListener != null) {
                return videoViewControlListener.hasPlayer();
            }
            return false;
        }

        /**
         * 释放
         * */
        public void realse(){
            videoViewControlListener = null;
        }
    }

    // -------------------------------------------------------------------------------------------
    // - Default impl
    // -------------------------------------------------------------------------------------------
    final class DefaultRenderControl implements IDLNARenderControl {
        public DefaultRenderControl() {
        }

        @Override
        public void play() {
        }

        @Override
        public void pause() {
        }

        @Override
        public void seek(long position) {
        }

        @Override
        public void stop() {
        }

        @Override
        public long getPosition() {
            return 0L;
        }

        @Override
        public long getDuration() {
            return 0L;
        }

        @Override
        public boolean hasPlayer() {
            return false;
        }
    }
}
