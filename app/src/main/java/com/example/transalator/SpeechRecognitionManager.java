package com.example.transalator;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

import java.util.ArrayList;

public class SpeechRecognitionManager {
    private Context context;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private RecognitionCallback callback;

    public SpeechRecognitionManager(Context context) {
        this.context = context;
        initializeSpeechRecognizer();
    }

    private static final int MAX_RETRY_COUNT = 3;
    private int currentRetryCount = 0;
    
    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "语音识别功能不可用，请确保已安装Google语音识别服务", Toast.LENGTH_LONG).show();
            if (callback != null) {
                callback.onError("语音识别服务不可用");
            }
            return;
        }

        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            if (speechRecognizer == null) {
                handleInitializationError("无法创建语音识别器");
                return;
            }
            setupRecognitionListener();
            setupRecognizerIntent();
        } catch (Exception e) {
            handleInitializationError("初始化语音识别器失败: " + e.getMessage());
        }
    }

    private void handleInitializationError(String errorMessage) {
        if (currentRetryCount < MAX_RETRY_COUNT) {
            currentRetryCount++;
            Toast.makeText(context, "正在重试初始化语音识别器...", Toast.LENGTH_SHORT).show();
            initializeSpeechRecognizer();
        } else {
            currentRetryCount = 0;
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            if (callback != null) {
                callback.onError(errorMessage);
            }
        }
    }

    private void setupRecognizerIntent() {
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
    }

    private void setupRecognitionListener() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                if (callback != null) {
                    callback.onError("请开始说话...");
                }
            }

            @Override
            public void onBeginningOfSpeech() {
                if (callback != null) {
                    callback.onError("正在听取语音...");
                }
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                String errorMessage;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        errorMessage = "音频错误，请重试";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        errorMessage = "客户端错误，请重启应用";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        errorMessage = "缺少必要权限，请在设置中授予权限";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        errorMessage = "网络连接错误，请检查网络设置";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        errorMessage = "网络超时，请检查网络连接";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        errorMessage = "未能识别语音，请重试";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        errorMessage = "识别服务忙，请稍后重试";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        errorMessage = "服务器错误，请稍后重试";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        errorMessage = "未检测到语音输入，请重试";
                        break;
                    default:
                        errorMessage = "语音识别出错，请重试";
                        break;
                }
                if (callback != null) {
                    callback.onError(errorMessage);
                }
                // 重新初始化语音识别器
                destroy();
                initializeSpeechRecognizer();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && callback != null) {
                    callback.onResult(matches.get(0));
                } else if (callback != null) {
                    callback.onError("未能识别语音，请重试");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && callback != null) {
                    callback.onResult(matches.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    public void startListening(boolean isChinese) {
        currentRetryCount = 0;
        if (speechRecognizer == null) {
            initializeSpeechRecognizer();
            if (speechRecognizer == null) {
                if (callback != null) {
                    callback.onError("语音识别初始化失败，请检查Google语音识别服务是否正常安装");
                }
                return;
            }
        }

        // 设置语言
        String language = isChinese ? "zh-CN" : "en-US";
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);

        try {
            speechRecognizer.startListening(recognizerIntent);
        } catch (Exception e) {
            if (callback != null) {
                callback.onError("启动语音识别失败: " + e.getMessage());
            }
        }
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    public void setCallback(RecognitionCallback callback) {
        this.callback = callback;
    }

    public interface RecognitionCallback {
        void onResult(String recognizedText);
        void onError(String errorMessage);
    }
}