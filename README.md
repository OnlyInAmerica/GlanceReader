# OpenSpritz-Android

![OpenSpritz](http://i.imgur.com/UPpz18r.gif)

OpenSpritz-Android is a Spritz-like .epub reader for Android. Inspired by Miserlou's [OpenSpritz](https://github.com/Miserlou/OpenSpritz).

## Lend a hand

+ It'd be nice to digest `http://...` share intents, parse & spritz the resulting page.
+ Progress indicator?
+ More sophisticated pivot choosing.

## Building

Make sure you've installed the following from the Android SDK Manager before building:

+ Android SDK Build-tools 19.0.2
+ Android SDK tools 22.3
+ SDK Platform 19
+ Android Support Repository 4

To build an .apk from this directory, make sure `./gradlew` is executable and run:

    $ ./gradlew assemble
    
The resulting .apk will be availble in `./app/build/apk`.


## Thanks

+ [epublib](https://github.com/psiegman/epublib) by psiegman (LGPL)

#### A Note About the Name

OpenSpritz has nothing to do with [Spritz Incorporated](http://www.spritzinc.com/). This is an open source, community created project, made with love because Spritz is such an awesome technique for reading with.

## License

GPLv3