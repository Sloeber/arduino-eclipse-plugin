All os'es
==

Arduino IDE **1.5.2** beta and **1.5.4** beta are **not supported** due to a change in the library system.

Arduino IDE **1.5.5** beta works with V2.2 but you **need to fix** the remaining V1.5 Libraries (see library madness)

Arduino IDE **1.5.6** beta switched from **RXTX to JSSC**. This means that the RXTX dll is no longer delivered as part of the Arduino IDE. The plugin comes with a RXTX library which does not contain the adaptations the Arduino team did which improves user experience. (mostly windows and mac)
V2 has also changed to JSSC but this version has not yet been released. You need the nightly for that.

Arduino IDE **1.5.7** comes with a **new toolchain** and no longer includes the make utility. Most linux machines will not be affected but it gives problems in windows and mac. See the platform dependent info on how to get this fixed.

Arduino IDE **1.6.0** can deliver problems in windows

Arduino IDE **1.6.2** does not work. [See why on github.](https://github.com/arduino/Arduino/issues/2982)

Arduino IDE **1.6.3** does not work. [See why on github.](https://github.com/arduino/Arduino/issues/2982)

Arduino IDE **1.6.4** does not work. [See why on github.](https://github.com/arduino/Arduino/issues/2982)