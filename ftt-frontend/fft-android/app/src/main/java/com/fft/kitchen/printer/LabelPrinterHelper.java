package com.fft.kitchen.printer;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.util.Log;

public class LabelPrinterHelper extends AidlPrinterHelper {
    // Reduce default feed lines from 3 to 1.5 to save paper
    private static final float DEFAULT_LABEL_FEED_LINES = 1.5f;
    private static final String TAG = "LabelPrinterHelper";

    public LabelPrinterHelper(Context context) {
        super(context);
    }

    /**
     * Prints a complete label with the given content
     * @param content Text content to print on the label
     */
    public void printLabel(String content) {
        printLabel(content, true); // Default to use configured lines per feed
    }
    
    /**
     * Prints a complete label with the given content and control over lines feed settings
     * @param content Text content to print on the label
     * @param useConfiguredLinesPerFeed Whether to use the lines per feed from shared preferences
     */
    public void printLabel(String content, boolean useConfiguredLinesPerFeed) {
        Log.d(TAG, "Printing label: " + content);
        
        // Get current lines setting
        float currentLines = useConfiguredLinesPerFeed ? getLinesPerPrint() : DEFAULT_LABEL_FEED_LINES;
        
        // Enforce a reasonable maximum to prevent excessive paper usage
        if (currentLines > 3.0f) {
            Log.d(TAG, "Capping excessive lines per feed value: " + currentLines + " to 1.5");
            currentLines = 1.5f;
        }
        
        Log.d(TAG, "Using lines per feed: " + currentLines + (useConfiguredLinesPerFeed ? " (from settings)" : " (default)"));
        
        try {
            if (!isConnected()) {
                Log.e(TAG, "Printer not connected");
                throw new Exception("Printer not connected");
            }
            
            printerInit();
            
            // Set alignment to center
            setAlignment(1); // 0=left, 1=center, 2=right
            
            // Split the content by lines for better formatting control
            String[] lines = content.split("\n");
            for (String line : lines) {
                // Check for formatting indicators (could enhance with more complex formatting)
                if (line.contains("====")) {
                    // Print separator line
                    printText(line);
                } else if (line.trim().startsWith("PRODUCT:")) {
                    // Left-align product information
                    setAlignment(0);
                    printText(line);
                } else if (line.contains("PRODUCT LABEL")) {
                    // Center the header
                    setAlignment(1);
                    printTextWithSize(line, 28); // Larger text for header
                } else {
                    // Default printing for other lines
                    setAlignment(0); // Left align regular content
                    printText(line);
                }
            }
            
            // Feed paper and cut - use our controlled lines value
            feedPaper(currentLines);
            cutPaper();
            
            Log.d(TAG, "Label printed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error printing label: " + e.getMessage(), e);
            throw new RuntimeException("Error printing label: " + e.getMessage(), e);
        }
    }

    /**
     * Prints a label with a bitmap image
     * @param bitmap Image to print on the label
     */
    public void printLabelWithImage(Bitmap bitmap) {
        printLabelWithImage(bitmap, true); // Default to use configured lines per feed
    }
    
    /**
     * Prints a label with a bitmap image and control over lines feed settings
     * @param bitmap Image to print on the label
     * @param useConfiguredLinesPerFeed Whether to use the lines per feed from shared preferences
     */
    public void printLabelWithImage(Bitmap bitmap, boolean useConfiguredLinesPerFeed) {
        Log.d(TAG, "Printing label with image");
        
        // Get current lines setting
        float currentLines = useConfiguredLinesPerFeed ? getLinesPerPrint() : DEFAULT_LABEL_FEED_LINES;
        
        // Enforce a reasonable maximum to prevent excessive paper usage
        if (currentLines > 3.0f) {
            Log.d(TAG, "Capping excessive lines per feed value: " + currentLines + " to 1.5");
            currentLines = 1.5f;
        }
        
        Log.d(TAG, "Using lines per feed: " + currentLines + (useConfiguredLinesPerFeed ? " (from settings)" : " (default)"));
        
        try {
            if (!isConnected()) return;
            
            printerInit();
            printBitmap(bitmap);
            feedPaper(currentLines);
            cutPaper();
        } catch (Exception e) {
            Log.e(TAG, "Error printing label with image: " + e.getMessage(), e);
        }
    }

