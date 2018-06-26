# PianoScape

*其他語言版本: [English](README.en.md), [French](README.fr.md), [日本語](README.ja.md), [正體中文](README.zh-hant.md).*

## Overview
Pianoscape is a piano tutoring app (currently only on Android) that can be connected to a MIDI keyboard to assist in learning to play the piano. The app consists of two major components: the OMR system, and the Evaluation system.

The OMR (Optical Music Recognition) system performs image processing on music sheets in PDF/image formats to extract music elements, and export them into MusicXML format. This allows students to import music pieces that they currently own on paper, as they can scan the sheets into PDF/image formats. Also, PDF versions of music sheets can be found easily online, which can also be imported into the app.

The Evaluation system allows users to get real-time feedback on their playing performance on imported music sheets. This system renders MusicXML files into a flexible GUI, that can be adjusted in terms of zoom level as up to users' needs. After connecting the Android device with a MIDI keyboard, any keyboard inputs (notes/chords) will be visible on the GUI, which users can use to track their progress through a music piece. Any wrong notes or missed rests will be picked up using this system's comparison engine, and displayed to the user as real-time feedback. After the piece has been played, the user can see a summary of statistics of their performance. The MusicXML rendering is done using an educational license for SeeScore (from Dolphin Computing).

This app was created for our Fourth-Year Engineering Design Project.

![Pianoscape Home Screen](/Images/PianoScape.jpg?)


## Optical Music Recognition

The OMR system uses an image processing pipeline designed by us. It currently supports the following music elements:
● Notes: Whole, Half, Quarter, Eighth, Sixteenth, Beamed, Dotted
● Rests: Whole, Half, Quarter, Eighth, Sixteenth
● Clefs: Treble, Bass
● Accidentals and Key Signatures: Flat, Sharp, Natural
● Time Signatures: All numbers between 1 to 9
● Articulation Marks: Staccato
● Note Relationships: Ties, Chords

![OMR Engine's Image Processing Pipeline](/Images/imgproc_pipeline.png?)
