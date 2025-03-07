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
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // 调用vosk模型来识别语音
    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_FILE = 3;
    static private final int STATE_MIC = 4;
    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private boolean isChineseToEnglish = false;
    private TextView resultView;

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;
    private EditText inputText;
    private TextView translatedText;
    private ImageButton voiceButton;
    private Button buttonSpeak;
    private Button translateButton;
    private RadioGroup translationDirection;
    private RadioButton chineseToEnglish, englishToChinese;
    private Translator chineseEnglishTranslator;
    private Translator englishChineseTranslator;
    private org.vosk.android.RecognitionListener voskListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        inputText = findViewById(R.id.inputText);           //文本输入
        translatedText = findViewById(R.id.translatedText); //翻译文本
        voiceButton = findViewById(R.id.voiceButton);               //Google语音
        buttonSpeak = findViewById(R.id.holdToSpeakButton);         //vosk按住说话
        translateButton = findViewById(R.id.translateButton);           //翻译按钮
        translationDirection = findViewById(R.id.translationDirection); //radio Group
        chineseToEnglish = findViewById(R.id.chineseToEnglish); //中文转英文radio按钮
        englishToChinese = findViewById(R.id.englishToChinese); //英文转中文radio
        resultView = findViewById(R.id.result_text);    //vosk语音包生成结果

        // 检查并下载离线语音识别包
        checkAndDownloadOfflineRecognitionModels();

        // 初始化翻译器
        initTranslators();

        // 设置语言方向切换监听
        translationDirection.setOnCheckedChangeListener((group, checkedId) -> {
            isChineseToEnglish = checkedId == R.id.chineseToEnglish;
            // 重新初始化模型
            initModel();
        });

        // 保留原有的按钮事件，但不会显示在界面上
        findViewById(R.id.recognize_file).setOnClickListener(view -> recognizeFile());
        findViewById(R.id.recognize_mic).setOnClickListener(view -> recognizeMicrophone());
        ((ToggleButton) findViewById(R.id.pause)).setOnCheckedChangeListener((view, isChecked) -> pause(isChecked));

        LibVosk.setLogLevel(LogLevel.INFO);

        // 检查录音权限
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initModel();
        }

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
        // 修改为点击说话按钮
        buttonSpeak.setText("点击说话");
        buttonSpeak.setOnClickListener(v -> {
            if (speechService != null) {
                // 如果正在录音，则停止
                speechService.stop();
                speechService = null;
                buttonSpeak.setText("点击说话");
                setUiState(STATE_DONE);
            } else {
                // 开始录音
                recognizeMicrophone();
                buttonSpeak.setText("停止说话");
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

        voskListener = new org.vosk.android.RecognitionListener() {
            @Override
            public void onPartialResult(String hypothesis) {
                // 记录原始数据到resultView
                resultView.append("部分结果: " + hypothesis + "\n");

                // 提取部分识别结果
                if (hypothesis != null && !hypothesis.isEmpty()) {
                    try {
                        if (hypothesis.contains("\"partial\":")) {
                            // 使用简单的字符串处理提取文本
                            int startIndex = hypothesis.indexOf("\"partial\":") + 11;
                            int endIndex = hypothesis.lastIndexOf("\"");
                            if (startIndex > 10 && endIndex > startIndex) {
                                // 将text声明为final，使其可以在Lambda表达式中使用
                                final String text = hypothesis.substring(startIndex, endIndex)
                                        .replace("{", "").replace("}", "").replace(" ", "");

                                // 确保在UI线程更新EditText
                                runOnUiThread(() -> {
                                    // 直接设置文本到EditText
                                    inputText.setText(text);
                                    // 将光标移动到文本末尾
                                    inputText.setSelection(text.length());
                                    // 打印调试信息
                                    System.out.println("设置部分文本到EditText: " + text);
                                });
                            }
                        }
                    } catch (Exception e) {
                        resultView.append("解析错误: " + e.getMessage() + "\n");
                    }
                }
            }

            @Override
            public void onResult(String hypothesis) {
                // Vosk 的识别结果处理
                // 记录原始数据到resultView
                resultView.append("结果: " + hypothesis + "\n");

                // 提取识别结果
                if (hypothesis != null && !hypothesis.isEmpty()) {
                    try {
                        if (hypothesis.contains("\"text\":")) {
                            // 使用简单的字符串处理提取文本
                            int startIndex = hypothesis.indexOf("\"text\":") + 8;
                            int endIndex = hypothesis.lastIndexOf("\"");
                            if (startIndex > 7 && endIndex > startIndex) {
                                // 将text声明为final，使其可以在Lambda表达式中使用
                                final String text = hypothesis.substring(startIndex, endIndex)
                                        .replace("{", "").replace("}", "").replace(" ", "");

                                // 确保在UI线程更新EditText
                                runOnUiThread(() -> {
                                    // 直接设置文本到EditText
                                    inputText.setText(text);
                                    // 将光标移动到文本末尾
                                    inputText.setSelection(text.length());
                                    // 打印调试信息
                                    System.out.println("设置结果文本到EditText: " + text);
                                });
                            }
                        }
                    } catch (Exception e) {
                        resultView.append("解析错误: " + e.getMessage() + "\n");
                    }
                }
            }

            @Override
            public void onFinalResult(String hypothesis) {
                // 记录原始数据到resultView
                resultView.append("最终结果: " + hypothesis + "\n");

                // 提取最终识别结果
                if (hypothesis != null && !hypothesis.isEmpty()) {
                    try {
                        // 使用更精确的方式提取JSON中的text字段
                        if (hypothesis.contains("\"text\"")) {
                            // 提取text字段的值并声明为final
                            final String text = hypothesis.replaceAll(".*\"text\"\\s*:\\s*\"([^\"]*)\".*", "$1")
                                    .replace("{", "").replace("}", "").replace(" ", "");

                            // 确保在UI线程更新EditText
                            runOnUiThread(() -> {
                                // 直接设置文本到EditText
                                inputText.setText(text);
                                translateText(text);
                                // 将光标移动到文本末尾
                                inputText.setSelection(text.length());
                                // 打印调试信息
                                System.out.println("设置最终文本到EditText: " + text);
                            });
                        }
                    } catch (Exception e) {
                        resultView.append("解析错误: " + e.getMessage() + "\n");
                        e.printStackTrace();
                    }
                }

                setUiState(STATE_DONE);
                if (speechStreamService != null) {
                    speechStreamService = null;
                }

                // 重置按钮文字
                runOnUiThread(() -> {
                    buttonSpeak.setText("点击说话");
                });
            }

            @Override
            public void onError(Exception e) {
                setErrorState(e.getMessage());
            }

            @Override
            public void onTimeout() {
                // Vosk 的超时处理
                setUiState(STATE_DONE);
            }
        };


    }

    // Vosk 初始化模型
    private void initModel() {
        setUiState(STATE_START);

        if (isChineseToEnglish) {
            // 加载中文模型
            StorageService.unpack(this, "vosk-model-small-cn-0.22", "model",
                    (model) -> {
                        this.model = model;
                        setUiState(STATE_READY);
                    },
                    (exception) -> setErrorState("加载中文模型失败: " + exception.getMessage()));
        } else {
            // 加载英文模型
            StorageService.unpack(this, "model-en-us", "model",
                    (model) -> {
                        this.model = model;
                        setUiState(STATE_READY);
                    },
                    (exception) -> setErrorState("加载英文模型失败: " + exception.getMessage()));
        }
    }

    private void recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_DONE);
            speechService.stop();
            speechService = null;
        } else {
            setUiState(STATE_MIC);
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                // 修改这里：使用voskListener而不是MainActivity.this
                speechService.startListening(voskListener);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

    private void recognizeFile() {
        if (speechStreamService != null) {
            setUiState(STATE_DONE);
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            setUiState(STATE_FILE);
            try {
                Recognizer rec = new Recognizer(model, 16000.f, "[\"one zero zero zero one\", " +
                        "\"oh zero one two three four five six seven eight nine\", \"[unk]\"]");

                InputStream ais = getAssets().open(
                        "10001-90210-01803.wav");
                if (ais.skip(44) != 44) throw new IOException("File too short");

                speechStreamService = new SpeechStreamService(rec, ais, 16000);
                // 修改这里：使用voskListener而不是this
                speechStreamService.start(voskListener);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
                resultView.setText("正在准备Vosk模型...");
                buttonSpeak.setEnabled(false);
                break;
            case STATE_READY:
                resultView.setText("Vosk模型准备就绪，请按住按钮说话");
                buttonSpeak.setEnabled(true);
                break;
            case STATE_DONE:
                buttonSpeak.setEnabled(true);
                break;
            case STATE_MIC:
                resultView.setText("请说话...");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    private void setErrorState(String message) {
        resultView.setText(message);
        buttonSpeak.setEnabled(false);
    }

    private void pause(boolean checked) {
        if (speechService != null) {
            speechService.setPause(checked);
        }
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
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
                initModel();
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

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }
}