SikuliX Version 2
============

**Version 2.0.0 under developement (pre-alpha!)** (not useable yet, massive redesign planned until Oct. 2018)

**[Download](http://sikulix.com) latest ready to use version 1.1.x** [or get the Sources](https://github.com/RaiMan/SikuliX1)

<hr>

Travis CI (Ubuntu 12.04-64, Java 9, xvfb)
<br>Developing and testing locally with latest Java on macOS 10.13 and Windows 10
<br>using [JetBrains IntelliJ IDEA community Edition](https://www.jetbrains.com/idea/)
<br>Translation project: [Transifex::SikuliX2](https://www.transifex.com/sikulix/sikulix2/dashboard/)
<br>Documentation project: [ReadTheDocs::SikuliX2](http://sikulix2.readthedocs.org/) based on [GitHub::SikuliX2-Docs](https://github.com/RaiMan/SikuliX2-Docs)

**Forking and/or downloading this repo only makes sense:**

 - if you want to get a knowledge about the internals of SikuliX
 - if you want to create your own packages containing SikuliX features
 - if you want to contribute.

For use with Java aware scripting and Java programming you might need additional stuff and steps at your own resposibility. 

<hr>

**BE AWARE: Java 8+ required** 

I am developing on latest Java (currently 10)<br>
Source code level and byte code level are both 1.8

**Issues and pull requests are only accepted here on Github**

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
 
