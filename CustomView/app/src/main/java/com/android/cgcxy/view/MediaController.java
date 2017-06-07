package com.android.cgcxy.view;

/**
 * Created by chuangguo.qi on 2017/5/12.
 */

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.android.cgcxy.customview.R;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

/**
 * A view containing controls for a MediaPlayer. Typically contains the
 * buttons like "Play/Pause", "Rewind", "Fast Forward" and a progress
 * slider. It takes care of synchronizing the controls with the state
 * of the MediaPlayer.
 * <p>
 * The way to use this class is to instantiate it programmatically.
 * The MediaController will create a default set of controls
 * and put them in a window floating above your application. Specifically,
 * the controls will float above the view specified with setAnchorView().
 * The window will disappear if left idle for three seconds and reappear
 * when the user touches the anchor view.
 * <p>
 * Functions like show() and hide() have no effect when MediaController
 * is created in an xml layout.
 *
 * MediaController will hide and
 * show the buttons according to these rules:
 * <ul>
 * <li> The "previous" and "next" buttons are hidden until setPrevNextListeners()
 *   has been called
 * <li> The "previous" and "next" buttons are visible but disabled if
 *   setPrevNextListeners() was called with null listeners
 * <li> The "rewind" and "fastforward" buttons are shown unless requested
 *   otherwise by using the MediaController(Context, boolean) constructor
 *   with the boolean set to false
 * </ul>
 */
public class MediaController extends FrameLayout {

    private android.widget.MediaController.MediaPlayerControl mPlayer;
    private final Context mContext;
    private View mAnchor;
    private View mRoot;
    private WindowManager mWindowManager;
    private Window mWindow;
    private View mDecor;
    private WindowManager.LayoutParams mDecorLayoutParams;
    private ProgressBar mProgress;
    private TextView mEndTime, mCurrentTime;
    private boolean mShowing;
    private boolean mDragging;
    private static final int sDefaultTimeout = 3000;
    private final boolean mUseFastForward;
    private boolean mFromXml;
    private boolean mListenersSet;
    private View.OnClickListener mNextListener, mPrevListener;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private ImageButton mPauseButton;
    private ImageButton mFfwdButton;
    private ImageButton mRewButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private CharSequence mPlayDescription;
    private CharSequence mPauseDescription;
    //private final AccessibilityManager mAccessibilityManager;

