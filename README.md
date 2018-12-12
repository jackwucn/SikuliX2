SikuliX Version 2
============

**Version 2.0.0 under developement (pre-alpha!)** (not useable yet, massive redesign planned for 2019 ;-)

**[Read about the latest ready to use version 1.1.x](http://sikulix.com)**

<hr>

**Forking and/or downloading this repo only makes sense:**

 - if you want to get a knowledge about the internals of SikuliX
 - if you want to create your own packages containing SikuliX features
 - if you want to contribute (what currently does not really make sense)

For use with Java aware scripting and Java programming you might need additional stuff and steps at your own resposibility. 

<hr>

**BE AWARE: Java 8+ required** 

I am developing on latest Java (currently 11)<br>
Source code level and byte code level are both 1.8

**Issues and pull requests are only accepted here on Github, but currently ignored**

<hr>

**Redesign goal**

Implement the API completely as a REST-API backed by a server running on the target machine.

This will allow many more client implementations even in a browser context.

For backward compatibility the version 1 API will be reimplemented based on this REST-API.
 
Prerequisites for development and testing
---

 - a Java JDK 1.8+
 - Maven 3+
 - only 64-Bit Systems supported

**For developement I use the [JetBrains IDEs](https://www.jetbrains.com)**

 - **[IntelliJ IDEA CE](https://www.jetbrains.com/idea/)** for Java and everything else
 - **[PyCharm CE](https://www.jetbrains.com/pycharm/)** for special Jython/Python stuff
 - **[RubyMine](https://www.jetbrains.com/ruby/)** for special JRuby/Ruby stuff (special license for OpenSource projects)
 
