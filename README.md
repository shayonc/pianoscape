# Pianoscape

*其他語言版本: [English](README.en.md), [French](README.fr.md), [日本語](README.ja.md), [正體中文](README.zh-hant.md).*

## Overview
Pianoscape is a piano tutoring app (currently only on Android) that can be connected to a MIDI keyboard to assist in learning to play the piano. As students play the keyboard, the app tracks the key presses and presents feedback. It also displays music sheets of choice, and assists students in terms of playing the correct notes/chords. The app consists of two major components: the OMR system, and the Evaluation system.

The OMR (Optical Music Recognition) system performs image processing on music sheets in PDF/image formats to extract music elements, and export them into MusicXML format. This can include music sheets scanned from standardized piano books, such as the RCM book series. Also, PDF versions of music sheets can be found easily online, which can also be imported into the app.

The Evaluation system allows users to get real-time feedback on their playing performance on imported music sheets. This system renders MusicXML files into a flexible GUI, that can be adjusted in terms of zoom level as up to users' needs. After connecting the Android device with a MIDI keyboard, any keyboard inputs (notes/chords) will be visible on the GUI, which users can use to track their progress through a music piece. Any wrong notes or missed rests will be picked up using this system's comparison engine, and displayed to the user as real-time feedback. After the piece has been played, the user can see a summary of statistics of their performance. The MusicXML rendering is done using an educational license for SeeScore (from Dolphin Computing).

These features would allow students to learn to play piano without taking part in expensive lessons from instructors. PianoScape would also aid students in practicing more effectively on their own, even if they choose to take lessons. This app was created for our Fourth-Year Engineering Design Project.

<div style="text-align:center">
    <img src ="/Images/Home-Screen.jpg" />
    <img src ="/Images/Score-Selected.jpg" />
</div>
<div style="text-align:center">
    <img src ="/Images/Playing-Screen.jpg" />
</div>

## Optical Music Recognition

The OMR system uses an image processing pipeline designed by us using OpenCV. It currently supports the following music elements:

● Notes: Whole, Half, Quarter, Eighth, Sixteenth, Beamed, Dotted

● Rests: Whole, Half, Quarter, Eighth, Sixteenth

● Clefs: Treble, Bass

● Accidentals and Key Signatures: Flat, Sharp, Natural

● Time Signatures: All numbers between 1 to 9

● Articulation Marks: Staccato

● Note Relationships: Ties, Chords

<div style="text-align:center"><img src ="/Images/Imgproc-Pipeline.png" /></div>
