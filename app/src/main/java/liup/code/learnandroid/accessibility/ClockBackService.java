package liup.code.learnandroid.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

import liup.code.learnandroid.R;

/**
 * @title
 *  提供自定义反馈的{@link AccessibilityService}
 *  用于Android设备默认使用的时钟应用程序。 它演示了Android可访问性API的以下关键特性：
 *
 *      1.简单演示如何使用可访问性API。
 *
 *      2.亲身体验使用可访问性API提供备选和补充反馈的各种方法。
 *
 *      3.提供应用程序的具体反馈; 该服务仅处理来自时钟应用程序的可访问事件。
 *
 *      4.提供动态的，与上下文相关的反馈信息; 反馈类型根据铃声状态而改变。
 *
 * @desc .
 * Created by liupeng on 2018/3/28.
 */

public class ClockBackService extends AccessibilityService {

    /** service登陆的tag. */
    private static final String LOG_TAG = "ClockBackService";

    /** 我们想要接收的可访问性事件之间的最小超时 */
    private static final int EVENT_NOTIFICATION_TIMEOUT_MILLIS = 80;

    /** 这适用于包名称在不同版本中更改的AlarmClock和Clock */
    private static final String[] PACKAGE_NAMES = new String[] {
            "com.android.alarmclock", "com.google.android.deskclock", "com.android.deskclock"
    };

    // 我们传递的消息类型。

    /** 语音. */
    private static final int MESSAGE_SPEAK = 1;

    /** 停止语音. */
    private static final int MESSAGE_STOP_SPEAK = 2;

    /** 启动TTS（文本到语音服务）. */
    private static final int MESSAGE_START_TTS = 3;

    /** 停止TTS（文本到语音服务）. */
    private static final int MESSAGE_SHUTDOWN_TTS = 4;

    /** 用耳机播放. */
    private static final int MESSAGE_PLAY_EARCON = 5;

    /** 停止用耳机播放. */
    private static final int MESSAGE_STOP_PLAY_EARCON = 6;

    /** 启动振动模式. */
    private static final int MESSAGE_VIBRATE = 7;

    /** 停止振动模式. */
    private static final int MESSAGE_STOP_VIBRATE = 8;

    //屏幕状态广播相关常量。

    /**  打开屏幕广播的关键字. */
    private static final int INDEX_SCREEN_ON = 0x00000100;

    /**  关闭屏幕广播的关键字. */
    private static final int INDEX_SCREEN_OFF = 0x00000200;

    //振铃模式改变相关常数。

    /** 正常铃声模式key. */
    private static final int INDEX_RINGER_NORMAL = 0x00000400;

    /** 振动铃声模式key. */
    private static final int INDEX_RINGER_VIBRATE = 0x00000800;

    /** 无声铃声模式key. */
    private static final int INDEX_RINGER_SILENT = 0x00001000;

    //与语音相关的常量。

    /**
     * 排队模式 - 有新语音时，停止之前的语音，然后开始播放语音
     */
    private static final int QUEUING_MODE_INTERRUPT = 2;

    /** 空格字符串常量. */
    private static final String SPACE = " ";

    /** 震动模式map. */
    private static final SparseArray<long[]> sVibrationPatterns = new SparseArray<long[]>();
    static {
        sVibrationPatterns.put(AccessibilityEvent.TYPE_VIEW_CLICKED, new long[] {//点击
                0L, 100L
        });
        sVibrationPatterns.put(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED, new long[] {//长按
                0L, 100L
        });
        sVibrationPatterns.put(AccessibilityEvent.TYPE_VIEW_SELECTED, new long[] {//选择
                0L, 15L, 10L, 15L
        });
        sVibrationPatterns.put(AccessibilityEvent.TYPE_VIEW_FOCUSED, new long[] {//输入焦点事件
                0L, 15L, 10L, 15L
        });
        sVibrationPatterns.put(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, new long[] {//PopupWindow、Menu、Dialog的打开事件
                0L, 25L, 50L, 25L, 50L, 25L
        });
        sVibrationPatterns.put(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER, new long[] {//view的悬停事件
                0L, 15L, 10L, 15L, 15L, 10L
        });
        sVibrationPatterns.put(INDEX_SCREEN_ON, new long[] {//打开屏幕广播
                0L, 10L, 10L, 20L, 20L, 30L
        });
        sVibrationPatterns.put(INDEX_SCREEN_OFF, new long[] {//关闭屏幕广播
                0L, 30L, 20L, 20L, 10L, 10L
        });
    }

