package pro.dbro.glance.http;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.koushikdutta.ion.Ion;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import pro.dbro.glance.R;

/**
 * Created by davidbrodsky on 9/23/14.
 */
public class TrustManager {

    private static final int sCertResId = R.raw.apidiffbot;
    public static final String sIonInstanceName = "ohMyIon";
    private static Pair<javax.net.ssl.TrustManager[], SSLContext> sTrustResult;

    public interface TrustRequestCallback {
        public void onSuccess(JsonObject result);
    }

    public static void setupIonTrustManager(Context context) {
        if (sTrustResult == null) sTrustResult = setupTrustManagement(context);

        Ion.getInstance(context, sIonInstanceName).getHttpClient().getSSLSocketMiddleware().setTrustManagers(sTrustResult.first);
        Ion.getInstance(context, sIonInstanceName).getHttpClient().getSSLSocketMiddleware().setSSLContext(sTrustResult.second);
    }

    public static void makeTrustRequest(Context context, final String urlStr, final TrustRequestCallback cb) {
        if (sTrustResult == null) sTrustResult = setupTrustManagement(context);
        new AsyncTask<Void, Void, JsonObject>() {

            @Override
            protected JsonObject doInBackground(Void... params) {
                try {
                    URL url = new URL(urlStr);
                    HttpsURLConnection urlConnection = null;
                    urlConnection = (HttpsURLConnection)url.openConnection();
                    urlConnection.setSSLSocketFactory(sTrustResult.second.getSocketFactory());
                    InputStream in = urlConnection.getInputStream();

                    final Gson gson = new Gson();
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    return gson.fromJson(reader, JsonObject.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JsonObject json) {
                cb.onSuccess(json);
                super.onPostExecute(json);
            }
        }.execute();
    }

    private static Pair<javax.net.ssl.TrustManager[], SSLContext> setupTrustManagement(Context context) {
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
            InputStream inStream = context.getResources().openRawResource(sCertResId);
            X509Certificate caCertificate = (X509Certificate)cf.generateCertificate(inStream);

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", caCertificate);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            return new Pair<javax.net.ssl.TrustManager[], SSLContext>(tmf.getTrustManagers(), sslContext);
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

//    private static String getStringFromStream(InputStream is) throws IOException {
//        BufferedReader r = new BufferedReader(new InputStreamReader(is));
//        StringBuilder total = new StringBuilder();
//        String line;
//        while ((line = r.readLine()) != null) {
//            total.append(line);
//        }
//        Log.i("NETWORK", "Got string from response " + total.toString());
//        return total.toString();
//    }
}
