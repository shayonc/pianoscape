/**
 * SeeScore For Android
 * Dolphin Computing http://www.dolphin-com.co.uk
 */
/* SeeScoreLib Key for University of Waterloo, Canada

 IMPORTANT! This file is for University of Waterloo, Canada only.
 It must be used only for the application for which it is licensed,
 and must not be released to any other individual or company.

 Please keep it safe, and make sure you don't post it online or email it.
 Keep it in a separate folder from your source code, so that when you backup the code
 or store it in a source management system, the key is not included.
 */

package uk.co.dolphin_com.seescoreandroid;

import uk.co.dolphin_com.sscore.SScoreKey;

/**
 * The licence key to enable features in SeeScoreLib supplied by Dolphin Computing
 */

public class LicenceKeyInstance
{
    // licence keys: draw, multipart, android, id2, embed_id
    // The license key bytes below were replaced by dummy bytes.
    private static final int[] keycap = {0X12345,0X0};
    private static final int[] keycode = {0X12345678,0X12345678};

    public static final SScoreKey SeeScoreLibKey = new SScoreKey("University of Waterloo, Canada", keycap, keycode);
}
