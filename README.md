# Android Airmote #

Remote Control Application for InAir system (Android)

### How do I get set up? ###

* Download source code. `git clone git@bitbucket.org:seespace/androidairmote.git`
* In Android Studio, open it as a new project
* Build!

### How to run? ###

Note: Since Android Emulator maintains a virtual ethernet LAN, sending events from device to emulator is not possible unless you have a [modified adb](http://rxwen.blogspot.com/2009/11/adb-for-remote-connections.html).

* Start InAir emulator and open an InAir application.
* You need to forward port **8989** on the emulator to same port on the host machine: `adb forward tcp:8989 tcp:8989`
* Start Android emulator and open Airmote application, enter `10.0.2.2` as AirServer IP address.

### Supports ###
* Bug reporting: file an issue in this repo
* Discussions/questions: create new topic at [InAir Developer Forums](http://developer.inair.tv/category/13/remote-control-applications-forum)