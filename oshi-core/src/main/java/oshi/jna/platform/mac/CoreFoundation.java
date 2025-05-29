/*
 * Copyright 2021-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native;

/**
 * CoreFoundation class. This class should be considered non-API as it may be removed if/when its code is incorporated
 * into the JNA project.
 */
public interface CoreFoundation extends com.sun.jna.platform.mac.CoreFoundation {

    CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);

    /**
     * The CFLocale opaque type provides support for obtaining available locales, obtaining localized locale names, and
     * converting among locale data formats.
     */
    class CFLocale extends CFTypeRef {
    }

    /**
     * Returns a copy of the logical locale for the current user.
     *
     * @return The logical locale for the current user that is formed from the settings for the current user’s chosen
     *         system locale overlaid with any custom settings the user has specified in System Preferences. May return
     *         a retained cached object, not a new object.
     *         <p>
     *         This reference must be released with {@link #CFRelease} to avoid leaking references.
     */
    CFLocale CFLocaleCopyCurrent();

    /**
     * CFDateFormatter objects format the textual representations of CFDate and CFAbsoluteTime objects, and convert
     * textual representations of dates and times into CFDate and CFAbsoluteTime objects.
     */
    class CFDateFormatter extends CFTypeRef {
    }

    /**
     * Enum of values used for {@link CFDateFormatterStyle} in {@link #CFDateFormatterCreate}. Use
     * {@link CFDateFormatterStyle#index} for the expected integer value corresponding to the C-style enum.
     */
    enum CFDateFormatterStyle {
        kCFDateFormatterNoStyle, kCFDateFormatterShortStyle, kCFDateFormatterMediumStyle, kCFDateFormatterLongStyle,
        kCFDateFormatterFullStyle;

        /**
         * Style for the type of {@link CFDateFormatterStyle} stored.
         *
         * @return a {@link CFIndex} representing the enum ordinal.
         */
        public CFIndex index() {
            return new CFIndex(this.ordinal());
        }
    }

    /**
     * Creates a new CFDateFormatter object, localized to the given locale, which will format dates to the given date
     * and time styles.
     *
     * @param allocator The allocator to use to allocate memory for the new object. Pass {@code null} or
     *                  {@code kCFAllocatorDefault} to use the current default allocator.
     * @param locale    The locale to use for localization. If {@code null} uses the default system locale. Use
     *                  {@link #CFLocaleCopyCurrent()} to specify the locale of the current user.
     * @param dateStyle The date style to use when formatting dates.
     * @param timeStyle The time style to use when formatting times.
     * @return A new date formatter, localized to the given locale, which will format dates to the given date and time
     *         styles. Returns {@code null} if there was a problem creating the object.
     *         <p>
     *         This reference must be released with {@link #CFRelease} to avoid leaking references.
     */
    CFDateFormatter CFDateFormatterCreate(CFAllocatorRef allocator, CFLocale locale, CFIndex dateStyle,
            CFIndex timeStyle);

    /**
     * Returns a format string for the given date formatter object.
     *
     * @param formatter The date formatter to examine.
     * @return The format string for {@code formatter}.
     */
    CFStringRef CFDateFormatterGetFormat(CFDateFormatter formatter);
}
