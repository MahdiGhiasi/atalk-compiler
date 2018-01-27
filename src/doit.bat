@echo off
echo Compiling HerbertPass1.g4 ...
java org.antlr.v4.Tool HerbertPass1.g4
echo Compiling HerbertPass2.g4 ...
java org.antlr.v4.Tool HerbertPass2.g4
echo Cleaning .antlr files (if any)...
del .antlr\*.java
del .antlr\*.class

echo Done!
pause