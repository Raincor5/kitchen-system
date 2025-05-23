package woyou.aidlservice.jiuiv5;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.TransBean;
import android.graphics.Bitmap;

interface IWoyouService {
    void updateFirmware();
    int getFirmwareStatus();
    String getServiceVersion();
    void printerInit(in ICallback callback);
    void printerSelfChecking(in ICallback callback);
    String getPrinterSerialNo();
    String getPrinterVersion();
    String getPrinterModal();
    void getPrintedLength(in ICallback callback);
    void lineWrap(int n, in ICallback callback);
    void sendRAWData(in byte[] data, in ICallback callback);
    void setAlignment(int alignment, in ICallback callback);
    void setFontName(String typeface, in ICallback callback);
    void setFontSize(float fontsize, in ICallback callback);
    void printText(String text, in ICallback callback);
    void printTextWithFont(String text, String typeface, float fontsize, in ICallback callback);
    void printColumnsText(in String[] colsTextArr, in int[] colsWidthArr, in int[] colsAlign, in ICallback callback);
    void printBitmap(in Bitmap bitmap, in ICallback callback);
    void printBarCode(String data, int symbology, int height, int width, int textposition, in ICallback callback);
    void printQRCode(String data, int modulesize, int errorlevel, in ICallback callback);
    void printOriginalText(String text, in ICallback callback);
    void commitPrint(in TransBean[] transbean, in ICallback callback);
    void commitPrinterBuffer();
    void enterPrinterBuffer(in boolean clean);
    void exitPrinterBuffer(in boolean commit);
    void printColumnsString(in String[] colsTextArr, in int[] colsWidthArr, in int[] colsAlign, in ICallback callback);
    void updatePrinterState();
    int getPrinterState();
    void cutPaper(in ICallback callback);
    void openDrawer(in ICallback callback);
    int getOpenDrawerTimes();
    void sendLCDCommand(in int flag);
    void sendLCDString(String string, in ICallback callback);
    void sendLCDBitmap(in Bitmap bitmap, in ICallback callback);
    void commitPrinterBufferWithCallback(in ICallback callback);
    void exitPrinterBufferWithCallback(in boolean commit, in ICallback callback);
    void sendLCDDoubleString(String topText, String bottomText, in ICallback callback);
    void printBitmapCustom(in Bitmap bitmap, in int type, in ICallback callback);
} 