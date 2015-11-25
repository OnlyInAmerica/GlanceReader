
package pro.dbro.glance.tts;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import pro.dbro.glance.lib.Spritzer;
import pro.dbro.glance.lib.SpritzerTextView;

import java.util.concurrent.Semaphore;

/**
 * Implements the Flite Engine as a TextToSpeechService
 *
 */

@TargetApi(14)
public class GlanceTtsService extends TextToSpeechService {
	private final static String LOG_TAG = "Flite_Java_" + GlanceTtsService.class.getSimpleName();

	private static final String DEFAULT_LANGUAGE = "eng";
	private static final String DEFAULT_COUNTRY = "";
	private static final String DEFAULT_VARIANT = "";

	private String mCountry = DEFAULT_COUNTRY;
	private String mLanguage = DEFAULT_LANGUAGE;
	private String mVariant = DEFAULT_VARIANT;
	private Semaphore sem = new Semaphore(0);
	private Object mAvailableVoices;
	private SynthesisCallback mCallback;

	private Util util = new Util();

	private HandlerThread ht = new HandlerThread("HELPER");

	private Spritzer.SpritzerCallback mSplitzerCallback = new Spritzer.SpritzerCallback() {
     @Override
     public void onSpritzerFinished() {
         finish(false);
     }
 };

	@Override
	public void onCreate() {
		initializeFliteEngine();

		ht.start();

		// This calls onIsLanguageAvailable() and must run after Initialization
		super.onCreate();

		IntentFilter receiveFilter = new IntentFilter("finishTTS");

		LocalBroadcastManager.getInstance(this).
				registerReceiver(mBroadcastReceiver, receiveFilter);

	}

	private void initializeFliteEngine() {
	}

	@Override
	protected String[] onGetLanguage() {
		Log.v(LOG_TAG, "onGetLanguage");
		return new String[] {
				mLanguage, mCountry, mVariant
		};
	}

	@Override
	protected int onIsLanguageAvailable(String language, String country, String variant) {
		Log.v(LOG_TAG, "onIsLanguageAvailable"+language+country+variant);
		return TextToSpeech.LANG_AVAILABLE;//mEngine.isLanguageAvailable(language, country, variant);
	}

	@Override
	protected int onLoadLanguage(String language, String country, String variant) {
		Log.v(LOG_TAG, "onLoadLanguage"+language+country+variant);
		return TextToSpeech.LANG_AVAILABLE;//mEngine.isLanguageAvailable(language, country, variant);
	}

	@Override
	protected void onStop() {

		Log.v(LOG_TAG, "onStop");
	}

	@Override
	protected synchronized void onSynthesizeText(
			SynthesisRequest request, SynthesisCallback callback) {
		Log.v(LOG_TAG, "onSynthesize");

		String language = request.getLanguage();
		String country = request.getCountry();
		String variant = request.getVariant();
		final String text = request.getText();
		Integer speechrate = request.getSpeechRate();

		Log.v(LOG_TAG, text);

		boolean result = true;

		if (! ((mLanguage == language) &&
				(mCountry == country) &&
				(mVariant == variant ))) {
			//result = mEngine.setLanguage(language, country, variant);
			mLanguage = language;
			mCountry = country;
			mVariant = variant;
		}

		if (!result) {
			Log.e(LOG_TAG, "Could not set language for synthesis");
			return;
		}

		mCallback = callback;

		Handler hd = new Handler(ht.getLooper());

		hd.post(new Runnable() {
			@Override
			public void run() {

				SpritzerTextView spritzerTextView = (SpritzerTextView) util.generateView(GlanceTtsService.this);
				spritzerTextView.getSpritzer().setTextAndStart(text, mSplitzerCallback, true);


			}
		});

		sem = new Semaphore(0);

		try {
			sem.acquire();
		} catch (InterruptedException e) {
		}
		mCallback.done();
	}

	/**
	 * Listens for language update broadcasts and initializes the flite engine.
	 */
	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Handler hd = new Handler(ht.getLooper());
			hd.post(new Runnable() {
				@Override
				public void run() {
					finish(true);
				}
			});
		}
	};

	private void finish(boolean b) {
		if ( mCallback != null ) {
			util.removeView(this);
			sem.release();
		}
	}
}
