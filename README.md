# Introduction
This project contains Unreal Engine 4 Launcher + Marketplace for Linux based systems.

Everyone is free to use / change / edit this system.
Also, thanks to https://github.com/Allar/ue4-mp-downloader this project, I was able to make basic parts for Login / Downloading.

This system is made with IntelliJ and Java8 + JavaFX.

# How to launch
1. Navigate to 'Compiled' folder.
2. Assign 'start.sh' execution permissions.
3. Start 'start.sh' script.

_PS. Both start.sh and UnrealEngineLauncher.jar must be in same folder and execution must be started from that folder._

# Update [2020-03-01]
Currently this project is under reconstruction, since Epic Games made some changes into their API, in same time this launcher will be reworked.

Better local storage (less requests to API, faster loading), cleaner structure and adaptation to new API.

Main parts of launcher are already reworked.

To do:
1. Fix 'Launch Engine' action.
2. Load full marketplace (currently only owned assets are loaded).
3. If possible, find a way to check if downloaded files are correct (can't find a way for compare Hash from their API).
4. Rework some design parts.

### WARNING
Epic Games implemented Captcha to their system, after several bad tries, Captcha will be required, which is not supported with Launcher,
currently I can't find a way to perform captcha correctly, so if authentication will start failing, it may be captcha, or some different parts,
which I haven't covered yet, so fill issue if you will encounter any bugs.

#### Working parts:
1. Authentication.
2. Two factor authentication.
3. Owned Assets loading.
4. Engine and projects loading (tested with 4.22 version).
5. Assets download.