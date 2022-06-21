# cornroot

🎶 A music system for minecraft paper

## Setup

After installing the plugin (and running it once) a new directory will be added to the plugins folder. It will contain
the config file and the songs subdirectory. The config file had comments to explain how it works. For the songs they
have to be in the new [Note Block Studio](https://opennbs.org) file format (v4+).

When starting the server you should see the song files being loaded like this:

![idea64_tI3fNWirP1](https://user-images.githubusercontent.com/50306817/174854670-6cdd6e2b-25e5-48df-9dcb-2ca0aa2fcf6d.png)

For some nbs files you can check out [this](https://github.com/astrogue/Songs) github repo. These files are in an older
version of the nbs file format and need to be converted. This can be done with the following python script. When running
this make sure there is a directory named `upgraded` in the current DIR for the upgraded files to be saved to. And you
need the [pynbs](https://github.com/OpenNBS/pynbs) python library.

<details>
<summary>Code</summary>

```python3
import pynbs
import os

for filename in os.listdir("."):
    if filename.endswith(".nbs"):
        file = pynbs.read(filename)
        parts = filename.split(".")[0].split(" - ")
        if len(parts) != 2:
            print(filename + " is not in the correct format")
            tmp = parts[0]
            parts = ["Unknown", tmp]

        file.header.song_author = parts[0].strip()
        file.header.song_name = parts[1].strip()
        file.save('./upgraded/' + filename.strip())
        print("[UPGRADED] " + parts[1].strip() + " - " +
              parts[0].strip())
```  

</details>

## Commands

`/globalkeyadd <UUID>` adds one global music key to the player with the defined UUID. This command requires OP to be
run.

`/globalkeylist` lists all the players with global keys and how many they have. This command requires OP to be run.

`/music` opens the jukebox GUI from anywhere and can be run by anyone by default.