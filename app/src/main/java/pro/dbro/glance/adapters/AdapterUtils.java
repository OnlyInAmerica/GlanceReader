package pro.dbro.glance.adapters;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import pro.dbro.glance.activities.MainActivity;

/**
 * Common functions
 *
 * Created by davidbrodsky on 9/12/14.
 */
public class AdapterUtils {

    /** Intent Keys */

    /** Whether Activity should finish after
     * completing action specified in Intent
     */
    public static final String FINISH_AFTER = "FinishAfter";

    /** Indicates this media is internal to the Glance network and NOT from an external source
     * e.g: Media shared from a web browser
     */
    public static final String IS_INTERNAL_MEDIA = "InternalMedia";

    public static View.OnClickListener getArticleClickListener(final Context c) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent communityIntent = new Intent(c, MainActivity.class);
                communityIntent.setAction(Intent.ACTION_SEND);
                communityIntent.putExtra(Intent.EXTRA_TEXT, (String) view.getTag());
                communityIntent.putExtra(IS_INTERNAL_MEDIA, true);
                communityIntent.putExtra(FINISH_AFTER, true);
                c.startActivity(communityIntent);
            }
        };
    }

    // XXX: If don't have book:
        //      Start book download
        //      Add to local library
        //      Open book
    //  If have book:
        //      Open book
    public static View.OnClickListener getBookClickListener(final Context c) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(c, MainActivity.class);
                bookIntent.setAction(Intent.ACTION_SEND);
                bookIntent.putExtra(Intent.EXTRA_TEXT, (String) view.getTag());
                bookIntent.putExtra(IS_INTERNAL_MEDIA, true);
                bookIntent.putExtra(FINISH_AFTER, true);
                c.startActivity(bookIntent);

                new DownloadManager(c).execute((String) view.getTag(), (String) view.getTag());

            }

            class DownloadManager extends AsyncTask<String, Integer, Drawable> {

                private Drawable d;
                private HttpURLConnection conn;
                private InputStream stream; //to read
                private ByteArrayOutputStream out; //to write
                private Context mCtx;

                private double fileSize;
                private double downloaded; // number of bytes downloaded
                private int status = DOWNLOADING; //status of current process

                private ProgressDialog progressDialog;

                private static final int MAX_BUFFER_SIZE = 1024; //1kb
                private static final int DOWNLOADING = 0;
                private static final int COMPLETE = 1;

                public DownloadManager(Context ctx) {
                    d = null;
                    conn = null;
                    fileSize = 0;
                    downloaded = 0;
                    status = DOWNLOADING;
                    mCtx = ctx;
                }

                public boolean isOnline() {
                    try {
                        ConnectivityManager cm = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
                        return cm.getActiveNetworkInfo().isConnectedOrConnecting();
                    } catch (Exception e) {
                        return false;
                    }
                }

                @Override
                protected Drawable doInBackground(String... url) {
                    try {
                        String filename = url[1];
                        if (isOnline()) {
                            conn = (HttpURLConnection) new URL(url[0]).openConnection();
                            fileSize = conn.getContentLength();
                            out = new ByteArrayOutputStream((int) fileSize);
                            conn.connect();

                            stream = conn.getInputStream();
                            // loop with step
                            while (status == DOWNLOADING) {
                                byte buffer[];

                                if (fileSize - downloaded > MAX_BUFFER_SIZE) {
                                    buffer = new byte[MAX_BUFFER_SIZE];
                                } else {
                                    buffer = new byte[(int) (fileSize - downloaded)];
                                }
                                int read = stream.read(buffer);

                                if (read == -1) {
                                    publishProgress(100);
                                    break;
                                }
                                // writing to buffer
                                out.write(buffer, 0, read);
                                downloaded += read;
                                // update progress bar
                                publishProgress((int) ((downloaded / fileSize) * 100));
                            } // end of while

                            if (status == DOWNLOADING) {
                                status = COMPLETE;
                            }
                            try {
                                FileOutputStream fos = new FileOutputStream(filename);
                                fos.write(out.toByteArray());
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                return null;
                            }

                            //d = Drawable.createFromStream((InputStream) new ByteArrayInputStream(out.toByteArray()), "filename");
                            return d;
                        } // end of if isOnline
                        else {
                            return null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }// end of catch
                } // end of class DownloadManager()

                @Override
                protected void onProgressUpdate(Integer... changed) {
                    //progressDialog.setProgress(changed[0]);
                }

                @Override
                protected void onPreExecute() {
//        progressDialog = new ProgressDialog(); // your activity
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progressDialog.setMessage("Downloading ...");
//        progressDialog.setCancelable(false);
//        progressDialog.show();
                }

                @Override
                protected void onPostExecute(Drawable result) {
                    //progressDialog.dismiss();
                    // do something
                }
            }

        };
    }

}

