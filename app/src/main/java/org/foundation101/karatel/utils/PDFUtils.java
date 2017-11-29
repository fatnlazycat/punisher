package org.foundation101.karatel.utils;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by Dima on 27.11.2017.
 */

@TargetApi(21)
public class PDFUtils {
    public static String TAG = "PDFUtils";

    public static String contentForWebView(File file) {
        PdfRenderer pdfRenderer = null;
        ParcelFileDescriptor fileDescriptor = null;
        Bitmap bitmap = null;
        String result = "";
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

            pdfRenderer = new PdfRenderer(fileDescriptor);

            if (pdfRenderer.getPageCount() > 0) {
                PdfRenderer.Page page = pdfRenderer.openPage(0);
                bitmap = bitmapFromPdfPage(page);
                page.close();

                result = "data:image/png;base64," + encodeBitmapBase64(bitmap);
            }
        } catch (IOException e) {
            Log.e(TAG, "error in contentForWebView", e);
        } finally {
            if (pdfRenderer != null) pdfRenderer.close();
            if (bitmap      != null) bitmap     .recycle();
            try {
                if (fileDescriptor != null) fileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "again error in contentForWebView - can't close fileDescriptor", e);
            }
        }
        return result;
    }

    private static Bitmap bitmapFromPdfPage(PdfRenderer.Page page) {
        int rendererPageWidth = page.getWidth();
        int rendererPageHeight = page.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(
                rendererPageWidth,
                rendererPageHeight,
                Bitmap.Config.ARGB_8888);

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        return bitmap;
    }

    private static String encodeBitmapBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

        return imageBase64;
    }
}