    public MediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = this;
        mContext = context;
        mUseFastForward = true;
        mFromXml = true;
        //mAccessibilityManager = AccessibilityManager.getInstance(context);
    }

    @Override
    public void onFinishInflate() {
        if (mRoot != null)
            initControllerView(mRoot);
    }

    public MediaController(Context context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;
        initFloatingWindowLayout();
        initFloatingWindow();
        //mAccessibilityManager = AccessibilityManager.getInstance(context);
    }

    public MediaController(Context context) {
        this(context, true);
    }

    private void initFloatingWindow() {
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        //mWindow = new PhoneWindow(mContext);
        mWindow = PolicyCompat.createWindow(mContext);
        mWindow.setWindowManager(mWindowManager, null, null);
        mWindow.requestFeature(Window.FEATURE_NO_TITLE);
        mDecor = mWindow.getDecorView();
        mDecor.setOnTouchListener(mTouchListener);
        mWindow.setContentView(this);
        mWindow.setBackgroundDrawableResource(android.R.color.transparent);

        // While the media controller is up, the volume control keys should
        // affect the media stream type
        mWindow.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        requestFocus();
    }

    // Allocate and initialize the static parts of mDecorLayoutParams. Must
    // also call updateFloatingWindowLayout() to fill in the dynamic parts
    // (y and width) before mDecorLayoutParams can be used.
    private void initFloatingWindowLayout() {
        mDecorLayoutParams = new WindowManager.LayoutParams();
        WindowManager.LayoutParams p = mDecorLayoutParams;
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.height = LayoutParams.WRAP_CONTENT;
        p.x = 0;
        p.format = PixelFormat.TRANSLUCENT;
        p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;
        p.token = null;
        p.windowAnimations = 0; // android.R.style.DropDownAnimationDown;
    }

    // Update the dynamic parts of mDecorLayoutParams
    // Must be called with mAnchor != NULL.
    private void updateFloatingWindowLayout() {
        int [] anchorPos = new int[2];
        mAnchor.getLocationOnScreen(anchorPos);

        // we need to know the size of the controller so we can properly position it
        // within its space
        mDecor.measure(MeasureSpec.makeMeasureSpec(mAnchor.getWidth(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(mAnchor.getHeight(), MeasureSpec.AT_MOST));

        WindowManager.LayoutParams p = mDecorLayoutParams;
        p.width = mAnchor.getWidth();
        p.x = anchorPos[0] + (mAnchor.getWidth() - p.width) / 2;
        p.y = anchorPos[1] + mAnchor.getHeight() - mDecor.getMeasuredHeight();
    }

    // This is called whenever mAnchor's layout bound changes
    private final OnLayoutChangeListener mLayoutChangeListener =
            new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight,
                                           int oldBottom) {
                    updateFloatingWindowLayout();
                    if (mShowing) {
                        mWindowManager.updateViewLayout(mDecor, mDecorLayoutParams);
                    }
                }
            };

    private final OnTouchListener mTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mShowing) {
                    hide();
                }
            }
            return false;
        }
    };

    public void setMediaPlayer(android.widget.MediaController.MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * When VideoView calls this method, it will use the VideoView's parent
     * as the anchor.
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(View view) {
        if (mAnchor != null) {
            mAnchor.removeOnLayoutChangeListener(mLayoutChangeListener);
        }
        mAnchor = view;
        if (mAnchor != null) {
            mAnchor.addOnLayoutChangeListener(mLayoutChangeListener);
        }

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_controller, null);

        initControllerView(mRoot);

        return mRoot;
    }

    private void initControllerView(View v) {
        Resources res = mContext.getResources();
        mPlayDescription = res
                .getText(R.string.lockscreen_transport_play_description);
        mPauseDescription = res
                .getText(R.string.lockscreen_transport_pause_description);
        mPauseButton = (ImageButton) v.findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mFfwdButton = (ImageButton) v.findViewById(R.id.ffwd);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(mFfwdListener);
            if (!mFromXml) {
                mFfwdButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        mRewButton = (ImageButton) v.findViewById(R.id.rew);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(mRewListener);
            if (!mFromXml) {
                mRewButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        // By default these are hidden. They will be enabled when setPrevNextListeners() is called
        mNextButton = (ImageButton) v.findViewById(R.id.next);
        if (mNextButton != null && !mFromXml && !mListenersSet) {
            mNextButton.setVisibility(View.GONE);
        }
        mPrevButton = (ImageButton) v.findViewById(R.id.prev);
        if (mPrevButton != null && !mFromXml && !mListenersSet) {
            mPrevButton.setVisibility(View.GONE);
        }

        mProgress = (ProgressBar) v.findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) v.findViewById(R.id.time);
        mCurrentTime = (TextView) v.findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        installPrevNextListeners();
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
            if (mPauseButton != null && !mPlayer.canPause()) {
                mPauseButton.setEnabled(false);
            }
            if (mRewButton != null && !mPlayer.canSeekBackward()) {
                mRewButton.setEnabled(false);
            }
            if (mFfwdButton != null && !mPlayer.canSeekForward()) {
                mFfwdButton.setEnabled(false);
            }
            // TODO What we really should do is add a canSeek to the MediaPlayerControl interface;
            // this scheme can break the case when applications want to allow seek through the
            // progress bar but disable forward/backward buttons.
            //
            // However, currently the flags SEEK_BACKWARD_AVAILABLE, SEEK_FORWARD_AVAILABLE,
            // and SEEK_AVAILABLE are all (un)set together; as such the aforementioned issue
            // shouldn't arise in existing applications.
            if (mProgress != null && !mPlayer.canSeekBackward() && !mPlayer.canSeekForward()) {
                mProgress.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    public void show(int timeout) {
        if (!mShowing && mAnchor != null) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            disableUnsupportedButtons();
            updateFloatingWindowLayout();
            mWindowManager.addView(mDecor, mDecorLayoutParams);
            mShowing = true;
        }
        updatePausePlay();

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        post(mShowProgress);

       /* if (timeout != 0 && !mAccessibilityManager.isTouchExplorationEnabled()) {
            removeCallbacks(mFadeOut);
            postDelayed(mFadeOut, timeout);
        }*/
    }

    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mAnchor == null)
            return;

        if (mShowing) {
            try {
                removeCallbacks(mShowProgress);
                mWindowManager.removeView(mDecor);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
            mShowing = false;
        }
    }

    private final Runnable mFadeOut = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final Runnable mShowProgress = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            if (!mDragging && mShowing && mPlayer.isPlaying()) {
                postDelayed(mShowProgress, 1000 - (pos % 1000));
            }
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress( (int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                show(0); // show until hide is called
                break;
            case MotionEvent.ACTION_UP:
                show(sDefaultTimeout); // start timeout
                break;
            case MotionEvent.ACTION_CANCEL:
                hide();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mPauseButton != null) {
                    mPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            doPauseResume();
            show(sDefaultTimeout);
        }
    };

    private void updatePausePlay() {
        if (mRoot == null || mPauseButton == null)
            return;

        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            mPauseButton.setContentDescription(mPauseDescription);
        } else {
            mPauseButton.setImageResource(android.R.drawable.ic_media_play);
            mPauseButton.setContentDescription(mPlayDescription);
        }
    }

    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);

            mDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            removeCallbacks(mShowProgress);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            mPlayer.seekTo( (int) newposition);
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime( (int) newposition));
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            updatePausePlay();
            show(sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            post(mShowProgress);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mFfwdButton != null) {
            mFfwdButton.setEnabled(enabled);
        }
        if (mRewButton != null) {
            mRewButton.setEnabled(enabled);
        }
        if (mNextButton != null) {
            mNextButton.setEnabled(enabled && mNextListener != null);
        }
        if (mPrevButton != null) {
            mPrevButton.setEnabled(enabled && mPrevListener != null);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return android.widget.MediaController.class.getName();
    }

    private final View.OnClickListener mRewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = mPlayer.getCurrentPosition();
            pos -= 5000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        }
    };

    private final View.OnClickListener mFfwdListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = mPlayer.getCurrentPosition();
            pos += 15000; // milliseconds
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        }
    };

    private void installPrevNextListeners() {
        if (mNextButton != null) {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setEnabled(mNextListener != null);
        }

        if (mPrevButton != null) {
            mPrevButton.setOnClickListener(mPrevListener);
            mPrevButton.setEnabled(mPrevListener != null);
        }
    }

    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;
        mListenersSet = true;

        if (mRoot != null) {
            installPrevNextListeners();

            if (mNextButton != null && !mFromXml) {
                mNextButton.setVisibility(View.VISIBLE);
            }
            if (mPrevButton != null && !mFromXml) {
                mPrevButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public interface MediaPlayerControl {
        void    start();
        void    pause();
        int     getDuration();
        int     getCurrentPosition();
        void    seekTo(int pos);
        boolean isPlaying();
        int     getBufferPercentage();
        boolean canPause();
        boolean canSeekBackward();
        boolean canSeekForward();

        /**
         * Get the audio session id for the player used by this VideoView. This can be used to
         * apply audio effects to the audio track of a video.
         * @return The audio session, or 0 if there was an error.
         */
        int     getAudioSessionId();
    }

    public static class PolicyCompat {
        /*
         * Private constants
         */
        private static final String PHONE_WINDOW_CLASS_NAME   = "com.android.internal.policy.PhoneWindow";
        private static final String POLICY_MANAGER_CLASS_NAME = "com.android.internal.policy.PolicyManager";


        private PolicyCompat() {
        }


        /*
         * Private methods
         */
        private static Window createPhoneWindow(Context context) {
            try {
                /* Find class */
                Class<?> cls = Class.forName(PHONE_WINDOW_CLASS_NAME);

                /* Get constructor */
                Constructor c = cls.getConstructor(Context.class);

                /* Create instance */
                return (Window)c.newInstance(context);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(PHONE_WINDOW_CLASS_NAME + " could not be loaded", e);
            }
            catch (Exception e) {
                throw new RuntimeException(PHONE_WINDOW_CLASS_NAME + " class could not be instantiated", e);
            }
        }

        private static Window makeNewWindow(Context context) {
            try {
                /* Find class */
                Class<?> cls = Class.forName(POLICY_MANAGER_CLASS_NAME);

                /* Find method */
                Method m = cls.getMethod("makeNewWindow", Context.class);

                /* Invoke method */
                return (Window)m.invoke(null, context);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(POLICY_MANAGER_CLASS_NAME + " could not be loaded", e);
            }
            catch (Exception e) {
                throw new RuntimeException(POLICY_MANAGER_CLASS_NAME + ".makeNewWindow could not be invoked", e);
            }
        }

        /*
         * Public methods
         */
        public static Window createWindow(Context context) {
            if (false)
                return createPhoneWindow(context);
            else
                return makeNewWindow(context);
        }
    }

    /**
     * Displays a video file.  The VideoView class
     * can load images from various sources (such as resources or content
     * providers), takes care of computing its measurement from the video so that
     * it can be used in any layout manager, and provides various display options
     * such as scaling and tinting.<p>
     *
     * <em>Note: VideoView does not retain its full state when going into the
     * background.</em>  In particular, it does not restore the current play state,
     * play position, selected tracks, or any subtitle tracks added via
     * {@link #addSubtitleSource addSubtitleSource()}.  Applications should
     * save and restore these on their own in
     * {@link android.app.Activity#onSaveInstanceState} and
     * {@link android.app.Activity#onRestoreInstanceState}.<p>
     * Also note that the audio session id (from {@link #getAudioSessionId}) may
     * change from its previously returned value when the VideoView is restored.
     */
    @SuppressLint("NewApi")
    public static class VideoView extends SurfaceView implements android.widget.MediaController.MediaPlayerControl {
        private String TAG = "VideoView";
        // settable by the client
        private Uri mUri;
        private Map<String, String> mHeaders;

        // all possible internal states
        private static final int STATE_ERROR              = -1;
        private static final int STATE_IDLE               = 0;
        private static final int STATE_PREPARING          = 1;
        private static final int STATE_PREPARED           = 2;
        private static final int STATE_PLAYING            = 3;
        private static final int STATE_PAUSED             = 4;
        private static final int STATE_PLAYBACK_COMPLETED = 5;

        // mCurrentState is a VideoView object's current state.
        // mTargetState is the state that a method caller intends to reach.
        // For instance, regardless the VideoView object's current state,
        // calling pause() intends to bring the object to a target state
        // of STATE_PAUSED.
        private int mCurrentState = STATE_IDLE;
        private int mTargetState  = STATE_IDLE;

        // All the stuff we need for playing and showing a video
        private SurfaceHolder mSurfaceHolder = null;
        private MediaPlayer mMediaPlayer = null;
        private int         mAudioSession;
        private int         mVideoWidth;
        private int         mVideoHeight;
        private int         mSurfaceWidth;
        private int         mSurfaceHeight;
        private MediaController mMediaController;
        private MediaPlayer.OnCompletionListener mOnCompletionListener;
        private MediaPlayer.OnPreparedListener mOnPreparedListener;
        private int         mCurrentBufferPercentage;
        private MediaPlayer.OnErrorListener mOnErrorListener;
        private MediaPlayer.OnInfoListener mOnInfoListener;
        private int         mSeekWhenPrepared;  // recording the seek position while preparing
        private boolean     mCanPause;
        private boolean     mCanSeekBack;
        private boolean     mCanSeekForward;
        private LinearLayout ll_title;

        public VideoView(Context context) {
            super(context);
            initVideoView();
        }

        public VideoView(Context context, AttributeSet attrs) {
            super(context, attrs);
            initVideoView();
        }

        public VideoView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            initVideoView();
        }

        public VideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr);
            initVideoView();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            //Log.i("@@@@", "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) + ", "
            //        + MeasureSpec.toString(heightMeasureSpec) + ")");

            int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
            int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
    //        if (mVideoWidth > 0 && mVideoHeight > 0) {
    //            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
    //            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
    //            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
    //            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
    //
    //            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
    //                // the size is fixed
    //                width = widthSpecSize;
    //                height = heightSpecSize;
    //
    //                // for compatibility, we adjust size based on aspect ratio
    //                if ( mVideoWidth * height  < width * mVideoHeight ) {
    //                    //Log.i("@@@", "image too wide, correcting");
    //                    width = height * mVideoWidth / mVideoHeight;
    //                } else if ( mVideoWidth * height  > width * mVideoHeight ) {
    //                    //Log.i("@@@", "image too tall, correcting");
    //                    height = width * mVideoHeight / mVideoWidth;
    //                }
    //            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
    //                // only the width is fixed, adjust the height to match aspect ratio if possible
    //                width = widthSpecSize;
    //                height = width * mVideoHeight / mVideoWidth;
    //                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
    //                    // couldn't match aspect ratio within the constraints
    //                    height = heightSpecSize;
    //                }
    //            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
    //                // only the height is fixed, adjust the width to match aspect ratio if possible
    //                height = heightSpecSize;
    //                width = height * mVideoWidth / mVideoHeight;
    //                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
    //                    // couldn't match aspect ratio within the constraints
    //                    width = widthSpecSize;
    //                }
    //            } else {
    //                // neither the width nor the height are fixed, try to use actual video size
    //                width = mVideoWidth;
    //                height = mVideoHeight;
    //                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
    //                    // too tall, decrease both width and height
    //                    height = heightSpecSize;
    //                    width = height * mVideoWidth / mVideoHeight;
    //                }
    //                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
    //                    // too wide, decrease both width and height
    //                    width = widthSpecSize;
    //                    height = width * mVideoHeight / mVideoWidth;
    //                }
    //            }
    //        } else {
    //            // no size yet, just adopt the given spec sizes
    //        }
            setMeasuredDimension(width, height);

            if(mSurfaceHolder!=null)
            {
                mSurfaceHolder.setFixedSize(width, height);
            }
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            event.setClassName(VideoView.class.getName());
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(VideoView.class.getName());
        }

        public int resolveAdjustedSize(int desiredSize, int measureSpec) {
            return getDefaultSize(desiredSize, measureSpec);
        }

        @SuppressWarnings("deprecation")
        private void initVideoView() {
            mVideoWidth = 0;
            mVideoHeight = 0;
            getHolder().addCallback(mSHCallback);
            getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
        }

        /**
         * Sets video path.
         *
         * @param path the path of the video.
         */
        public void setVideoPath(String path) {
            setVideoURI(Uri.parse(path));
        }

        /**
         * Sets video URI.
         *
         * @param uri the URI of the video.
         */
        public void setVideoURI(Uri uri) {
            setVideoURI(uri, null);
        }

        /**
         * Sets video URI using specific headers.
         *
         * @param uri     the URI of the video.
         * @param headers the headers for the URI request.
         *                Note that the cross domain redirection is allowed by default, but that can be
         *                changed with key/value pairs through the headers parameter with
         *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
         *                to disallow or allow cross domain redirection.
         */
        public void setVideoURI(Uri uri, Map<String, String> headers) {
            mUri = uri;
            mHeaders = headers;
            mSeekWhenPrepared = 0;
            openVideo();
            requestLayout();
            invalidate();
        }

        public void stopPlayback() {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                mCurrentState = STATE_IDLE;
                mTargetState  = STATE_IDLE;
            }
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        private void openVideo() {
            if (mUri == null || mSurfaceHolder == null) {
                // not ready for playback just yet, will try again later
                return;
            }
            AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            // we shouldn't clear the target state, because somebody might have
            // called start() previously
            release(false);
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setOnPreparedListener(mPreparedListener);
                mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
                mMediaPlayer.setOnCompletionListener(mCompletionListener);
                mMediaPlayer.setOnErrorListener(mErrorListener);
                mMediaPlayer.setOnInfoListener(mOnInfoListener);
                mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
                mCurrentBufferPercentage = 0;
                try {
                    Method m = MediaPlayer.class.getMethod("setDataSource", Context.class, Uri.class, Map.class);
                    m.setAccessible(true);
                    m.invoke(mMediaPlayer, getContext(), mUri, mHeaders);
                } catch (Exception e) {
                    mMediaPlayer.setDataSource(getContext(), mUri);
                }
                mMediaPlayer.setDisplay(mSurfaceHolder);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setScreenOnWhilePlaying(true);
                mMediaPlayer.prepareAsync();
                // we don't set the target state here either, but preserve the
                // target state that was there before.
                mCurrentState = STATE_PREPARING;
                attachMediaController();
            } catch (IOException ex) {
                Log.w(TAG, "Unable to open content: " + mUri, ex);
                mCurrentState = STATE_ERROR;
                mTargetState = STATE_ERROR;
                mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
                return;
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "Unable to open content: " + mUri, ex);
                mCurrentState = STATE_ERROR;
                mTargetState = STATE_ERROR;
                mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
                return;
            }
        }

        public void setMediaController(MediaController controller ,LinearLayout ll_title) {
            if (mMediaController != null) {
                mMediaController.hide();
                ll_title.setVisibility(GONE);
            }
            mMediaController = controller;
            this.ll_title=ll_title;
            attachMediaController();
        }

        private void attachMediaController() {
            if (mMediaPlayer != null && mMediaController != null) {
                mMediaController.setMediaPlayer(this);
                View anchorView = this.getParent() instanceof View ?
                        (View)this.getParent() : this;
                mMediaController.setAnchorView(anchorView);
                mMediaController.setEnabled(isInPlaybackState());
            }
        }

        MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
                new MediaPlayer.OnVideoSizeChangedListener() {
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        mVideoWidth = mp.getVideoWidth();
                        mVideoHeight = mp.getVideoHeight();
                        if (mVideoWidth != 0 && mVideoHeight != 0) {
                            getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                            requestLayout();
                        }
                    }
                };

        MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                mCurrentState = STATE_PREPARED;

                mCanPause = mCanSeekBack = mCanSeekForward = true;

                if (mOnPreparedListener != null) {
                    mOnPreparedListener.onPrepared(mMediaPlayer);
                }
                if (mMediaController != null) {
                    mMediaController.setEnabled(true);
                }

                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();

                int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
                if (seekToPosition != 0) {
                    seekTo(seekToPosition);

                }
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    //Log.i("@@@@", "video size: " + mVideoWidth +"/"+ mVideoHeight);
                    getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                    if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                        // We didn't actually change the size (it was already at the size
                        // we need), so we won't get a "surface changed" callback, so
                        // start the video here instead of in the callback.
                        if (mTargetState == STATE_PLAYING) {
                            start();
                            if (mMediaController != null) {
                                mMediaController.show();
                                ll_title.setVisibility(VISIBLE);
                            }
                        } else if (!isPlaying() &&
                                (seekToPosition != 0 || getCurrentPosition() > 0)) {
                            if (mMediaController != null) {
                                // Show the media controls when we're paused into a video and make 'em stick.
                                mMediaController.show(0);
                            }
                        }
                    }
                } else {
                    // We don't know the video size yet, but should start anyway.
                    // The video size might be reported to us later.
                    if (mTargetState == STATE_PLAYING) {
                        start();
                    }
                }
            }
        };

        private MediaPlayer.OnCompletionListener mCompletionListener =
                new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        mCurrentState = STATE_PLAYBACK_COMPLETED;
                        mTargetState = STATE_PLAYBACK_COMPLETED;
                        if (mMediaController != null) {
                            mMediaController.hide();
                            ll_title.setVisibility(GONE);
                        }
                        if (mOnCompletionListener != null) {
                            mOnCompletionListener.onCompletion(mMediaPlayer);
                        }
                    }
                };

        private MediaPlayer.OnErrorListener mErrorListener =
                new MediaPlayer.OnErrorListener() {
                    public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
                        Log.d(TAG, "Error: " + framework_err + "," + impl_err);
                        mCurrentState = STATE_ERROR;
                        mTargetState = STATE_ERROR;
                        if (mMediaController != null) {
                            mMediaController.hide();
                            ll_title.setVisibility(GONE);
                        }

                /* If an error handler has been supplied, use it and finish. */
                        if (mOnErrorListener != null) {
                            if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                                return true;
                            }
                        }

                        return true;
                    }
                };

        private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
                new MediaPlayer.OnBufferingUpdateListener() {
                    public void onBufferingUpdate(MediaPlayer mp, int percent) {
                        mCurrentBufferPercentage = percent;
                    }
                };

        /**
         * Register a callback to be invoked when the media file
         * is loaded and ready to go.
         *
         * @param l The callback that will be run
         */
        public void setOnPreparedListener(MediaPlayer.OnPreparedListener l)
        {
            mOnPreparedListener = l;
        }

        /**
         * Register a callback to be invoked when the end of a media file
         * has been reached during playback.
         *
         * @param l The callback that will be run
         */
        public void setOnCompletionListener(MediaPlayer.OnCompletionListener l)
        {
            mOnCompletionListener = l;
        }

        /**
         * Register a callback to be invoked when an error occurs
         * during playback or setup.  If no listener is specified,
         * or if the listener returned false, VideoView will inform
         * the user of any errors.
         *
         * @param l The callback that will be run
         */
        public void setOnErrorListener(MediaPlayer.OnErrorListener l)
        {
            mOnErrorListener = l;
        }

        /**
         * Register a callback to be invoked when an informational event
         * occurs during playback or setup.
         *
         * @param l The callback that will be run
         */
        public void setOnInfoListener(MediaPlayer.OnInfoListener l) {
            mOnInfoListener = l;
        }

        SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback()
        {
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int w, int h)
            {
                mSurfaceWidth = w;
                mSurfaceHeight = h;
                boolean isValidState =  (mTargetState == STATE_PLAYING);
                boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
                if (mMediaPlayer != null && isValidState && hasValidSize) {
                    if (mSeekWhenPrepared != 0) {
                        seekTo(mSeekWhenPrepared);
                    }
                    start();
                }
            }

            public void surfaceCreated(SurfaceHolder holder)
            {
                mSurfaceHolder = holder;
                openVideo();
            }

            public void surfaceDestroyed(SurfaceHolder holder)
            {
                // after we return from this we can't use the surface any more
                mSurfaceHolder = null;
                if (mMediaController != null) mMediaController.hide();
                release(true);
            }
        };

        /*
         * release the media player in any state
         */
        private void release(boolean cleartargetstate) {
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
                mCurrentState = STATE_IDLE;
                if (cleartargetstate) {
                    mTargetState  = STATE_IDLE;
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (isInPlaybackState() && mMediaController != null) {
                toggleMediaControlsVisiblity();
            }
            return false;
        }

        @Override
        public boolean onTrackballEvent(MotionEvent ev) {
            if (isInPlaybackState() && mMediaController != null) {
                toggleMediaControlsVisiblity();
            }
            return false;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event)
        {
            boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                    keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                    keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                    keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                    keyCode != KeyEvent.KEYCODE_MENU &&
                    keyCode != KeyEvent.KEYCODE_CALL &&
                    keyCode != KeyEvent.KEYCODE_ENDCALL;
            if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
                if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    if (mMediaPlayer.isPlaying()) {
                        pause();
                        mMediaController.show();
                        ll_title.setVisibility(VISIBLE);
                    } else {
                        start();
                        mMediaController.hide();
                        ll_title.setVisibility(GONE);
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                    if (!mMediaPlayer.isPlaying()) {
                        start();
                        mMediaController.hide();
                        ll_title.setVisibility(GONE);
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                        || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    if (mMediaPlayer.isPlaying()) {
                        pause();
                        mMediaController.show();
                        ll_title.setVisibility(VISIBLE);
                    }
                    return true;
                } else {
                    toggleMediaControlsVisiblity();
                }
            }

            return super.onKeyDown(keyCode, event);
        }

        private void toggleMediaControlsVisiblity() {
            if (mMediaController.isShowing()) {
                mMediaController.hide();
                ll_title.setVisibility(GONE);
            } else {
                mMediaController.show();
                ll_title.setVisibility(VISIBLE);
            }
        }

        @Override
        public void start() {
            if (isInPlaybackState()) {
                mMediaPlayer.start();
                mCurrentState = STATE_PLAYING;
            }
            mTargetState = STATE_PLAYING;
        }

        @Override
        public void pause() {
            if (isInPlaybackState()) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    mCurrentState = STATE_PAUSED;
                }
            }
            mTargetState = STATE_PAUSED;
        }

        public void suspend() {
            release(false);
        }

        public void resume() {
            openVideo();
        }

        @Override
        public int getDuration() {
            if (isInPlaybackState()) {
                return mMediaPlayer.getDuration();
            }

            return -1;
        }

        @Override
        public int getCurrentPosition() {
            if (isInPlaybackState()) {
                return mMediaPlayer.getCurrentPosition();
            }
            return 0;
        }

        @Override
        public void seekTo(int msec) {
            if (isInPlaybackState()) {
                mMediaPlayer.seekTo(msec);
                mSeekWhenPrepared = 0;
            } else {
                mSeekWhenPrepared = msec;
            }
        }

        @Override
        public boolean isPlaying() {
            return isInPlaybackState() && mMediaPlayer.isPlaying();
        }

        @Override
        public int getBufferPercentage() {
            if (mMediaPlayer != null) {
                return mCurrentBufferPercentage;
            }
            return 0;
        }

        private boolean isInPlaybackState() {
            return (mMediaPlayer != null &&
                    mCurrentState != STATE_ERROR &&
                    mCurrentState != STATE_IDLE &&
                    mCurrentState != STATE_PREPARING);
        }

        @Override
        public boolean canPause() {
            return mCanPause;
        }

        @Override
        public boolean canSeekBackward() {
            return mCanSeekBack;
        }

        @Override
        public boolean canSeekForward() {
            return mCanSeekForward;
        }

        @Override
        public int getAudioSessionId() {
            if (mAudioSession == 0) {
                MediaPlayer foo = new MediaPlayer();
                mAudioSession = foo.getAudioSessionId();
                foo.release();
            }
            return mAudioSession;
        }
    }

    public static class MyCheckBox extends AppCompatActivity {

        private CheckBox checkBox;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_my_check_box);

            checkBox = (CheckBox) findViewById(R.id.checkBox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    System.out.println("===isChecked==="+isChecked);
                }
            });

            checkBox.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CompoundButton) v).isChecked();
                    System.out.println("====v==="+checked);
                }
            });

            findViewById(R.id.button).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checkBox.isChecked()){
                        checkBox.setChecked(false);
                    }else {
                        checkBox.setChecked(true);
                    }
                }
            });
        }
    }

    /**
     * Created by chuangguo.qi on 2017/5/10.
     */

    public static class CustomCircleProgress extends ProgressBar {

        private int mDefaultColor;
        private int PROGRESS_DEFAULT_COLOR= Color.parseColor("#005EB8");
        private int PROGRESS_REACHED_COLOR=Color.parseColor("#D9DFE3");
        private int mReachedColor;
        private int mDefaultHeight=5;
        private int mReachedHeight=5;
        private int mRadius=300;
        private Paint mPaint;
        private Status status= Status.PUSE;
        private Path mPath;
        private float triangleLength;
        private Path p;
        private PathMeasure mPathMeasure;
        private ValueAnimator mAnimator;
        private float fraction = 0;
        private boolean isStart=false;
        private Paint mPaint02;

        private enum Status{

            START,PUSE;
        }

        public CustomCircleProgress(Context context) {
            this(context,null);
        }

        public CustomCircleProgress(Context context, AttributeSet attrs) {
            this(context, attrs,0);
        }

        public CustomCircleProgress(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);

            //获取自定义属性的值
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.CustomCircleProgress);
           //默认圆的颜色
            mDefaultColor = typedArray.getColor(R.styleable.CustomCircleProgress_progress_default_color, PROGRESS_DEFAULT_COLOR);
            //进度条的颜色
            mReachedColor = typedArray.getColor(R.styleable.CustomCircleProgress_progress_reached_color, PROGRESS_REACHED_COLOR);
            //默认圆的高度
            mDefaultHeight = (int) typedArray.getDimension(R.styleable.CustomCircleProgress_progress_default_height, mDefaultHeight);
            //进度条的高度
            mReachedHeight = (int) typedArray.getDimension(R.styleable.CustomCircleProgress_progress_reached_height, mReachedHeight);
            //圆的半径
            mRadius = (int) typedArray.getDimension(R.styleable.CustomCircleProgress_circle_radius, mRadius);

            typedArray.recycle();

            setPaint();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {


            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            int paintHeight = Math.max(mReachedHeight, mDefaultHeight);//比较两数，取最大值

            if(heightMode != MeasureSpec.EXACTLY){
                //如果用户没有精确指出宽高时，我们就要测量整个View所需要分配的高度了，测量自定义圆形View设置的上下内边距+圆形view的直径+圆形描边边框的高度
                int exceptHeight = getPaddingTop() + getPaddingBottom() + mRadius*2 + paintHeight;
                //然后再将测量后的值作为精确值传给父类，告诉他我需要这么大的空间，你给我分配吧
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(exceptHeight, MeasureSpec.EXACTLY);
            }
            if(widthMode != MeasureSpec.EXACTLY){
                //这里在自定义属性中没有设置圆形边框的宽度，所以这里直接用高度代替
                int exceptWidth = getPaddingLeft() + getPaddingRight() + mRadius*2 + paintHeight;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(exceptWidth, MeasureSpec.EXACTLY);
            }

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        private void setPaint() {

            mPaint = new Paint();
            //下面是设置画笔的一些属性
            mPaint.setAntiAlias(true);//抗锯齿
            mPaint.setDither(true);//防抖动，绘制出来的图要更加柔和清晰
            mPaint.setStyle(Paint.Style.STROKE);//设置填充样式
            mPaint.setStrokeCap(Paint.Cap.ROUND);//设置画笔笔刷类型

            mPaint02 = new Paint();
            //下面是设置画笔的一些属性
            mPaint02.setAntiAlias(true);//抗锯齿
            mPaint02.setDither(true);//防抖动，绘制出来的图要更加柔和清晰
            mPaint02.setStyle(Paint.Style.STROKE);//设置填充样式
            mPaint02.setStrokeCap(Paint.Cap.ROUND);//设置画笔笔刷类型

            //通过path路径绘制三角形
            mPath = new Path();
            //让三角形的长度等于圆的半径(等边三角形)
            triangleLength = mRadius;
            //绘制三角形，首先我们需要确定三个点的坐标
            float firstX = (float) ((mRadius*2 - Math.sqrt(3.0) / 2 * mRadius) / 2);//左上角第一个点的横坐标，根据勾三股四弦五定律,Math.sqrt(3.0)表示开方
            //为了显示的好看些，这里微调下firstX横坐标
            float mFirstX = (float)(firstX + firstX*0.2);
            float firstY = mRadius - triangleLength / 2;
            //同理，依次可得出第二个点(左下角)第三个点的坐标
            float secondX = mFirstX;
            float secondY = (float) (mRadius + triangleLength / 2);
            float thirdX = (float) (mFirstX + Math.sqrt(3.0) / 2 * mRadius);
            float thirdY =  mRadius;
            mPath.moveTo(mFirstX,firstY);
            mPath.lineTo(secondX,secondY);
            mPath.lineTo(thirdX,thirdY);
            mPath.lineTo(mFirstX,firstY);

            p = new Path();
            p.moveTo(mRadius/2,mRadius);
            p.lineTo(mRadius-mRadius*1/9,mRadius+mRadius/3);
            p.lineTo(mRadius+mRadius*2/3,mRadius/2);
            //p.close();

            mPathMeasure = new PathMeasure(p, false);
            final float length = mPathMeasure.getLength();
            mAnimator = ValueAnimator.ofFloat(1, 0);
            mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mAnimator.setDuration(200);
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    fraction = (float) valueAnimator.getAnimatedValue();
                    DashPathEffect mEffect =  new DashPathEffect(new float[]{length, length}, fraction * length);
                    mPaint02.setPathEffect(mEffect);
                    invalidate();
                }
            });

        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.save();
            //canvas.translate(getPaddingLeft(),getPaddingTop());
            onDrawButton(canvas);
            onDrawProgress(canvas);


        }

        private void onDrawButton(Canvas canvas){

            if(status == Status.PUSE){//未开始状态，画笔填充三角形
                setProgress(0);
                mPaint.setStyle(Paint.Style.FILL);
                /*//设置颜色
                mPaint.setColor(Color.parseColor("#01A1EB"));
                //画三角形
                canvas.drawPath(mPath,mPaint);*/

                canvas.drawLine(mRadius,mRadius-mRadius/3,mRadius,mRadius+mRadius/3,mPaint);
                canvas.drawLine(mRadius,mRadius+mRadius/3,mRadius-mRadius/3,mRadius,mPaint);
                canvas.drawLine(mRadius,mRadius+mRadius/3,mRadius+mRadius/3,mRadius,mPaint);


            }else{//正在进行状态,画两条竖线
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mPaint.setStrokeWidth(dp2px(getContext(),5));
                mPaint.setColor(Color.parseColor("#005EB8"));
                if (getProgress()<=98) {
                    canvas.drawRect(mRadius - mRadius / 3, mRadius - mRadius / 3, mRadius + mRadius / 3, mRadius + mRadius / 3, mPaint);
                }
            }
        }

        private void onDrawProgress(Canvas canvas){

            //为了保证最外层的圆弧全部显示，我们通常会设置自定义view的padding属性，这样就有了内边距，所以画笔应该平移到内边距的位置，这样画笔才会刚好在最外层的圆弧上
            //画笔平移到指定paddingLeft， getPaddingTop()位置

            mPaint.setStyle(Paint.Style.STROKE);
            //画默认圆(边框)的一些设置
            mPaint.setColor(mDefaultColor);
            mPaint.setStrokeWidth(mDefaultHeight);
            canvas.drawCircle(mRadius,mRadius,mRadius,mPaint);

            //画进度条的一些设置
            mPaint.setColor(mReachedColor);
            mPaint.setStrokeWidth(mReachedHeight);
            //根据进度绘制圆弧
            float sweepAngle = getProgress() * 1.0f / getMax() * 360;
            canvas.drawArc(new RectF(0, 0,mRadius * 2,mRadius *2), 0, sweepAngle, false, mPaint);//drawArc：绘制圆弧
            canvas.restore();

            if (getProgress()>=100){
                if (!isStart){
                    mAnimator.start();
                    isStart=true;
                }

                loadDoneDraw(canvas);
            }

        }

        public void loadDoneDraw(Canvas canvas){
            mPaint02.setStrokeWidth(20);
            //canvas.drawLine(mRadius/2,mRadius,mRadius-mRadius*1/9,mRadius+mRadius/3,mPaint);
            //canvas.drawLine(mRadius-mRadius*1/9,mRadius+mRadius/3,mRadius+mRadius*2/3,mRadius/2,mPaint);
            canvas.drawPath(p,mPaint02);


        }

        public void setStatus(){

            if (status== Status.PUSE){

                status= Status.START;
            }else {

                status= Status.PUSE;
            }

            invalidate();
        }


        public static int dp2px(Context context, int dpValue) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    dpValue, context.getResources().getDisplayMetrics());
        }
    }
}
