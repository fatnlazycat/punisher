package org.foundation101.karatel.utils;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by Dima on 27.11.2017.
 */

@TargetApi(21)
public class PDFUtils {
    public static final String TAG = "PDFUtils";
    private static final String WEB_VIEW_CONTENT_START = "<img src='data:image/png;base64,";
    private static final String WEB_VIEW_CONTENT_END = "'/>";
    private static final int WEB_VIEW_SOURCE_MAX_LENGTH = 2097152; //otherwise WebView won't load the source
    private static final int AVAILABLE_LENGTH = WEB_VIEW_SOURCE_MAX_LENGTH
            - WEB_VIEW_CONTENT_START.length()
            - WEB_VIEW_CONTENT_END.length();

    public static String contentForWebView(File file) {
        PdfRenderer pdfRenderer = null;
        ParcelFileDescriptor fileDescriptor = null;
        String result = "";
        try {
            //debug block
            // file = new File("/storage/emulated/0/Download/2562_GERVAIS_SCIENTIST-0225.pdf");

            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

            pdfRenderer = new PdfRenderer(fileDescriptor);

            if (pdfRenderer.getPageCount() > 0) {
                PdfRenderer.Page page = pdfRenderer.openPage(0);
                result = getContentData(page, 1);
                page.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "error in contentForWebView", e);
        } finally {
            if (pdfRenderer != null) pdfRenderer.close();
            try {
                if (fileDescriptor != null) fileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "again error in contentForWebView - can't close fileDescriptor", e);
            }
        }
        return result;
    }

    private static Bitmap bitmapFromPdfPage(PdfRenderer.Page page, int scaleSize) {
        int rendererPageWidth  = page.getWidth()  / scaleSize;
        int rendererPageHeight = page.getHeight() / scaleSize;

        Bitmap bitmap = Bitmap.createBitmap(
                rendererPageWidth,
                rendererPageHeight,
                Bitmap.Config.ARGB_8888);

        float scaleRatio = (float) (1.0 / scaleSize);
        Matrix matrix = new Matrix();
        matrix.setScale(scaleRatio, scaleRatio);

        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        return bitmap;
    }

    private static String encodeBitmapBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //PNG cannot be compressed with less quality, but JPEG is not suitable either hence it converts transparent background to black
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
        return imageBase64;
    }

    private static String getContentData(PdfRenderer.Page page, int bitmapScaleSize) {
        try {
            /*//debug block
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = bitmapScaleSize;
            Bitmap bitmap = BitmapFactory.decodeFile("/storage/emulated/0/Download/2562_GERVAIS_SCIENTIST-0225.jpg", options);*/

            Bitmap bitmap = bitmapFromPdfPage(page, bitmapScaleSize);
            String result = WEB_VIEW_CONTENT_START + encodeBitmapBase64(bitmap)+ WEB_VIEW_CONTENT_END;
            bitmap.recycle();
            if (result.length() > AVAILABLE_LENGTH) throw new OutOfMemoryError();
            return result;
        } catch (OutOfMemoryError e) {
            return getContentData(page, ++bitmapScaleSize);
        }
    }
}
