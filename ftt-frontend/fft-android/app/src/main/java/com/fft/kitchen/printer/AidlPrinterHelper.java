package com.fft.kitchen.printer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.IWoyouService;

public class AidlPrinterHelper {
    private static final String TAG = "AidlPrinterHelper";
    private static final String SERVICE_PACKAGE = "woyou.aidlservice.jiuiv5";
    private static final String SERVICE_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService";
    private static final String PREFS_NAME = "PrinterPrefs";
    private static final String LINES_PER_PRINT_KEY = "lines_per_print";

    public enum PrinterStatus {
        DISCONNECTED,
        CONNECTED,
        ERROR
    }

    private static AidlPrinterHelper instance;
    protected IWoyouService printerService;
    private Context context;
    private PrinterCallback printerCallback;
    private boolean isBound = false;
    private SharedPreferences prefs;
    private final Object serviceLock = new Object();
    private PrinterSettings settings;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (serviceLock) {
                Log.d(TAG, "Service connected: " + name);
                printerService = IWoyouService.Stub.asInterface(service);
                isBound = true;
                
                // Initialize printer after connection with retries
                int maxRetries = 3;
                int retryCount = 0;
                boolean initialized = false;
                
                while (retryCount < maxRetries && !initialized) {
                    try {
                        printerInit();
                        initialized = true;
                        Log.d(TAG, "Printer initialized successfully on attempt " + (retryCount + 1));
                    } catch (Exception e) {
                        retryCount++;
                        Log.w(TAG, "Printer initialization attempt " + retryCount + " failed", e);
                        try {
                            Thread.sleep(1000); // Wait 1 second before retry
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                // Notify callback after initialization attempts
                if (printerCallback != null) {
                    if (initialized) {
                        printerCallback.onConnected();
                    } else {
                        printerCallback.onError("Failed to initialize printer after " + maxRetries + " attempts");
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (serviceLock) {
                Log.d(TAG, "Service disconnected: " + name);
                printerService = null;
                isBound = false;
                
                if (printerCallback != null) {
                    printerCallback.onDisconnected();
                }
            }
        }
    };

    private final ICallback callback = new ICallback.Stub() {
        @Override
        public void onRunResult(boolean isSuccess) throws RemoteException {
            Log.d(TAG, "Print operation result: " + isSuccess);
            if (!isSuccess && printerCallback != null) {
                printerCallback.onError("Print operation failed");
            }
        }

        @Override
        public void onReturnString(String result) throws RemoteException {
            Log.d(TAG, "Printer returned string: " + result);
        }

        @Override
        public void onRaiseException(int code, String msg) throws RemoteException {
            Log.e(TAG, "Printer error: " + msg + " (code: " + code + ")");
            if (printerCallback != null) {
                printerCallback.onError("Printer error: " + msg);
            }
        }
    };

    protected AidlPrinterHelper(Context context) {
        // Always use application context to prevent memory leaks
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.settings = PrinterSettings.getInstance(context);
        Log.d(TAG, "Initialized with lines per print: " + settings.getLinesPerFeed());
    }

    public static synchronized AidlPrinterHelper getInstance(Context context) {
        if (instance == null) {
            // Always use application context to prevent memory leaks
            Context appContext = context.getApplicationContext();
            instance = new AidlPrinterHelper(appContext);
        }
        return instance;
    }

    public synchronized void bindService(PrinterCallback callback) {
        Log.d(TAG, "Binding to printer service");
        this.printerCallback = callback;
        
        synchronized (serviceLock) {
            if (isBound && printerService != null) {
                try {
                    // Test if the service is still responsive
                    printerService.asBinder().pingBinder();
                    Log.d(TAG, "Service already bound and responsive");
                    if (printerCallback != null) {
                        printerCallback.onConnected();
                    }
                    return;
                } catch (Exception e) {
                    Log.w(TAG, "Existing service binding is unresponsive", e);
                    // Continue with rebinding
                }
            }
            
            // Cleanup any existing binding
            try {
                if (isBound) {
                    Log.d(TAG, "Cleaning up previous binding");
                    context.unbindService(serviceConnection);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up previous binding", e);
            } finally {
                isBound = false;
                printerService = null;
            }

            try {
                Intent intent = new Intent();
                intent.setPackage(SERVICE_PACKAGE);
                intent.setAction(SERVICE_ACTION);
                // Use flags to ensure service priority
                int flags = Context.BIND_AUTO_CREATE | 
                           Context.BIND_IMPORTANT | 
                           Context.BIND_ABOVE_CLIENT;
                
                boolean bound = context.bindService(intent, serviceConnection, flags);
                Log.d(TAG, "Service binding result: " + bound);
                
                if (!bound && printerCallback != null) {
                    printerCallback.onError("Failed to bind to printer service");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding to printer service", e);
                if (printerCallback != null) {
                    printerCallback.onError("Error binding to printer service: " + e.getMessage());
                }
            }
        }
    }

    public synchronized void unbindService() {
        Log.d(TAG, "Unbinding from printer service");
        synchronized (serviceLock) {
            if (isBound && serviceConnection != null) {
                try {
                    context.unbindService(serviceConnection);
                    Log.d(TAG, "Successfully unbound from printer service");
                } catch (Exception e) {
                    Log.e(TAG, "Error unbinding from printer service", e);
                } finally {
                    isBound = false;
                    printerService = null;
                }
            } else {
                Log.d(TAG, "Service was not bound, no need to unbind");
            }
        }
    }

    public void setPrinterCallback(PrinterCallback callback) {
        this.printerCallback = callback;
    }

    public boolean isConnected() {
        return isBound && printerService != null;
    }

    public void printerInit() {
        Log.d(TAG, "Initializing printer");
        try {
            if (printerService != null) {
                printerService.printerInit(callback);
            } else {
                Log.e(TAG, "Printer service is null during initialization");
                if (printerCallback != null) {
                    printerCallback.onError("Printer service not connected");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to initialize printer", e);
            if (printerCallback != null) {
                printerCallback.onError("Failed to initialize printer: " + e.getMessage());
            }
        }
    }

    public void printText(String text) {
        Log.d(TAG, "Printing text: " + text);
        try {
            if (printerService != null) {
                // Apply current font settings before printing
                applyFontSettings();
                
                // Print the text and feed the paper
                printerService.printText(text, callback);
                feedPaper(settings.getLinesPerFeed()); // Use our feedPaper method which handles float values
            } else {
                Log.e(TAG, "Printer service is null during text printing");
                if (printerCallback != null) {
                    printerCallback.onError("Printer service not connected");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to print text", e);
            if (printerCallback != null) {
                printerCallback.onError("Failed to print text: " + e.getMessage());
            }
        }
    }

    public void printTextWithSize(String text, float size) {
        Log.d(TAG, "Printing text with size: " + text + " (size: " + size + ")");
        try {
            if (printerService != null) {
                printerService.setFontSize(size, callback);
                printerService.printText(text, callback);
                printerService.setFontSize(settings.getFontSize(), callback); // Reset to default font size
                feedPaper(settings.getLinesPerFeed()); // Use our feedPaper method which handles float values
            } else {
                Log.e(TAG, "Printer service is null during text printing with size");
                if (printerCallback != null) {
                    printerCallback.onError("Printer service not connected");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to print text with size", e);
            if (printerCallback != null) {
                printerCallback.onError("Failed to print text with size: " + e.getMessage());
            }
        }
    }

    public void printQRCode(String data, int size) {
        Log.d(TAG, "Printing QR code: " + data + " (size: " + size + ")");
        try {
            if (printerService != null) {
                printerService.printQRCode(data, size, 2, callback);
                feedPaper(settings.getLinesPerFeed()); // Use our feedPaper method which handles float values
            } else {
                Log.e(TAG, "Printer service is null during QR code printing");
                if (printerCallback != null) {
                    printerCallback.onError("Printer service not connected");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to print QR code", e);
            if (printerCallback != null) {
                printerCallback.onError("Failed to print QR code: " + e.getMessage());
            }
        }
    }

    public void printBarCode(String data) {
        Log.d(TAG, "Printing barcode: " + data);
        try {
            if (printerService != null) {
                printerService.printBarCode(data, 8, 80, 2, 0, callback);
                feedPaper(settings.getLinesPerFeed()); // Use our feedPaper method which handles float values
            } else {
                Log.e(TAG, "Printer service is null during barcode printing");
                if (printerCallback != null) {
                    printerCallback.onError("Printer service not connected");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to print barcode", e);
            if (printerCallback != null) {
                printerCallback.onError("Failed to print barcode: " + e.getMessage());
            }
        }
    }

    public void printBitmap(Bitmap bitmap) {
        Log.d(TAG, "Printing bitmap");
        try {
            if (printerService != null) {
                printerService.printBitmap(bitmap, callback);
                feedPaper(settings.getLinesPerFeed()); // Use our feedPaper method which handles float values
            } else {
                Log.e(TAG, "Printer service is null during bitmap printing");
                if (printerCallback != null) {
                    printerCallback.onError("Printer service not connected");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to print bitmap", e);
            if (printerCallback != null) {
                printerCallback.onError("Failed to print bitmap: " + e.getMessage());
            }
        }
    }

    public void setAlignment(int alignment) {
        Log.d(TAG, "Setting alignment: " + alignment);
        try {
            if (printerService != null) {
                printerService.setAlignment(alignment, callback);
            } else {
                Log.e(TAG, "Printer service is null during alignment setting");
                if (printerCallback != null) {
                    printerCallback.onError("Printer service not connected");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set alignment", e);
            if (printerCallback != null) {
                printerCallback.onError("Failed to set alignment: " + e.getMessage());
            }
        }
    }

    public void cutPaper() {
        Log.d(TAG, "Cutting paper with minimal feed");
        try {
            if (printerService != null) {
                // Some printer implementations automatically feed paper before cutting
                // Try to find a way to minimize this behavior
                
                // First, try partial cut (mode 1) which may use less paper
                try {
                    // Use standard callback interface
                    printerService.cutPaper(new ICallback.Stub() {
                        @Override
                        public void onRunResult(boolean isSuccess) {
                            Log.d(TAG, "Cut paper result: " + isSuccess);
                        }

                        @Override
                        public void onReturnString(String result) {}

                        @Override
                        public void onRaiseException(int code, String msg) {
                            Log.e(TAG, "Error cutting paper: " + msg);
                        }
                    });
                } catch (Exception e) {
                    // If that fails, try the regular cut method
                    Log.e(TAG, "Error with custom cut, using standard method: " + e.getMessage());
                    printerService.cutPaper(callback);
                }
            } else {
                Log.e(TAG, "Printer service is null during paper cutting");
                if (printerCallback != null) {
                    printerCallback.onError("Printer service not connected");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to cut paper", e);
            if (printerCallback != null) {
                printerCallback.onError("Failed to cut paper: " + e.getMessage());
            }
        }
    }

    public void feedPaper() {
        Log.d(TAG, "Feeding paper with default lines");
        feedPaper(settings.getLinesPerFeed());
    }

    public void feedPaper(float lines) {
        try {
            if (printerService != null) {
                Log.d(TAG, "Feeding paper with " + lines + " lines");
                int wholeLines = Math.max(1, (int) Math.ceil(lines));  // Use at least 1 line, and round up
                printerService.lineWrap(wholeLines, callback);
            } else {
                Log.e(TAG, "Printer service is null during paper feeding");
                if (printerCallback != null) {
                    printerCallback.onError("Printer service not connected");
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to feed paper", e);
            if (printerCallback != null) {
                printerCallback.onError("Failed to feed paper: " + e.getMessage());
            }
        }
    }
    
    // Apply font settings from PrinterSettings
    private void applyFontSettings() throws RemoteException {
        if (printerService != null) {
            float fontSize = settings.getFontSize();
            printerService.setFontSize(fontSize, callback);
            
            // Note: Actual font type setting would go here if the printer supports it
            // This is hardware dependent
        }
    }

    public float getLinesPerPrint() {
        return settings.getLinesPerFeed();
    }

    public void setLinesPerPrint(float lines) {
        settings.setLinesPerFeed((int)lines);
    }
    
    public float getFontSize() {
        return settings.getFontSize();
    }
    
    public void setFontSize(float size) {
        settings.setFontSize((int)size);
    }
    
    public String getFontName() {
        return settings.getFontName();
    }
    
    public void setFontName(String fontName) {
        settings.setFontName(fontName);
    }
    
    public int getLabelWidth() {
        return settings.getLabelWidth();
    }
    
    public void setLabelWidth(int width) {
        settings.setLabelWidth(width);
    }
    
    public int getLabelHeight() {
        return settings.getLabelHeight();
    }
    
    public void setLabelHeight(int height) {
        settings.setLabelHeight(height);
    }
    
    public String getCurrentProfile() {
        return settings.getCurrentProfile();
    }
    
    public void setCurrentProfile(String profile) {
        settings.setCurrentProfile(profile);
    }

    private float getLinesPerPrintFromPreferences() {
        // Safely retrieve the lines per print from shared preferences
        // This handles the case where the value was previously stored as an integer
        try {
            return prefs.getFloat(LINES_PER_PRINT_KEY, 3.0f);
        } catch (ClassCastException e) {
            // If previously stored as an integer, convert it
            int intValue = prefs.getInt(LINES_PER_PRINT_KEY, 3);
            // Save it as a float for future use
            setLinesPerPrint((float) intValue);
            return (float) intValue;
        }
    }

    public interface PrinterCallback {
        void onConnected();
        void onDisconnected();
        void onError(String message);
    }
} 