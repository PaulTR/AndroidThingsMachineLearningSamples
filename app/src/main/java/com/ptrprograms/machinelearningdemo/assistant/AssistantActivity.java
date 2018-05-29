package com.ptrprograms.machinelearningdemo.assistant;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.assistant.embedded.v1alpha2.AssistConfig;
import com.google.assistant.embedded.v1alpha2.AssistRequest;
import com.google.assistant.embedded.v1alpha2.AssistResponse;
import com.google.assistant.embedded.v1alpha2.AudioInConfig;
import com.google.assistant.embedded.v1alpha2.AudioOutConfig;
import com.google.assistant.embedded.v1alpha2.DeviceConfig;
import com.google.assistant.embedded.v1alpha2.DialogStateIn;
import com.google.assistant.embedded.v1alpha2.EmbeddedAssistantGrpc;
import com.google.protobuf.ByteString;
import com.ptrprograms.machinelearningdemo.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.stub.StreamObserver;

//TODO cleanup and kotlin-ize
//TODO save volume in shared preference to persist across device restart
public class AssistantActivity extends Activity implements Button.OnButtonEventListener {
    private static final String TAG = AssistantActivity.class.getSimpleName();

    private static int mVolumePercentage = 100;

    // Peripheral and drivers constants.
    private static final int BUTTON_DEBOUNCE_DELAY_MS = 20;

    // Audio constants.
    private static final int SAMPLE_RATE = 16000;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static AudioInConfig.Encoding ENCODING_INPUT = AudioInConfig.Encoding.LINEAR16;
    private static AudioOutConfig.Encoding ENCODING_OUTPUT = AudioOutConfig.Encoding.LINEAR16;
    private static final AudioInConfig ASSISTANT_AUDIO_REQUEST_CONFIG =
            AudioInConfig.newBuilder()
                    .setEncoding(ENCODING_INPUT)
                    .setSampleRateHertz(SAMPLE_RATE)
                    .build();

    private static final AudioOutConfig ASSISTANT_AUDIO_RESPONSE_CONFIG =
            AudioOutConfig.newBuilder()
                    .setEncoding(ENCODING_OUTPUT)
                    .setSampleRateHertz(SAMPLE_RATE)
                    .setVolumePercentage(mVolumePercentage)
                    .build();
    private static final AudioFormat AUDIO_FORMAT_STEREO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private static final AudioFormat AUDIO_FORMAT_OUT_MONO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private static final AudioFormat AUDIO_FORMAT_IN_MONO =
            new AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .build();
    private static final int SAMPLE_BLOCK_SIZE = 1024;
    private int mOutputBufferSize;

    // Google Assistant API constants.
    private static final String ASSISTANT_ENDPOINT = "embeddedassistant.googleapis.com";

