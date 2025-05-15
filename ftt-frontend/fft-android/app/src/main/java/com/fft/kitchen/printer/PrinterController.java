package com.fft.kitchen.printer;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import com.fft.kitchen.printer.AidlPrinterHelper;

public class PrinterController {
    private static final String TAG = "PrinterController";
    private final Context context;
    private final PrinterSettings settings;
    private final AidlPrinterHelper printer;
    
    public PrinterController(Context context) {
        this.context = context;
        this.settings = PrinterSettings.getInstance(context);
        this.printer = AidlPrinterHelper.getInstance(context);
    }
    
    public int getMaxPrinterWidth() {
        return 480; // Standard thermal printer width in pixels
    }
    
    private int getAlignment(int gravity) {
        switch (gravity) {
            case Gravity.CENTER:
                return 1; // CENTER
            case Gravity.END:
                return 2; // RIGHT
            case Gravity.START:
            default:
                return 0; // LEFT
        }
    }
    
    private float getTextSize(float size) {
        return size; // AidlPrinterHelper handles font sizes directly
    }
    
    public void printLabel(String productName, String date) {
        try {
            // Get current settings
            float fontSize = settings.getFontSize();
            int alignment = settings.getTextAlignment();
            int labelWidth = settings.getLabelWidth();
            int labelHeight = settings.getLabelHeight();
            
            // Print label
            printer.printerInit();
            printer.setAlignment(getAlignment(alignment));
            printer.setFontSize(getTextSize(fontSize));
            printer.printText(productName);
            printer.feedPaper(1.0f);
            
            printer.setFontSize(getTextSize(fontSize * 0.8f));
            printer.printText(date);
            printer.feedPaper(settings.getLinesPerFeed());
            
            Log.d(TAG, "Label printed successfully");
            Log.d(TAG, "Product Name: " + productName);
            Log.d(TAG, "Date: " + date);
            Log.d(TAG, "Font Size: " + fontSize);
            Log.d(TAG, "Alignment: " + alignment);
            Log.d(TAG, "Label Width: " + labelWidth);
            Log.d(TAG, "Label Height: " + labelHeight);
            
        } catch (Exception e) {
            Log.e(TAG, "Error printing label", e);
        }
    }
    
    public void printText(String text) {
        try {
            // Get current settings
            float fontSize = settings.getFontSize();
            int alignment = settings.getTextAlignment();
            
            // Print text
            printer.printerInit();
            printer.setAlignment(getAlignment(alignment));
            printer.setFontSize(getTextSize(fontSize));
            printer.printText(text);
            printer.feedPaper(settings.getLinesPerFeed());
            
            Log.d(TAG, "Text printed successfully");
            Log.d(TAG, "Text: " + text);
            Log.d(TAG, "Font Size: " + fontSize);
            Log.d(TAG, "Alignment: " + alignment);
            
        } catch (Exception e) {
            Log.e(TAG, "Error printing text", e);
        }
    }
} 