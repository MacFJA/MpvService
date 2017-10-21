# MpvService

MpvService allow you the communicate with [Mpv Media Player](https://mpv.io/). 

## Installation<a id="installation"></a>

Clone the project:
```
git clone https://github.com/MacFJA/MpvService.git
```
Install the project into your local Maven repository:
```
cd MpvService/
mvn clean
mvn install
```
Remove the source:
```
cd ..
rm -r MpvService/
```
Add the dependency in your Maven project:
```xml
<project>
    <!-- ... -->
    <dependencies>
        <!-- ... -->
        <dependency>
            <groupId>io.github.macfja</groupId>
            <artifactId>mpv</artifactId>
            <version>0.1.0</version>
        </dependency>
        <!-- ... -->
    </dependencies>
    <!-- ... -->
</project>
```
