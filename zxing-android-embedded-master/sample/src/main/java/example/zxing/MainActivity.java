package example.zxing;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Intents;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.CameraPreview;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Hashtable;
import org.apache.commons.text.StringEscapeUtils;


public class MainActivity extends AppCompatActivity {
    private ImageView mImageView;
    private Button mButton;
    private static Button mButton2;
    private EditText mTime, mData;
    private RadioGroup mLevel;

    private CountDownTimer timer;
    private String[] dataArr; // ????????????
    private String dataStr; // ????????????
    private int dataLen; // ????????????
    private int countDown = 1500; // ????????????
    private int qrInt = 0; // ???????????????????????????
    private int qrSize = 1000; // QRcode????????????
    private int singleDataSize = 1000; // ??????QRCode????????????

    public final int CUSTOMIZED_REQUEST_CODE = 0x0000ffff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        mImageView = (ImageView) findViewById(R.id.iv);
        mButton = (Button) findViewById(R.id.bt);
        mButton2 = (Button) findViewById(R.id.bt2);
        mTime = (EditText) findViewById(R.id.editTime);
        mData = (EditText) findViewById(R.id.editData);
        mLevel = (RadioGroup) findViewById(R.id.rgLevel);

        mTime.setText("" + countDown);
        mData.setText("" + singleDataSize);
        mLevel.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton radioButton = (RadioButton) radioGroup.findViewById(i);
                QRCodeUtil.level = radioButton.getText().toString();
            }
        });

        timer = new TimeCount(0,0);
    }

    public void checkPermission() { // ????????????????????????
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    1);
        }

        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            finish();
            System.exit(0);
        }

    }

    public void dataCut(File file) { // ????????????
        byte bytes[] = new byte[(int) file.length()]; // ??????????????????????????????
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file)); // ??????????????????
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DataInputStream dis = new DataInputStream(bis); // ????????????
        try {
            dis.readFully(bytes); // ??????????????????
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataStr = bytes2HexString(bytes, bytes.length);
//        dataStr = new String(bytes);
//        dataStr = str2HexStr(dataStr); // ????????????16??????
        dataLen = (dataStr.length() / singleDataSize) + 1; // ???????????????
        dataArr = new String[dataLen];
        for(int i = 0; i < dataLen - 1; i++) { // ??????????????????????????????
            int num = i * singleDataSize;
            dataArr[i] = "!@#$" + file.getName() + "@" + i + "#" + dataLen + "$" + dataStr.substring(num, num + singleDataSize);
        }
        dataArr[dataLen - 1] = "!@#$" + file.getName() + "@" + (dataLen - 1) + "#" + dataLen + "$" + dataStr.substring((dataLen - 1) * singleDataSize, (dataLen - 1) * singleDataSize + dataStr.length() % singleDataSize);

        timer = new TimeCount(dataLen * countDown, countDown).start(); // ???????????????????????????QRcode
    }

    public void codeDisplay(String str) { // ??????QRcode
        Bitmap mBitmap = QRCodeUtil.createQRCodeBitmap(str, qrSize, qrSize);
        mImageView.setImageBitmap(mBitmap);
    }

    public void onClick(View view) {
        qrSize = mImageView.getWidth();
        countDown = Integer.parseInt(mTime.getText().toString());
        singleDataSize = Integer.parseInt(mData.getText().toString());

        Intent getContent = new Intent(Intent.ACTION_GET_CONTENT); // ????????????????????????
        getContent.setType("*/*");
        getContent.addCategory(Intent.CATEGORY_OPENABLE);
        getContent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
//        startActivity(getContent);
        startActivityForResult(Intent.createChooser(getContent, "Select a File to Upload"), 1);
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);// ????????????????????????,????????????????????????
        }

        @Override
        public void onFinish() { // ?????????????????????
            timer.cancel();
            timer = new TimeCount(dataLen * countDown, countDown).start();
        }

        @Override
        public void onTick(long millisUntilFinished) {// ????????????
            codeDisplay(dataArr[qrInt]);
            mButton.setText("" + dataStr.length() / 2 + ":" + (qrInt + 1) + "/" + dataLen); // ????????????QRcode
            if (qrInt < dataLen - 1) {
                qrInt++;
            } else {
                qrInt = 0;
            }
        }
    }

    public void scanContinuous(View view) { // ??????QRCode????????????
            Intent intent = new Intent(this, ContinuousCaptureActivity.class);
            startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) { // ??????????????????
            timer.cancel();
            if (data != null) {
                qrInt = 0;

                final Uri uri = data.getData();
                String path = getFilePathByUri(MainActivity.this, uri);
                File file = new File(path);
                dataCut(file);
            }
        }

        if (requestCode != CUSTOMIZED_REQUEST_CODE && requestCode != IntentIntegrator.REQUEST_CODE) {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        switch (requestCode) {
            case CUSTOMIZED_REQUEST_CODE: {
                Toast.makeText(this, "REQUEST_CODE = " + requestCode, Toast.LENGTH_LONG).show();
                break;
            }
            default:
                break;
        }

        IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);

        if(result.getContents() == null) {
            Log.d("MainActivity", "Cancelled scan");
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            Log.d("MainActivity", "Scanned");
            Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sample of scanning from a Fragment
     */
    public static class ScanFragment extends Fragment {
        private String toast;

        public ScanFragment() {
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            displayToast();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_scan, container, false);
            Button scan = (Button) view.findViewById(R.id.scan_from_fragment);
            scan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanFromFragment();
                }
            });
            return view;
        }

        public void scanFromFragment() {
            IntentIntegrator.forSupportFragment(this).initiateScan();
        }

        private void displayToast() {
            if(getActivity() != null && toast != null) {
                Toast.makeText(getActivity(), toast, Toast.LENGTH_LONG).show();
                toast = null;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if(result != null) {
                if(result.getContents() == null) {
                    toast = "Cancelled from fragment";
                } else {
                    toast = "Scanned from fragment: " + result.getContents();
                }

                // At this point we may or may not have a reference to the activity
                displayToast();
            }
        }
    }

    public static String getFilePathByUri(Context context, Uri uri) {
//        mButton2.setText(uri.getAuthority());
        String path = null;
        // ??? file:// ?????????
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        // ??? content:// ?????????????????? content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
            return path;
        }
        // 4.4???????????? ?????? content:// ?????????????????? content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                }else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {

                // Return the remote address
                if (isGooglePhotosUri(uri))
                    return uri.getLastPathSegment();

                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
