//package com.example.transalator;
//
//import android.Manifest;
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.speech.RecognitionListener;
//import android.speech.RecognizerIntent;
//import android.speech.SpeechRecognizer;
//import android.view.MotionEvent;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.RadioButton;
//import android.widget.RadioGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.os.Handler;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.google.mlkit.nl.translate.TranslateLanguage;
//import com.google.mlkit.nl.translate.Translation;
//import com.google.mlkit.nl.translate.Translator;
//import com.google.mlkit.nl.translate.TranslatorOptions;
//
//import java.util.ArrayList;
//import java.util.Locale;
//
//public class MainActivity extends AppCompatActivity {
//
//    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
//    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;
//
//    private EditText inputText;
//    private TextView translatedText;
//    private ImageButton voiceButton;
//    private Button holdToSpeakButton;
//    private Button translateButton;
//    private RadioGroup translationDirection;
//    private RadioButton chineseToEnglish, englishToChinese;
//
//    private Translator chineseEnglishTranslator;
//    private Translator englishChineseTranslator;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // 初始化UI组件
//        inputText = findViewById(R.id.inputText);
//        translatedText = findViewById(R.id.translatedText);
//        voiceButton = findViewById(R.id.voiceButton);
//        holdToSpeakButton = findViewById(R.id.holdToSpeakButton);
//        translateButton = findViewById(R.id.translateButton);
//        translationDirection = findViewById(R.id.translationDirection);
//        chineseToEnglish = findViewById(R.id.chineseToEnglish);
//        englishToChinese = findViewById(R.id.englishToChinese);
//
//        // 检查并下载离线语音识别包
//        checkAndDownloadOfflineRecognitionModels();
//
//        // 初始化翻译器
//        initTranslators();
//
//        // 设置语音按钮点击事件
//        voiceButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // 检查麦克风权限
//                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(MainActivity.this,
//                            new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
//                } else {
//                    startSpeechRecognition();
//                }
//            }
//        });
//
//        // 设置按住说话按钮的触摸事件
//        holdToSpeakButton.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(MainActivity.this,
//                            new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
//                    return false;
//                }
//
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        // 按下按钮时开始录音
//                      //  holdToSpeakButton.setBackgroundResource(R.drawable.circle_button_pressed);
//                        Toast.makeText(MainActivity.this, "开始录音...", Toast.LENGTH_SHORT).show();
//                        startContinuousSpeechRecognition();
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                    case MotionEvent.ACTION_CANCEL:
//                        // 松开按钮时停止录音
//                      //  holdToSpeakButton.setBackgroundResource(R.drawable.circle_button_background);
//                        Toast.makeText(MainActivity.this, "录音结束", Toast.LENGTH_SHORT).show();
//                        stopContinuousSpeechRecognition();
//                        return true;
//                }
//                return false;
//            }
//        });
//
//        // 设置翻译按钮点击事件
//        translateButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String text = inputText.getText().toString().trim();
//                if (!text.isEmpty()) {
//                    translateText(text);
//                } else {
//                    Toast.makeText(MainActivity.this, "请输入或说出要翻译的文本", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }
//
//    private void initTranslators() {
//        // 创建中译英翻译器
//        TranslatorOptions chineseEnglishOptions = new TranslatorOptions.Builder()
//                .setSourceLanguage(TranslateLanguage.CHINESE)
//                .setTargetLanguage(TranslateLanguage.ENGLISH)
//                .build();
//        chineseEnglishTranslator = Translation.getClient(chineseEnglishOptions);
//
//        // 下载翻译模型（如果需要）
//        chineseEnglishTranslator.downloadModelIfNeeded()
//                .addOnSuccessListener(unused -> {
//                    // 模型下载成功
//                })
//                .addOnFailureListener(e -> {
//                    // 模型下载失败
//                    Toast.makeText(MainActivity.this, "中译英模型下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//
//        // 创建英译中翻译器
//        TranslatorOptions englishChineseOptions = new TranslatorOptions.Builder()
//                .setSourceLanguage(TranslateLanguage.ENGLISH)
//                .setTargetLanguage(TranslateLanguage.CHINESE)
//                .build();
//        englishChineseTranslator = Translation.getClient(englishChineseOptions);
//
//        // 下载翻译模型（如果需要）
//        englishChineseTranslator.downloadModelIfNeeded()
//                .addOnSuccessListener(unused -> {
//                    // 模型下载成功
//                })
//                .addOnFailureListener(e -> {
//                    // 模型下载失败
//                    Toast.makeText(MainActivity.this, "英译中模型下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    private void startSpeechRecognition() {
//        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        // 添加离线语音识别支持
//        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        // 修改这部分代码，确保正确设置语言
//        if (chineseToEnglish.isChecked()) {
//            // 如果是中译英，则设置中文识别
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN");
//            // 检查是否支持中文离线识别
//            checkLanguageSupport("zh-CN");
//        } else {
//            // 如果是英译中，则设置英文识别
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
//        }
//
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话...");
//
//        try {
//            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
//        } catch (Exception e) {
//            Toast.makeText(this, "您的设备不支持语音识别: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }
//    private void translateText(String text) {
//        // 显示加载状态
//        translatedText.setText("翻译中...");
//
//        // 根据选择的翻译方向选择翻译器
//        Translator translator;
//        if (chineseToEnglish.isChecked()) {
//            translator = chineseEnglishTranslator;
//        } else {
//            translator = englishChineseTranslator;
//        }
//
//        // 执行翻译
//        translator.translate(text)
//                .addOnSuccessListener(translatedText::setText)
//                .addOnFailureListener(e -> {
//                    translatedText.setText("翻译失败: " + e.getMessage());
//                    Toast.makeText(MainActivity.this, "翻译失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
//    // 添加检查语言支持的方法
//    private void checkLanguageSupport(String languageCode) {
//        // 检查是否支持指定的语言
//        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
//        sendOrderedBroadcast(detailsIntent, null, new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Bundle results = getResultExtras(true);
//                if (results != null) {
//                    ArrayList<String> supported = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
//                    if (supported != null && !supported.contains(languageCode)) {
//                        // 如果不支持该语言，提示下载
//                        Toast.makeText(MainActivity.this, "您的设备可能不支持离线" +
//                                (languageCode.equals("zh-CN") ? "中文" : "英文") +
//                                "语音识别，请下载相应语音包", Toast.LENGTH_LONG).show();
//
//                        // 提示用户下载语音包
//                        new Handler().postDelayed(() -> showDownloadDialog(), 1000);
//                    }
//                }
//            }
//        }, null, Activity.RESULT_OK, null, null);
//    }
//    // 添加检查并下载离线语音识别模型的方法
//    private void checkAndDownloadOfflineRecognitionModels() {
//        // 直接尝试打开语音识别器，检查是否支持离线模式
//        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(this);
//        Intent testIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        testIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
//        testIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//
//        // 设置一个临时的监听器来检测是否支持离线模式
//        recognizer.setRecognitionListener(new RecognitionListener() {
//            @Override
//            public void onReadyForSpeech(Bundle params) {
//                // 语音识别器准备就绪，可能支持离线模式
//                // 不要立即取消，让它有时间检测是否真的支持离线模式
//                new Handler().postDelayed(() -> {
//                    if (recognizer != null) {
//                        recognizer.cancel();
//                    }
//                }, 500);
//            }
//
//            @Override
//            public void onError(int error) {
//                // 如果出现错误，可能不支持离线模式或需要下载语音包
//                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
//                    error == SpeechRecognizer.ERROR_NETWORK ||
//                    error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
//                    error == SpeechRecognizer.ERROR_NO_MATCH) {
//                    // 提示用户下载离线语音识别包
//                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                    builder.setTitle("需要下载语音识别包")
//                            .setMessage("为了支持离线语音识别，需要下载语音识别包。是否现在下载？")
//                            .setPositiveButton("下载", (dialog, which) -> {
//                                try {
//                                    // 尝试直接打开语音设置
//                                    Intent intent = new Intent();
//                                    intent.setAction("android.settings.VOICE_INPUT_SETTINGS");
//                                    startActivity(intent);
//                                } catch (Exception e1) {
//                                    try {
//                                        // 如果上面的方法失败，尝试打开TTS设置
//                                        Intent ttsIntent = new Intent();
//                                        ttsIntent.setAction("com.android.settings.TTS_SETTINGS");
//                                        startActivity(ttsIntent);
//                                    } catch (Exception e2) {
//                                        try {
//                                            // 如果上面的方法也失败，尝试打开语言设置
//                                            Intent langIntent = new Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS);
//                                            startActivity(langIntent);
//                                        } catch (Exception e3) {
//                                            // 所有方法都失败，提示用户手动设置
//                                            Toast.makeText(MainActivity.this, "无法自动打开设置，请手动前往设置->语言和输入法->语音设置下载语音包", Toast.LENGTH_LONG).show();
//                                        }
//                                    }
//                                }
//                            })
//                            .setNegativeButton("取消", null)
//                            .show();
//            }
//            if (recognizer != null) {
//                recognizer.destroy();
//            }
//        }
//
//        // 实现其他必要的接口方法（可以留空）
//        @Override public void onBeginningOfSpeech() {}
//        @Override public void onRmsChanged(float rmsdB) {}
//        @Override public void onBufferReceived(byte[] buffer) {}
//        @Override public void onEndOfSpeech() {}
//        @Override public void onResults(Bundle results) {}
//        @Override public void onPartialResults(Bundle partialResults) {}
//        @Override public void onEvent(int eventType, Bundle params) {}
//    });
//
//    // 尝试启动语音识别
//    try {
//        recognizer.startListening(testIntent);
//        // 设置一个延迟，然后取消识别（我们只是测试，不需要真正识别）
//        new Handler().postDelayed(() -> {
//            if (recognizer != null) {
//                recognizer.cancel();
//                recognizer.destroy();
//            }
//        }, 1000);
//    } catch (Exception e) {
//        // 如果启动失败，可能是设备不支持或需要下载语音包
//        showDownloadDialog();
//        if (recognizer != null) {
//            recognizer.destroy();
//        }
//    }
//}
//
//// 显示下载对话框的辅助方法
//private void showDownloadDialog() {
//    AlertDialog.Builder builder = new AlertDialog.Builder(this);
//    builder.setTitle("需要下载语音识别包")
//            .setMessage("为了支持离线语音识别，需要下载语音识别包。是否现在下载？")
//            .setPositiveButton("下载", (dialog, which) -> {
//                try {
//                    // 尝试直接打开语音设置
//                    Intent intent = new Intent();
//                    intent.setAction("android.settings.VOICE_INPUT_SETTINGS");
//                    startActivity(intent);
//                } catch (Exception e) {
//                    Toast.makeText(this, "无法打开语音设置，请手动前往设置->语言和输入法下载语音包", Toast.LENGTH_LONG).show();
//                }
//            })
//            .setNegativeButton("取消", null)
//            .show();
//}
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
//            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//            if (result != null && !result.isEmpty()) {
//                String recognizedText = result.get(0);
//                inputText.setText(recognizedText);
//                // 自动翻译识别到的文本
//                translateText(recognizedText);
//            }
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startSpeechRecognition();
//            } else {
//                Toast.makeText(this, "需要麦克风权限才能使用语音识别功能", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    // 添加用于连续语音识别的方法
//    private SpeechRecognizer speechRecognizer;
//    private Intent recognizerIntent;
//    private void startContinuousSpeechRecognition() {
//        if (speechRecognizer == null) {
//            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
//            // 设置识别监听器
//            speechRecognizer.setRecognitionListener(new RecognitionListener() {
//                @Override
//                public void onReadyForSpeech(Bundle params) {}
//
//                @Override
//                public void onBeginningOfSpeech() {}
//
//                @Override
//                public void onRmsChanged(float rmsdB) {}
//
//                @Override
//                public void onBufferReceived(byte[] buffer) {}
//
//                @Override
//                public void onEndOfSpeech() {}
//
//                @Override
//                public void onError(int error) {
//                    String errorMessage;
//                    switch (error) {
//                        case SpeechRecognizer.ERROR_AUDIO:
//                            errorMessage = "音频错误";
//                            break;
//                        case SpeechRecognizer.ERROR_CLIENT:
//                            errorMessage = "客户端错误";
//                            break;
//                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
//                            errorMessage = "权限不足";
//                            break;
//                        case SpeechRecognizer.ERROR_NETWORK:
//                            errorMessage = "网络错误";
//                            break;
//                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
//                            errorMessage = "网络超时";
//                            break;
//                        case SpeechRecognizer.ERROR_NO_MATCH:
//                            errorMessage = "没有匹配的结果";
//                            break;
//                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
//                            errorMessage = "识别器忙";
//                            break;
//                        case SpeechRecognizer.ERROR_SERVER:
//                            errorMessage = "服务器错误";
//                            break;
//                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
//                            errorMessage = "语音超时";
//                            break;
//                        default:
//                            errorMessage = "未知错误";
//                            break;
//                    }
//                    Toast.makeText(MainActivity.this, "语音识别错误: " + errorMessage, Toast.LENGTH_SHORT).show();
//                }
//
//                @Override
//                public void onResults(Bundle results) {
//                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//                    if (matches != null && !matches.isEmpty()) {
//                        String recognizedText = matches.get(0);
//                        inputText.setText(recognizedText);
//                        // 自动翻译识别到的文本
//                        translateText(recognizedText);
//                    }
//                }
//
//                @Override
//                public void onPartialResults(Bundle partialResults) {}
//
//                @Override
//                public void onEvent(int eventType, Bundle params) {}
//            });
//        }
//
//        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
//        // 添加离线语音识别支持
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
//
//        // 修改这部分代码，确保正确设置语言
//        if (chineseToEnglish.isChecked()) {
//            // 如果是中译英，则设置中文识别
//            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
//            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN");
//            // 检查是否支持中文离线识别
//            checkLanguageSupport("zh-CN");
//        } else {
//            // 如果是英译中，则设置英文识别
//            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
//            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
//        }
//
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
//        speechRecognizer.startListening(recognizerIntent);
//    }
//    private void stopContinuousSpeechRecognition() {
//        if (speechRecognizer != null) {
//            speechRecognizer.stopListening();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // 关闭翻译器
//        if (chineseEnglishTranslator != null) {
//            chineseEnglishTranslator.close();
//        }
//        if (englishChineseTranslator != null) {
//            englishChineseTranslator.close();
//        }
//        // 释放语音识别器资源
//        if (speechRecognizer != null) {
//            speechRecognizer.destroy();
//        }
//    }
//}
package com.example.transalator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;
    private EditText inputText;
    private TextView translatedText;
    private ImageButton voiceButton;
    private Button holdToSpeakButton;
    private Button translateButton;
    private RadioGroup translationDirection;
    private RadioButton chineseToEnglish, englishToChinese;
    private Translator chineseEnglishTranslator;
    private Translator englishChineseTranslator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        inputText = findViewById(R.id.inputText);
        translatedText = findViewById(R.id.translatedText);
        voiceButton = findViewById(R.id.voiceButton);
        holdToSpeakButton = findViewById(R.id.holdToSpeakButton);
        translateButton = findViewById(R.id.translateButton);
        translationDirection = findViewById(R.id.translationDirection);
        chineseToEnglish = findViewById(R.id.chineseToEnglish);
        englishToChinese = findViewById(R.id.englishToChinese);

        // 检查并下载离线语音识别包
        checkAndDownloadOfflineRecognitionModels();

        // 初始化翻译器
        initTranslators();

        // 设置语音按钮点击事件
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 检查麦克风权限
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                } else {
                    startSpeechRecognition();
                }
            }
        });

        // 设置按住说话按钮的触摸事件
        holdToSpeakButton.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 按下按钮时开始录音
                        Toast.makeText(MainActivity.this, "开始录音...", Toast.LENGTH_SHORT).show();
                        startContinuousSpeechRecognition();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // 松开按钮时停止录音
                        Toast.makeText(MainActivity.this, "录音结束", Toast.LENGTH_SHORT).show();
                        stopContinuousSpeechRecognition();
                        return true;
                }
                return false;
            }
        });

        // 设置翻译按钮点击事件
        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString().trim();
                if (!text.isEmpty()) {
                    translateText(text);
                } else {
                    Toast.makeText(MainActivity.this, "请输入或说出要翻译的文本", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.setAction("android.settings.VOICE_INPUT_SETTINGS");
                    startActivity(intent);
                }
            }
        });
    }

    private void initTranslators() {
        // 创建中译英翻译器
        TranslatorOptions chineseEnglishOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.CHINESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build();
        chineseEnglishTranslator = Translation.getClient(chineseEnglishOptions);

        // 下载翻译模型（如果需要）
        chineseEnglishTranslator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    // 模型下载成功
                })
                .addOnFailureListener(e -> {
                    // 模型下载失败
                    Toast.makeText(MainActivity.this, "中译英模型下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // 创建英译中翻译器
        TranslatorOptions englishChineseOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.CHINESE)
                .build();
        englishChineseTranslator = Translation.getClient(englishChineseOptions);

        // 下载翻译模型（如果需要）
        englishChineseTranslator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    // 模型下载成功
                })
                .addOnFailureListener(e -> {
                    // 模型下载失败
                    Toast.makeText(MainActivity.this, "英译中模型下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // 添加离线语音识别支持
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // 修改这部分代码，确保正确设置语言
        if (chineseToEnglish.isChecked()) {
            // 如果是中译英，则设置中文识别
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN");
            // 检查是否支持中文离线识别
            checkLanguageSupport("zh-CN");
        } else {
            // 如果是英译中，则设置英文识别
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
        }

        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话...");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "您的设备不支持语音识别: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void translateText(String text) {
        // 显示加载状态
        translatedText.setText("翻译中...");

        // 根据选择的翻译方向选择翻译器
        Translator translator;
        if (chineseToEnglish.isChecked()) {
            translator = chineseEnglishTranslator;
        } else {
            translator = englishChineseTranslator;
        }

        // 执行翻译
        translator.translate(text)
                .addOnSuccessListener(translatedText::setText)
                .addOnFailureListener(e -> {
                    translatedText.setText("翻译失败: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "翻译失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // 添加检查语言支持的方法
    private void checkLanguageSupport(String languageCode) {
        // 检查是否支持指定的语言
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        sendOrderedBroadcast(detailsIntent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle results = getResultExtras(true);
                if (results != null) {
                    ArrayList<String> supported = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                    if (supported != null && !supported.contains(languageCode)) {
                        // 如果不支持该语言，提示下载
                        Toast.makeText(MainActivity.this, "您的设备可能不支持离线" +
                                (languageCode.equals("zh-CN") ? "中文" : "英文") +
                                "语音识别，请下载相应语音包", Toast.LENGTH_LONG).show();

                        // 提示用户下载语音包
                        new Handler().postDelayed(() -> showDownloadDialog(), 1000);
                    }
                }
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    // 添加检查并下载离线语音识别模型的方法
    private void checkAndDownloadOfflineRecognitionModels() {
        Log.d("SpeechRecognition", "Checking offline speech recognition support...");

        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent testIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        testIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        testIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("SpeechRecognition", "Speech recognizer is ready.");
                new Handler().postDelayed(() -> {
                    if (recognizer != null) {
                        recognizer.cancel();
                    }
                }, 500);
            }

            @Override
            public void onError(int error) {
                Log.e("SpeechRecognition", "Speech recognition error: " + error);
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                        error == SpeechRecognizer.ERROR_NETWORK ||
                        error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_NO_MATCH) {
                    showDownloadDialog();
                }
                if (recognizer != null) {
                    recognizer.destroy();
                }
            }

            // 其他方法...
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onResults(Bundle results) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        try {
            recognizer.startListening(testIntent);
            new Handler().postDelayed(() -> {
                if (recognizer != null) {
                    recognizer.cancel();
                    recognizer.destroy();
                }
            }, 1000);
        } catch (Exception e) {
            Log.e("SpeechRecognition", "Failed to start speech recognition: " + e.getMessage());
            showDownloadDialog();
            if (recognizer != null) {
                recognizer.destroy();
            }
        }
    }

    // 显示下载对话框的辅助方法
    private void showDownloadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("需要下载语音识别包")
                .setMessage("为了支持离线语音识别，需要下载语音识别包。是否现在下载？")
                .setPositiveButton("下载", (dialog, which) -> {
                    try {
                        // 尝试直接打开语音设置
                        Intent intent = new Intent();
                        intent.setAction("android.settings.VOICE_INPUT_SETTINGS");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "无法打开语音设置，请手动前往设置->语言和输入法下载语音包", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String recognizedText = result.get(0);
                inputText.setText(recognizedText);
                // 自动翻译识别到的文本
                translateText(recognizedText);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
            } else {
                Toast.makeText(this, "需要麦克风权限才能使用语音识别功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 添加用于连续语音识别的方法
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private void startContinuousSpeechRecognition() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            // 设置识别监听器
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {}

                @Override
                public void onBeginningOfSpeech() {}

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
                            errorMessage = "音频错误";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            errorMessage = "客户端错误";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            errorMessage = "权限不足";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            errorMessage = "网络错误";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            errorMessage = "网络超时";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            errorMessage = "没有匹配的结果";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            errorMessage = "识别器忙";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            errorMessage = "服务器错误";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            errorMessage = "语音超时";
                            break;
                        default:
                            errorMessage = "未知错误";
                            break;
                    }
                    Toast.makeText(MainActivity.this, "语音识别错误: " + errorMessage, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String recognizedText = matches.get(0);
                        inputText.setText(recognizedText);
                        // 自动翻译识别到的文本
                        translateText(recognizedText);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        // 添加离线语音识别支持
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        // 修改这部分代码，确保正确设置语言
        if (chineseToEnglish.isChecked()) {
            // 如果是中译英，则设置中文识别
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN");
            // 检查是否支持中文离线识别
            checkLanguageSupport("zh-CN");
        } else {
            // 如果是英译中，则设置英文识别
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
        }

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.startListening(recognizerIntent);
    }

    private void stopContinuousSpeechRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭翻译器
        if (chineseEnglishTranslator != null) {
            chineseEnglishTranslator.close();
        }
        if (englishChineseTranslator != null) {
            englishChineseTranslator.close();
        }
        // 释放语音识别器资源
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}