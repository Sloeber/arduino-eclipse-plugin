Supported boards
=====
So you have this very exotic board and you wonder if you can use Sloeber to program it.  
[First of all there is a known issue with windows 10 iot boards that I don't see fixed any time soon.](https://github.com/jantje/arduino-eclipse-plugin/issues/530)  

Secondly there is no way anyone can guarantee it will work. What I can tell you is that -at the time of writing- we have a unit test where we create more then 900 projects (using a blank sketch) with all different boards and less then 20 fail to compile. This board also fails in the Arduino IDE.  
[Here is a link to the board definitions in the unit test](https://github.com/jantje/arduino-eclipse-plugin/blob/master/io.sloeber.core/src/jUnit/CreateAndCompile.java#L62)  
  
Mind you: We are not running the unit tests all the time. We mostly just run it when we think we might have broken something and stop it after "enough test to raise our confidence". We do plan to run it before releasing a stable release.  