    /**  raw声音资源id map */
    private static SparseArray<Integer> sSoundsResourceIds = new SparseArray<Integer>();
    static {
        sSoundsResourceIds.put(AccessibilityEvent.TYPE_VIEW_CLICKED,
                R.raw.sound_view_clicked);
        sSoundsResourceIds.put(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                R.raw.sound_view_clicked);
        sSoundsResourceIds.put(AccessibilityEvent.TYPE_VIEW_SELECTED,
                R.raw.sound_view_focused_or_selected);
        sSoundsResourceIds.put(AccessibilityEvent.TYPE_VIEW_FOCUSED,
                R.raw.sound_view_focused_or_selected);
        sSoundsResourceIds.put(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                R.raw.sound_window_state_changed);
        sSoundsResourceIds.put(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER,
                R.raw.sound_view_hover_enter);
        sSoundsResourceIds.put(INDEX_SCREEN_ON, R.raw.sound_screen_on);
        sSoundsResourceIds.put(INDEX_SCREEN_OFF, R.raw.sound_screen_off);
        sSoundsResourceIds.put(INDEX_RINGER_SILENT, R.raw.sound_ringer_silent);
        sSoundsResourceIds.put(INDEX_RINGER_VIBRATE, R.raw.sound_ringer_vibrate);
        sSoundsResourceIds.put(INDEX_RINGER_NORMAL, R.raw.sound_ringer_normal);
    }

    //声音集合相关的。

    /** 耳机名字 - 动态设置. */
    private final SparseArray<String> mEarconNames = new SparseArray<String>();

    //辅助字段。

    Context mContext;

    /** 此服务目前提供的反馈. */
    int mProvidedFeedbackType;

    /** 用于构建话语的可重用实例. */
    private final StringBuilder mUtterance = new StringBuilder();

    //反馈提供服务。

    /** 用于说话的{@link TextToSpeech } */
    private TextToSpeech mTts;

    /** {@link AudioManager}检测振铃状态. */
    private AudioManager mAudioManager;

    /** 用于提供触觉反馈的振动器. */
    private Vibrator mVibrator;

    /** 标记infrastructure是否已初始化. */
    private boolean isInfrastructureInitialized;

