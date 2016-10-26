# On Road Media Sender
A tool for uploading media files to a FTP server, with low-res converting, all in a simple way.

It's designed for journalists in working on road, just after edit a video file with a NLE like Avid, afford to send export the simplest way it is: a drag & drop.

Tested and approved for run with Windows 7 (and +) and OSX Yosemite (and +).

Translated in english (as i can), and natural french.

## Usage
After change the configuration for setup the FTP destination(s), drag&drop a file in table and it will be converted and sended. Or just sended.

![screenshot](https://raw.githubusercontent.com/hdsdi3g/OnRoadMediaSender/gh-pages/img/screenshot.jpg)

## Functionalities
- auto-detect files format and auto select the action todo with it
- transcode only hi-bitrates video and audio files: if the submitted file has a low bitrate, it don't transcode it
- can transcode video only (w/o audio), or audio only (make an audio low-res from a video file)
- multiple video and audio profiles can be setup
- multiple FTP destinations can be setup
- handle enterlaced rescale, MOV/MXF streams (2 audio mono stream to a stereo stream)
- if the file is a document, it will be sended as it
- convert to a mp4/mov file with fast-start: partial sended files are playable
- Transcoding operation detect previousely transcoded files
- FTP transfert function can detect and ignore the sended files or resume transferts for partially transferred files
- FTP transfert never stop to try to send files ever if the link is not stable
- App log can be see in separate window (and log verbosity can be changed here)
- App name and logo can be changed for you're company logo

## Setup
Write in Java FX 8, it run with **Java 8** JRE and some dependencies :
- [ffprobe-jaxb-1.0.jar](https://github.com/hdsdi3g/ffprobe-jaxb/releases/download/v1.0/ffprobe-jaxb-1.0.jar)
- commons-beanutils-1.9.2.jar
- commons-configuration2-2.1.jar
- commons-io-2.4.jar
- commons-lang3-3.4.jar
- commons-logging-1.1.3.jar
- commons-net-3.1.jar
- log4j-1.2.17.jar
- slf4j-api-1.7.2.jar
- slf4j-log4j12-1.7.2.jar

It need **absoluty** ffmpeg and ffprobe v3 for media operations.

## License 
GNU/GPL v3 - (C) Copyright 2016 hdsdi3g for hd3g.tv - Ugly logo CC NC By SA
