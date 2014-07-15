# Android Airmote #

Remote Control Application for InAir system (Android)

### How to build? ###

Note: You can download the prebuilt apk [here](http://developer.inair.tv/upload_file/attachment/airmote.apk)

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

### Credits ###

**[AndroidProgressHUD](https://github.com/anupamdhanuka/AndroidProgressHUD)**

Credit to [@anupamdhanuka](https://github.com/anupamdhanuka)

### License ###

The MIT License

Copyright (c) 2014, SeeSpace. All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.