    /** 用于在服务的主线程上执行消息。. */
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_SPEAK://语音
                    String utterance = (String) message.obj;
                    mTts.speak(utterance, QUEUING_MODE_INTERRUPT, null);
                    return;
                case MESSAGE_STOP_SPEAK://停止语音
                    mTts.stop();
                    return;
                case MESSAGE_START_TTS://文字转语音
                    mTts = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
                        public void onInit(int status) {
                            //在这里注册，因为要添加耳机，TTS必须被初始化并且接收机立即以当前的振铃模式被调用。
                            registerBroadCastReceiver();
                        }
                    });
                    return;
                case MESSAGE_SHUTDOWN_TTS://停止文字转语音
                    mTts.shutdown();
                    return;
                case MESSAGE_PLAY_EARCON://使用耳机
                    int resourceId = message.arg1;
                    playEarcon(resourceId);
                    return;
                case MESSAGE_STOP_PLAY_EARCON://停止使用耳机
                    mTts.stop();
                    return;
                case MESSAGE_VIBRATE://启动振动模式
                    int key = message.arg1;
                    long[] pattern = sVibrationPatterns.get(key);
                    if (pattern != null) {
                        mVibrator.vibrate(pattern, -1);
                    }
                    return;
                case MESSAGE_STOP_VIBRATE://停止振动模式
                    mVibrator.cancel();
                    return;
            }
        }
    };
    /**
     *  用于接收我们的上下文 - 设备状态的更新。
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)) {//粘性广播意图动作，表明振铃模式已经改变。 包括新的铃声模式。
                int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE,//新的铃声模式。
                        AudioManager.RINGER_MODE_NORMAL);//铃声模式可能会发出声音并可能振动。 如果在更换此模式之前的音量可以听到，则会发出声音。 如果振动设置打开，它会振动。
                configureForRingerMode(ringerMode);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                provideScreenStateChangeFeedback(INDEX_SCREEN_ON);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                provideScreenStateChangeFeedback(INDEX_SCREEN_OFF);
            } else {
                Log.w(LOG_TAG, "Registered for but not handling action " + action);
            }
        }
        /**
         * 提供反馈以宣布屏幕状态更改。 这种变化是开启或关闭屏幕。
         *
         * @param feedbackIndex 静态映射反馈资源中反馈的索引。
         */
        private void provideScreenStateChangeFeedback(int feedbackIndex) {
            // 我们根据我们目前提供的反馈采取具体行动。
            switch (mProvidedFeedbackType) {
                case AccessibilityServiceInfo.FEEDBACK_SPOKEN://表示口头反馈
                    String utterance = generateScreenOnOrOffUtternace(feedbackIndex);
                    mHandler.obtainMessage(MESSAGE_SPEAK, utterance).sendToTarget();
                    return;
                case AccessibilityServiceInfo.FEEDBACK_AUDIBLE://表示可听（未说出）反馈
                    mHandler.obtainMessage(MESSAGE_PLAY_EARCON, feedbackIndex, 0).sendToTarget();
                    return;
                case AccessibilityServiceInfo.FEEDBACK_HAPTIC://表示触觉反馈
                    mHandler.obtainMessage(MESSAGE_VIBRATE, feedbackIndex, 0).sendToTarget();
                    return;
                default:
                    throw new IllegalStateException("Unexpected feedback type "
                            + mProvidedFeedbackType);
            }
        }
    };
    /**
     *  无障碍服务的生命周期仅由系统管理，并遵循既定的服务生命周期。
     *  启动无障碍服务是由用户明确地在设备设置中启用服务来触发的。
     *  系统绑定到服务后，它会调用{@link AccessibilityService＃onServiceConnected（）}。
     *  此方法可以被想要执行后绑定设置的客户端重写。
     *
     * */
    @Override
    public void onServiceConnected() {
        if (isInfrastructureInitialized) {
            return;
        }

        mContext = this;

        // 发送消息以启动TTS。
        mHandler.sendEmptyMessage(MESSAGE_START_TTS);

        // 获得振动器服务。
        mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

        // 获取AudioManager并根据当前铃声模式进行配置。
        mAudioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        // 在Froyo，铃声模式的广播接收器在注册时会回到当前状态，但在Eclair中这没有完成，因此我们在此进行了轮询。
        int ringerMode = mAudioManager.getRingerMode();
        configureForRingerMode(ringerMode);

        // 我们现在处于初始状态。
        isInfrastructureInitialized = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (isInfrastructureInitialized) {
            // 停止TTS服务。
            mHandler.sendEmptyMessage(MESSAGE_SHUTDOWN_TTS);

            // 取消注册意向广播接收器。
            if (mBroadcastReceiver != null) {
                unregisterReceiver(mBroadcastReceiver);
            }

            // 我们不再处于初始状态。
            isInfrastructureInitialized = false;
        }
        return false;
    }

    /**
     * 注册电话状态观察广播接收器。
     */
    private void registerBroadCastReceiver() {
        // 用我们感兴趣的广播意图创建一个过滤器。
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        // 注册感兴趣的广播。
        registerReceiver(mBroadcastReceiver, filter, null, null);
    }

    /**
     * 生成用于宣布屏幕和屏幕关闭的话语。
     *
     * @param feedbackIndex 查询反馈值的反馈指数。
     * @return 话语。
     */
    private String generateScreenOnOrOffUtternace(int feedbackIndex) {
        // 获取公布模板。
        int resourceId = (feedbackIndex == INDEX_SCREEN_ON) ? R.string.template_screen_on
                : R.string.template_screen_off;
        String template = mContext.getString(resourceId);

        // 使用铃声百分比格式化模板。
        int currentRingerVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);//AudioManager.STREAM_RING 用于识别电话铃声的音频流量
        int maxRingerVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int volumePercent = (100 / maxRingerVolume) * currentRingerVolume;

        // 让我们围绕五点，这听起来更好。
        int adjustment = volumePercent % 10;
        if (adjustment < 5) {
            volumePercent -= adjustment;
        } else if (adjustment > 5) {
            volumePercent += (10 - adjustment);
        }

        return String.format(template, volumePercent);
    }

    /**
     * 根据振铃模式配置服务。 可能的配置：
     *
     *   1. {@link AudioManager#RINGER_MODE_SILENT}
     *   Goal:     只提供自定义的触觉反馈。
     *   Approach: 通过配置此服务来提供这样的服务来接管触觉反馈。 这样系统就不会调用默认的触觉反馈服务KickBack。
     *             通过配置此服务来提供此类反馈，但不这样做，接管声音和语音反馈。 这样系统就不会调用默认的语音反馈服务TalkBack和默认的语音反馈服务SoundBack。
     *
     *   2. {@link AudioManager#RINGER_MODE_VIBRATE}<br/>
     *   Goal:     提供自定义的可听和默认触觉反馈。
     *   Approach: 接管听觉反馈并提供自定义的反馈。 接管口头反馈，但不提供此类反馈。 让其他服务提供触觉反馈（KickBack）。
     *
     *   3. {@link AudioManager#RINGER_MODE_NORMAL}
     *   Goal:     提供自定义口语，默认听觉和默认触觉反馈。
     *   Approach: 接管口头反馈并提供定制的意见。让其他服务提供声音反馈（SounBack）和触觉反馈（KickBack）。
     *
     * @param ringerMode 设备铃声模式。
     */
    private void configureForRingerMode(int ringerMode) {
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {//不会振动。 （这会覆盖振动设置。）
            // 当铃声静音时，我们只想提供触觉反馈。
            mProvidedFeedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC;//AccessibilityServiceInfo.FEEDBACK_HAPTIC 表示触觉反馈。

            // 接管口头和声音反馈，因此不提供此类反馈。
            setServiceInfo(AccessibilityServiceInfo.FEEDBACK_HAPTIC//表示触觉反馈
                    | AccessibilityServiceInfo.FEEDBACK_SPOKEN//表示口头反馈
                    | AccessibilityServiceInfo.FEEDBACK_AUDIBLE);//表示可听（未说出）反馈

            // 只使用耳机通知振铃器状态更改。
            mHandler.obtainMessage(MESSAGE_PLAY_EARCON, INDEX_RINGER_SILENT, 0).sendToTarget();
        } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {//（这会导致电话铃声总是振动，但是如果设置，通知振动只会振动。）
            // 当振铃器振动时，我们只想提供听觉反馈。
            mProvidedFeedbackType = AccessibilityServiceInfo.FEEDBACK_AUDIBLE;//表示可听（未说出）反馈。

            // 接管口头反馈，以免提供口头反馈。
            setServiceInfo(AccessibilityServiceInfo.FEEDBACK_AUDIBLE
                    | AccessibilityServiceInfo.FEEDBACK_SPOKEN);

            // 只使用耳机通知振铃器状态更改。
            mHandler.obtainMessage(MESSAGE_PLAY_EARCON, INDEX_RINGER_VIBRATE, 0).sendToTarget();
        } else if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            // 当铃声响起时，我们希望提供覆盖默认语音反馈的语音反馈。
            mProvidedFeedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
            setServiceInfo(AccessibilityServiceInfo.FEEDBACK_SPOKEN);

            // 只使用耳机通知振铃器状态更改。
            mHandler.obtainMessage(MESSAGE_PLAY_EARCON, INDEX_RINGER_NORMAL, 0).sendToTarget();
        }
    }

    /**
     * 设置通知系统如何处理这个{@link AccessibilityService}的{@link AccessibilityServiceInfo}。
     *
     * @param feedbackType 此服务将提供的反馈类型。
     * <p>
     *   Note: feedbackType参数是按位或全部
     *   feedback 此服务想提供类型。
     * </p>
     */
    private void setServiceInfo(int feedbackType) {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        // 我们对所有类型的辅助事件感兴趣。
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        // 我们想提供特定类型的反馈。
        info.feedbackType = feedbackType;
        // 我们希望以特定的时间间隔接收事件。
        info.notificationTimeout = EVENT_NOTIFICATION_TIMEOUT_MILLIS;
        // 我们只想接收来自特定包的无障碍事件。
        info.packageNames = PACKAGE_NAMES;
        setServiceInfo(info);
    }

    /** 辅助功能触发时的返回事件 */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(LOG_TAG, mProvidedFeedbackType + " " + event.toString());

        // 我们根据我们目前提供的反馈类型采取行动。
        if (mProvidedFeedbackType == AccessibilityServiceInfo.FEEDBACK_SPOKEN) {
            mHandler.obtainMessage(MESSAGE_SPEAK, formatUtterance(event)).sendToTarget();
        } else if (mProvidedFeedbackType == AccessibilityServiceInfo.FEEDBACK_AUDIBLE) {
            mHandler.obtainMessage(MESSAGE_PLAY_EARCON, event.getEventType(), 0).sendToTarget();
        } else if (mProvidedFeedbackType == AccessibilityServiceInfo.FEEDBACK_HAPTIC) {
            mHandler.obtainMessage(MESSAGE_VIBRATE, event.getEventType(), 0).sendToTarget();
        } else {
            throw new IllegalStateException("Unexpected feedback type " + mProvidedFeedbackType);
        }
    }
    /** 回调中断可访问性反馈。 */
    @Override
    public void onInterrupt() {
        // 我们根据我们目前提供的反馈类型采取行动。
        if (mProvidedFeedbackType == AccessibilityServiceInfo.FEEDBACK_SPOKEN) {
            mHandler.obtainMessage(MESSAGE_STOP_SPEAK).sendToTarget();
        } else if (mProvidedFeedbackType == AccessibilityServiceInfo.FEEDBACK_AUDIBLE) {
            mHandler.obtainMessage(MESSAGE_STOP_PLAY_EARCON).sendToTarget();
        } else if (mProvidedFeedbackType == AccessibilityServiceInfo.FEEDBACK_HAPTIC) {
            mHandler.obtainMessage(MESSAGE_STOP_VIBRATE).sendToTarget();
        } else {
            throw new IllegalStateException("Unexpected feedback type " + mProvidedFeedbackType);
        }
    }

    /**
     * 格式化来自{@link AccessibilityEvent}的话语。
     *
     * @param event 从中发起格式化事件的事件。
     * @return 格式化的话语。
     */
    private String formatUtterance(AccessibilityEvent event) {
        StringBuilder utterance = mUtterance;

        // 在追加格式化文本之前清除话语。
        utterance.setLength(0);

        List<CharSequence> eventText = event.getText();

        // 我们尝试获取事件文本，如果这样的话。
        if (!eventText.isEmpty()) {
            for (CharSequence subText : eventText) {
                // 使01发音为1
                if (subText.charAt(0) =='0') {
                    subText = subText.subSequence(1, subText.length());
                }
                utterance.append(subText);
                utterance.append(SPACE);
            }

            return utterance.toString();
        }

        // 没有事件文本，但我们尝试获取内容描述，这是描述视图（通常与ImageView一起使用）的可选属性。
        CharSequence contentDescription = event.getContentDescription();
        if (contentDescription != null) {
            utterance.append(contentDescription);
            return utterance.toString();
        }

        return utterance.toString();
    }

    /**
     * 用耳机播放给它的声音ID。
     *
     * @param earconId 要播放的声音ID。
     */
    private void playEarcon(int earconId) {
        String earconName = mEarconNames.get(earconId);
        if (earconName == null) {
            // 我们不知道声音ID，因此我们需要加载声音。
            Integer resourceId = sSoundsResourceIds.get(earconId);
            if (resourceId != null) {
                earconName = "[" + earconId + "]";
                mTts.addEarcon(earconName, getPackageName(), resourceId);
                mEarconNames.put(earconId, earconName);
            }
        }

        mTts.playEarcon(earconName, QUEUING_MODE_INTERRUPT, null);
    }
}
