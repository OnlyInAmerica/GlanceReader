# Glance-Android

![Glance](http://i.imgur.com/pO9as8z.gif)

Glance is a [RSVP](http://en.wikipedia.org/wiki/Rapid_serial_visual_presentation) .epub and website reader for Android 3.0+ (API 11).

You can share URLS to Glance from your favorite web browser, or open .epubs on your device's external storage directly.

Available on Google Play under the name **Glance**

[![Glance](https://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=pro.dbro.openspritz)

## Lend a hand

Some quick, incomplete thoughts on what's next.

#### Enhancements

+ Find some nice monospace fonts
+ More sophisticated pivot choosing
+ Better handle "Chapters" with epublib, or some other epub library
    + This seems unreasonably hard.

#### Features

+ Read text from clipboard

## Building

0. Make sure you've installed the following from the Android SDK Manager before building:
  	+ Android SDK Build-tools 19.0.2
	+ Android SDK tools 22.3
	+ SDK Platform 19
	+ Android Support Repository 4

1. Define the `ANDROID_HOME` environmental variable as your Android SDK location.

	If you need help check out [this guide](http://spring.io/guides/gs/android/).

3. Build!

  	To build an .apk from this directory, make sure `./gradlew` is executable and run:

    	$ ./gradlew assemble

	The resulting .apk will be availble in `./app/build/apk`.


## Thanks
In no partiular order. Thanks everybody!

+ [andrewgiang](https://github.com/andrewgiang) for contributing a re-usable library based on the early project, and other contributions
+ [defer](https://github.com/defer) for refactoring that made it easier to support multiple formats
+ [epublib](https://github.com/psiegman/epublib) by [psiegman](https://github.com/psiegman) (LGPL)
+ [rcarlsen](https://github.com/rcarlsen) for work adopting for Glass
+ [mneimsky](https://github.com/mneimsky) for work adopting for Glass

## License

GPLv3