//        return false;
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
//        return false;
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
//        return false;
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
//        return false;
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     *  ?????????????????????16???????????????
     * @param str
     * @return
     */
    public static String str2HexStr(String str)
    {
        byte[] bytes = str.getBytes();
        // ?????????????????????????????????Integer
        BigInteger bigInteger = new BigInteger(1, bytes);
        return bigInteger.toString(16);
    }

    public static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    /*
     * ???????????????16?????????????????????
     */
    public String bytes2HexString(byte[] b,int length) {
        String r = "";

        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            r += hex.toUpperCase();
        }

        return r;
    }
}

class QRCodeUtil {
    static String level = "L";

    /**
     * ?????????????????????
     *
     * @param content ???????????????(????????????)
     * @param width ????????????(??????:px)
     * @param height ????????????(??????:px)
     * @return
     */
    @Nullable
    public static Bitmap createQRCodeBitmap(String content, int width, int height){
        return createQRCodeBitmap(content, width, height, null, level, "2", Color.BLACK, Color.WHITE);
    }

    /**
     * ????????????????????? (???????????????????????????????????????)
     *
     * @param content ???????????????
     * @param width ????????????,??????>=0(??????:px)
     * @param height ????????????,??????>=0(??????:px)
     * @param character_set ?????????/?????????????????? (????????????:{@link CharacterSetECI })??????null???,zxing?????????????????? "ISO-8859-1"
     * @param error_correction ???????????? (????????????:{@link ErrorCorrectionLevel })??????null???,zxing?????????????????? "L"
     * @param margin ???????????? (?????????,??????:?????????>=0), ???null???,zxing??????????????????"4"???
     * @param color_black ?????????????????????????????????
     * @param color_white ?????????????????????????????????
     * @return
     */
    @Nullable
    public static Bitmap createQRCodeBitmap(String content, int width, int height,
                                            @Nullable String character_set, @Nullable String error_correction, @Nullable String margin,
                                            @ColorInt int color_black, @ColorInt int color_white){

        /** 1.????????????????????? */
        if(TextUtils.isEmpty(content)){ // ?????????????????????
            return null;
        }

        if(width < 0 || height < 0){ // ??????????????????>=0
            return null;
        }

        try {
            /** 2.???????????????????????????,??????BitMatrix(?????????)?????? */
            Hashtable<EncodeHintType, String> hints = new Hashtable<>();

            if(!TextUtils.isEmpty(character_set)) {
                hints.put(EncodeHintType.CHARACTER_SET, character_set); // ????????????????????????
            }

            if(!TextUtils.isEmpty(error_correction)){
                hints.put(EncodeHintType.ERROR_CORRECTION, error_correction); // ??????????????????
            }

            if(!TextUtils.isEmpty(margin)){
                hints.put(EncodeHintType.MARGIN, margin); // ??????????????????
            }
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            /** 3.??????????????????,?????????BitMatrix(?????????)????????????????????????????????? */
            int[] pixels = new int[width * height];
            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    if(bitMatrix.get(x, y)){
                        pixels[y * width + x] = color_black; // ????????????????????????
                    } else {
                        pixels[y * width + x] = color_white; // ????????????????????????
                    }
                }
            }

            /** 4.??????Bitmap??????,????????????????????????Bitmap???????????????????????????,????????????Bitmap?????? */
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;
    }
}

