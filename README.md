# MpvService

MpvService allow you the communicate with [Mpv Media Player](https://mpv.io/) with the use of JSON-IPC interface. 

## Installation

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
            <version>0.2.0</version>
        </dependency>
        <!-- ... -->
    </dependencies>
    <!-- ... -->
</project>
```

## Usage

```java
import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.Service;
import io.github.macfja.mpv.communication.handling.PropertyObserver;
import io.github.macfja.mpv.wrapper.Shorthand;

import java.io.IOException;

class App {
    public static void main(String[] args) {
        Shorthand mpv = new Shorthand(new Service());

        // Register the modification of the metadata
        // Indicate that the media changed
        try {
            mpv.registerPropertyChange(new PropertyObserver("metadata") {
                @Override
                public void changed(String propertyName, Object value, Integer id) {
                    JSONObject metadata = (JSONObject) value;
                    System.out.println(String.format(
                            "Playing %s by %s",
                            metadata.getString("title"),
                            metadata.getString("artist")
                    ));
                }
            });
        } catch (IOException e) {
            System.err.println("Unable to register property change!");
        }

        // Add media files to the current playlist
        try {
            mpv.addMedia("path/to/a/media.mp3", true);
            mpv.addMedia("path/another/media.mp3", true);
            mpv.addMedia("path/to/the/third/media.mp3", true);
        } catch (IOException e) {
            System.err.println("Unable to add media!");
        }

        // Start playing
        try {
            mpv.play();
        } catch (IOException e) {
            System.err.println("Unable to start playback!");
        }
        
        // Wait the end of the first file
        mpv.waitForEvent("end-file", 5 * 60 * 1000); // timeout to 5 minutes
    }
}
```

## Other implementation of mpv IPC

 - https://github.com/gustaebel/python-mpv (Pyhton3)
 - https://github.com/momomo5717/emms-player-simple-mpv (Emacs Lisp)
 - https://github.com/kljohann/mpv.el (Emacs Lisp)
 - https://github.com/siikamiika/mpv-python-ipc (Python)
 - https://github.com/DexterLB/mpvipc (Go)
 - https://github.com/gregadams4/mpvjson (Go)
 - https://github.com/mibli/mpvctl (Bash)
 - https://github.com/Syndim/rust-mpv-ipc (Rust)
 - https://github.com/enzzc/simplempv (Python)
 - https://gitlab.com/mpv-ipc/ncmpvc (Shell / ncurses)
 - https://gitlab.com/mpv-ipc/mpvipc (Rust)
 - https://gitlab.com/mpv-ipc/mpvc (Rust)

## License

The MIT License (MIT). Please see [License File](LICENSE.md) for more information.