    public void printLabelContent(String text, Bitmap image) {
        printLabelContent(text, image, true); // Default to use configured lines per feed
    }
    
    /**
     * Prints a label with text and image content and control over lines feed settings
     * @param text Text content to print
     * @param image Image content to print
     * @param useConfiguredLinesPerFeed Whether to use the lines per feed from shared preferences
     */
    public void printLabelContent(String text, Bitmap image, boolean useConfiguredLinesPerFeed) {
        Log.d(TAG, "Printing label with text and image");
        
        // Get current lines setting
        float currentLines = useConfiguredLinesPerFeed ? getLinesPerPrint() : DEFAULT_LABEL_FEED_LINES;
        
        // Enforce a reasonable maximum to prevent excessive paper usage
        if (currentLines > 3.0f) {
            Log.d(TAG, "Capping excessive lines per feed value: " + currentLines + " to 1.5");
            currentLines = 1.5f;
        }
        
        Log.d(TAG, "Using lines per feed: " + currentLines + (useConfiguredLinesPerFeed ? " (from settings)" : " (default)"));
        
        try {
            if (!isConnected()) return;
            
            printerInit();
            
            if (image != null) {
                printBitmap(image);
            }
            
            if (text != null && !text.isEmpty()) {
                printText(text);
            }
            
            feedPaper(currentLines);
            cutPaper();
        } catch (Exception e) {
            Log.e(TAG, "Error printing label content: " + e.getMessage(), e);
        }
    }

    public void printLabelContent(String text) {
        printLabelContent(text, null, true); // Default to use configured lines per feed
    }

    public void printLabelContent(Bitmap image) {
        printLabelContent(null, image, true); // Default to use configured lines per feed
    }

    /**
     * Prints text with specific font size without adding extra line feeds
     * This allows for more compact printing with manual spacing control
     * 
     * @param text The text to print
     * @param size The font size to use
     */
    public void printTextWithSize(String text, float size) {
        Log.d(TAG, "MINIMAL PAPER MODE: Printing text with minimal spacing: " + text);
        try {
            if (!isConnected()) {
                Log.e(TAG, "Printer not connected");
                return;
            }
            
            // Save current font size
            float originalSize = getFontSize();
            
            // Set the new font size
            setFontSize(size);
            
            // Print the text with absolute minimal spacing
            // Add only a single newline character - no extra padding
            if (printerService != null) {
                // Important: Do not add multiple newlines!
                String optimizedText = text;
                
                // Ensure there's only one newline at the end
                while (optimizedText.endsWith("\n\n")) {
                    optimizedText = optimizedText.substring(0, optimizedText.length() - 1);
                }
                
                // Make sure we have exactly one newline at the end
                if (!optimizedText.endsWith("\n")) {
                    optimizedText += "\n";
                }
                
                printerService.printText(optimizedText, new woyou.aidlservice.jiuiv5.ICallback.Stub() {
                    @Override
                    public void onRunResult(boolean isSuccess) { }
                    
                    @Override
                    public void onReturnString(String result) { }
                    
                    @Override
                    public void onRaiseException(int code, String msg) {
                        Log.e(TAG, "Error printing text with size: " + msg);
                    }
                });
            } else {
                Log.e(TAG, "Printer service is null during compact text printing");
                throw new Exception("Printer service not connected");
            }
            
            // Restore original font size
            setFontSize(originalSize);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to print text with size (compact): " + e.getMessage(), e);
        }
    }

    public interface PrinterCallback extends AidlPrinterHelper.PrinterCallback {
        void onCalibrationComplete();
    }
}