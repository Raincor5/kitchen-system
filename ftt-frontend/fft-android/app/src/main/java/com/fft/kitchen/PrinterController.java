package com.fft.kitchen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import woyou.aidlservice.jiuiv5.IWoyouService;

public class PrinterController {
    private static final String TAG = "PrinterController";
    private static final String PREFS_NAME = "LabelConfig";
    private static final String CONFIG_KEY = "label_config";
    
    private final Context context;
    private final Gson gson;
    private LabelConfig labelConfig;
    private IWoyouService woyouService;
    private boolean isServiceConnected = false;

    private final ServiceConnection connService = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            woyouService = null;
            isServiceConnected = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            woyouService = IWoyouService.Stub.asInterface(service);
            isServiceConnected = true;
        }
    };

    public PrinterController(Context context) {
        this.context = context;
        this.gson = new Gson();
        loadConfig();
        bindPrinterService();
    }

    private void bindPrinterService() {
        Intent intent = new Intent();
        intent.setPackage("woyou.aidlservice.jiuiv5");
        intent.setAction("woyou.aidlservice.jiuiv5.IWoyouService");
        context.bindService(intent, connService, Context.BIND_AUTO_CREATE);
    }

    private void loadConfig() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String configJson = prefs.getString(CONFIG_KEY, null);
        
        if (configJson != null) {
            Type type = new TypeToken<LabelConfig>(){}.getType();
            labelConfig = gson.fromJson(configJson, type);
        } else {
            labelConfig = new LabelConfig();
        }
    }

    public void printLabel(String productName, Date startDate, Date endDate) {
        if (!isServiceConnected) {
            Log.e(TAG, "Printer service not connected");
            return;
        }

        try {
            // Create a bitmap for the label
            float density = context.getResources().getDisplayMetrics().density;
            int widthPx = (int) (labelConfig.getLabelWidth() * density);
            int heightPx = (int) (labelConfig.getLabelHeight() * density);
            
            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            
            // Set background color
            canvas.drawColor(0xFFFFFFFF); // White background
            
            // Create paint for text
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(0xFF000000); // Black text
            
            // Format dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateText = dateFormat.format(startDate) + " - " + dateFormat.format(endDate);
            
            // Calculate text positions
            float centerX = widthPx / 2f;
            float productNameY = heightPx * 0.4f;
            float dateY = heightPx * 0.6f;
            
            // Draw product name
            paint.setTextSize(labelConfig.getProductNameSize() * density);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            float productNameWidth = paint.measureText(productName);
            float productNameX = getAlignedX(centerX, productNameWidth, labelConfig.getAlignment());
            canvas.drawText(productName, productNameX, productNameY, paint);
            
            // Draw dates
            paint.setTextSize(labelConfig.getDateSize() * density);
            paint.setTypeface(Typeface.DEFAULT);
            float dateWidth = paint.measureText(dateText);
            float dateX = getAlignedX(centerX, dateWidth, labelConfig.getAlignment());
            canvas.drawText(dateText, dateX, dateY, paint);
            
            // Print the bitmap
            printBitmap(bitmap);
            
        } catch (Exception e) {
            Log.e(TAG, "Error printing label", e);
        }
    }

    private float getAlignedX(float centerX, float textWidth, int alignment) {
        switch (alignment) {
            case Gravity.LEFT:
                return 10; // Small margin from left edge
            case Gravity.RIGHT:
                return centerX * 2 - textWidth - 10; // Small margin from right edge
            case Gravity.CENTER:
            default:
                return centerX - textWidth / 2;
        }
    }

    private void printBitmap(Bitmap bitmap) {
        if (!isServiceConnected) {
            Log.e(TAG, "Printer service not connected");
            return;
        }

        try {
            // Apply paper offset
            woyouService.lineWrap((int)labelConfig.getPaperOffset(), null);
            
            // Print the bitmap
            woyouService.printBitmap(bitmap, null);
            
            // Feed paper
            woyouService.lineWrap(3, null);
            
        } catch (RemoteException e) {
            Log.e(TAG, "Error printing bitmap", e);
        }
    }

    public void onDestroy() {
        if (isServiceConnected) {
            context.unbindService(connService);
            isServiceConnected = false;
        }
    }

    private static class LabelConfig {
        private float paperOffset = 0;
        private float labelWidth = 80;
        private float labelHeight = 50;
        private int productNameSize = 24;
        private int dateSize = 18;
        private int alignment = Gravity.CENTER;

        public float getPaperOffset() { return paperOffset; }
        public float getLabelWidth() { return labelWidth; }
        public float getLabelHeight() { return labelHeight; }
        public int getProductNameSize() { return productNameSize; }
        public int getDateSize() { return dateSize; }
        public int getAlignment() { return alignment; }
    }
} 