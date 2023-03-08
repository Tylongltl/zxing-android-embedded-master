package example.zxing;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import org.apache.commons.text.StringEscapeUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static example.zxing.MainActivity.uniteBytes;

/**
 * This sample performs continuous scanning, displaying the barcode and source image whenever
 * a barcode is scanned.
 */
public class ContinuousCaptureActivity extends Activity {
    private static final String TAG = ContinuousCaptureActivity.class.getSimpleName();
    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String lastText;
    private String[] dataArr;

    private Button mTopButton;
    private Button mBotButton;
    private int lastInt = 0;
    private int intCheck = 0;
    private String fileCheck = "";

    private String pathStr = "/storage/emulated/0/download/";

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) { // 資料接收
            if (result.getText() == null || result.getText().equals(lastText)) {
                // Prevent duplicate scans
                return;
            }

            if(result.getText().contains("!@#$")) { //判斷是否為動態條碼
                String subStr = result.getText().substring(4, result.getText().indexOf("$", 4)); // 獲得規格部分
                String name = subStr.substring(0, subStr.indexOf("@")); // 獲得檔案名字
                int nowInt = Integer.parseInt(subStr.substring(subStr.indexOf("@") + 1, subStr.indexOf("#"))); // 獲得當前頁數
                int total = Integer.parseInt(subStr.substring(subStr.indexOf("#") + 1)); // 獲得總共頁數

                if (!(fileCheck.equals(name) && intCheck != total)) { // 檢查是否為新檔案
                    fileCheck = name;
                    dataArr = new String[total]; // 建立緩存區
                    lastInt = 0;
                }

                if (dataArr[nowInt] != null) { // 檢查是否重複讀取
                    mTopButton.setBackgroundColor(Color.RED);
                } else {
                    dataArr[nowInt] = result.getText().substring(result.getText().indexOf("$", 4) + 1); // 資料分批寫入
                    if (lastInt < total) { // 記錄以完成張數
                        lastInt++;
                    }
                    mTopButton.setBackgroundColor(Color.GREEN);
                }
                barcodeView.setStatusText(dataArr[nowInt].length() + ":" + (nowInt + 1));

                if (total == lastInt) { // 檢查是否全部讀取完成
                    File file = new File(pathStr + name); // 建立空檔案
                    String allData = "";
                    for (int i = 0; i < total; i++) {
                        try {
                            allData = allData.concat(hex2String(dataArr[i])); // 資料片段整理後重新組合
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
//                    byte[] data = allData.getBytes(); // 資料轉型
                    try {
                        FileOutputStream OutputFile = new FileOutputStream(file);
                        DataOutputStream out = new DataOutputStream(OutputFile);
                        out.writeBytes(allData);
                        out.close();
                        OutputFile.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    barcodeView.setStatusText("" + allData.length() + ":OK");
                    beepManager.playBeepSoundAndVibrate(); // 發出聲音
                }

//                if(intCheck == 0) { // 漏頁檢查
//                    intCheck = nowInt;
//                } else {
//                    if(nowInt - intCheck == 1) {
//                        mTopButton.setBackgroundColor(Color.GREEN);
//                    } else {
//                        mTopButton.setBackgroundColor(Color.RED);
//                    }
//                    intCheck = nowInt;
//                }

                mTopButton.setText("" + lastInt + "/" + total);
            } else {
                beepManager.playBeepSoundAndVibrate(); // 發出聲音
                barcodeView.setStatusText(result.getText());
            }
            lastText = result.getText();

            //Added preview of scanned barcode
            ImageView imageView = (ImageView) findViewById(R.id.barcodePreview);
            imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW));
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    /** 16进制的字符串转换成16进制字符串数组
     * @param src
     * @return
     */
    public static byte[] hexString2Bytes(String src) {
        int len = src.length() / 2;
        byte[] ret = new byte[len];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < len; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    /*
     * 16进制字符串转字符串
     */
    public static String hex2String(String hexString) {
        StringBuilder str = new StringBuilder();
        for (int i = 0 ; i < hexString.length() ; i += 2)
            str.append((char) Integer.parseInt(hexString.substring(i, i + 2), 16));
        return str.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.continuous_scan);

        barcodeView = (DecoratedBarcodeView) findViewById(R.id.barcode_scanner);
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.decodeContinuous(callback);

        beepManager = new BeepManager(this);

        mTopButton = (Button) findViewById(R.id.btTop);
        mBotButton = (Button) findViewById(R.id.btBot);
    }

    @Override
    protected void onResume() {
        super.onResume();

        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();
    }

    public void pause(View view)  {
//        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();

        mTopButton.setBackgroundColor(Color.GRAY);
        lastInt = 0;
//        intCheck = 0;
        fileCheck = "";
        mTopButton.setText("" + lastInt);
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