    // gRPC client and stream observers.
    private EmbeddedAssistantGrpc.EmbeddedAssistantStub mAssistantService;
    private StreamObserver<AssistRequest> mAssistantRequestObserver;
    private StreamObserver<AssistResponse> mAssistantResponseObserver =
            new StreamObserver<AssistResponse>() {
                @Override
                public void onNext(AssistResponse value) {
                    if (value.getEventType() != null) {
                        Log.e(TAG, "converse response event: " + value.getEventType());
                    }

                    if (value.getDialogStateOut() != null) {
                        mConversationState = value.getDialogStateOut().getConversationState();
                    }

                    if (value.getDialogStateOut() != null) {
                        int volume = value.getDialogStateOut().getVolumePercentage();
                        if (volume > 0) {
                            mVolumePercentage = volume;
                            Log.e(TAG, "assistant volume changed: " + mVolumePercentage);
                            mAudioTrack.setVolume(AudioTrack.getMaxVolume() * (mVolumePercentage / 100.0f));
                        }
                    }

                    if (value.getAudioOut() != null) {
                        final ByteBuffer audioData =
                                ByteBuffer.wrap(value.getAudioOut().getAudioData().toByteArray());
                        Log.e(TAG, "converse audio size: " + audioData.remaining());
                        mAssistantResponses.add(audioData);
                    }

                    if (value.getDeviceAction() != null &&
                            !value.getDeviceAction().getDeviceRequestJson().isEmpty()) {
                        // Iterate through JSON object. This sucks, do it better with GSON for later use
                        try {
                            JSONObject deviceAction =
                                    new JSONObject(value.getDeviceAction().getDeviceRequestJson());
                            Log.e("Test", "json: " + deviceAction.toString());
                            JSONArray inputs = deviceAction.getJSONArray("inputs");
                            for (int i = 0; i < inputs.length(); i++) {
                                if (inputs.getJSONObject(i).getString("intent")
                                        .equals("action.devices.EXECUTE")) {
                                    JSONArray commands = inputs.getJSONObject(i)
                                            .getJSONObject("payload")
                                            .getJSONArray("commands");
                                    for (int j = 0; j < commands.length(); j++) {
                                        JSONArray execution = commands.getJSONObject(j)
                                                .getJSONArray("execution");
                                        for (int k = 0; k < execution.length(); k++) {
                                            String command = execution.getJSONObject(k)
                                                    .getString("command");
                                            JSONObject params = execution.getJSONObject(k)
                                                    .optJSONObject("params");
                                            handleDeviceAction(command, params);
                                        }
                                    }
                                }
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "converse error:", t);
                }

                @Override
                public void onCompleted() {
                    mAudioTrack = new AudioTrack.Builder()
                            .setAudioFormat(AUDIO_FORMAT_OUT_MONO)
                            .setBufferSizeInBytes(mOutputBufferSize)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build();

                    mAudioTrack.play();

                    for (ByteBuffer audioData : mAssistantResponses) {
                        final ByteBuffer buf = audioData;
                        Log.e(TAG, "Playing a bit of audio");
                        mAudioTrack.write(buf, buf.remaining(),
                                AudioTrack.WRITE_BLOCKING);
                    }
                    mAssistantResponses.clear();
                    mAudioTrack.stop();

                    Log.e(TAG, "assistant response finished");
                }
            };

    // Audio playback and recording objects.
    private AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord;


    // Hardware peripherals.
    private Button mButton;
    private Button mBackButton;
    private Apa102 mLEDStrip;

    private int[] mRainbow = new int[7];
    private int[] mLightsOff = new int[7];

    // Assistant Thread and Runnables implementing the push-to-talk functionality.
    private ByteString mConversationState = null;
    private HandlerThread mAssistantThread;
    private Handler mAssistantHandler;
    private ArrayList<ByteBuffer> mAssistantResponses = new ArrayList<>();
    private Runnable mStartAssistantRequest = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "starting assistant request");
            mAudioRecord.startRecording();
            mAssistantRequestObserver = mAssistantService.assist(mAssistantResponseObserver);
            AssistConfig.Builder converseConfigBuilder = AssistConfig.newBuilder()
                    .setAudioInConfig(ASSISTANT_AUDIO_REQUEST_CONFIG)
                    .setAudioOutConfig(ASSISTANT_AUDIO_RESPONSE_CONFIG)
                    .setDeviceConfig(DeviceConfig.newBuilder()
                            .setDeviceModelId(MyDevice.MODEL_ID)
                            .setDeviceId(MyDevice.INSTANCE_ID)
                            .build());
            DialogStateIn.Builder dialogStateInBuilder = DialogStateIn.newBuilder()
                    .setLanguageCode(MyDevice.LANGUAGE_CODE);
            if (mConversationState != null) {
                dialogStateInBuilder.setConversationState(mConversationState);
            }
            converseConfigBuilder.setDialogStateIn(dialogStateInBuilder.build());
            mAssistantRequestObserver.onNext(
                    AssistRequest.newBuilder()
                            .setConfig(converseConfigBuilder.build())
                            .build());
            mAssistantHandler.post(mStreamAssistantRequest);
        }
    };

    private Runnable mStreamAssistantRequest = new Runnable() {
        @Override
        public void run() {
            ByteBuffer audioData = ByteBuffer.allocateDirect(SAMPLE_BLOCK_SIZE);

            int result =
                    mAudioRecord.read(audioData, audioData.capacity(), AudioRecord.READ_BLOCKING);
            if (result < 0) {
                Log.e(TAG, "error reading from audio stream:" + result);
                return;
            }
            Log.e(TAG, "streaming ConverseRequest: " + result);
            mAssistantRequestObserver.onNext(AssistRequest.newBuilder()
                    .setAudioIn(ByteString.copyFrom(audioData))
                    .build());
            mAssistantHandler.post(mStreamAssistantRequest);
        }
    };

    private Runnable mStopAssistantRequest = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "ending assistant request");
            mAssistantHandler.removeCallbacks(mStreamAssistantRequest);
            if (mAssistantRequestObserver != null) {
                mAssistantRequestObserver.onCompleted();
                mAssistantRequestObserver = null;
            }
            mAudioRecord.stop();
            mAudioTrack.play();
        }
    };

    private Handler mMainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "starting assistant demo");

        mMainHandler = new Handler(getMainLooper());

        mAssistantThread = new HandlerThread("assistantThread");
        mAssistantThread.start();
        mAssistantHandler = new Handler(mAssistantThread.getLooper());

        try {
            mButton = RainbowHat.openButtonA();
            mButton.setDebounceDelay(BUTTON_DEBOUNCE_DELAY_MS);
            mButton.setOnButtonEventListener(this);

            mBackButton = RainbowHat.openButtonC();
            mBackButton.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    cleanup();
                    finish();
                }
            });

            mLEDStrip = RainbowHat.openLedStrip();
            mLEDStrip.setBrightness(1);

            mRainbow[0] = 0xFF0000;
            mRainbow[1] = 0xFFAB00;
            mRainbow[2] = 0xFFFF00;
            mRainbow[3] = 0x00FF00;
            mRainbow[4] = 0x0000FF;
            mRainbow[5] = 0x9933FF;
            mRainbow[6] = 0x990099;

            mLightsOff[0] = 0x000000;
            mLightsOff[1] = 0x000000;
            mLightsOff[2] = 0x000000;
            mLightsOff[3] = 0x000000;
            mLightsOff[4] = 0x000000;
            mLightsOff[5] = 0x000000;
            mLightsOff[6] = 0x000000;

        } catch( IOException e ) {
            return;
        }

        AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.e(TAG, "setting volume to: " + maxVolume);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        mOutputBufferSize = AudioTrack.getMinBufferSize(AUDIO_FORMAT_OUT_MONO.getSampleRate(),
                AUDIO_FORMAT_OUT_MONO.getChannelMask(),
                AUDIO_FORMAT_OUT_MONO.getEncoding());
        mAudioTrack = new AudioTrack.Builder()
                .setAudioFormat(AUDIO_FORMAT_OUT_MONO)
                .setBufferSizeInBytes(mOutputBufferSize)
                .build();
        mAudioTrack.play();
        int inputBufferSize = AudioRecord.getMinBufferSize(AUDIO_FORMAT_STEREO.getSampleRate(),
                AUDIO_FORMAT_STEREO.getChannelMask(),
                AUDIO_FORMAT_STEREO.getEncoding());
        mAudioRecord = new AudioRecord.Builder()
                .setAudioSource(AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(AUDIO_FORMAT_IN_MONO)
                .setBufferSizeInBytes(inputBufferSize)
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(ASSISTANT_ENDPOINT).build();
        try {
            mAssistantService = EmbeddedAssistantGrpc.newStub(channel)
                    .withCallCredentials(MoreCallCredentials.from(
                            Credentials.fromResource(this, R.raw.credentials)
                    ));
        } catch (IOException|JSONException e) {
            Log.e(TAG, "error creating assistant service:", e);
        }
    }

    @Override
    public void onButtonEvent(Button button, boolean pressed) {
        Log.e("Test", "onbuttonevent: " + pressed);
        if (pressed) {
            mAssistantHandler.post(mStartAssistantRequest);
        } else {
            mAssistantHandler.post(mStopAssistantRequest);
        }
    }

    public void handleDeviceAction(String command, JSONObject params)
            throws JSONException, IOException {
        if (command.equals("action.devices.commands.OnOff")) {
            if( params.get("on").equals(false) ) {
                Log.e("Test", "turn off");
                mLEDStrip.write(mLightsOff);
            } else {
                Log.e("Test", "turn on");
                mLEDStrip.write(mRainbow);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "destroying assistant demo");
        cleanup();
    }

    private void cleanup() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord = null;
        }
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }

        if (mButton != null) {
            try {
                mButton.close();
            } catch (IOException e) {
                Log.w(TAG, "error closing button", e);
            }
            mButton = null;
        }

        mAssistantHandler.post(() -> mAssistantHandler.removeCallbacks(mStreamAssistantRequest));
        mAssistantThread.quitSafely();

        if( mBackButton != null ) {
            try {
                mBackButton.close();
            } catch( IOException e ) {

            }
        }

        if( mLEDStrip != null ) {
            try {
                mLEDStrip.close();
            } catch( IOException e ) {

            }
        }
    }
}