# FileLogger
A FileLogger based on Timber implementation. Support for printing Log to local files.  

Step 1. Add it in your root build.gradle at the end of repositories:  
```
allprojects {
  repositories {  
    ...  
    maven { url 'https://jitpack.io' }  
  }  
}  
```
Step 2. Add the dependency  
```
dependencies {  
  compile 'com.github.GershonLin:FileLogger:v1.0.0'  
}
```
