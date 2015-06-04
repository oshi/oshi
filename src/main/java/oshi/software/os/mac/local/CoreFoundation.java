/*
 * Copyright (c) Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac.local;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * Memory and CPU stats from vm_stat and sysctl
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface CoreFoundation extends Library {
	CoreFoundation INSTANCE = (CoreFoundation) Native.loadLibrary(
			"CoreFoundation", CoreFoundation.class);

	public static final int UTF_8 = 0x08000100;

	int CFArrayGetCount(CFArrayRef array);

	CFTypeRef CFArrayGetValueAtIndex(CFArrayRef array, int index);

	void CFRelease(CFTypeRef blob);

	public class CFTypeRef extends PointerType {
	}

	public class CFArrayRef extends PointerType {
	}

	public class CFDictionaryRef extends PointerType {
	}

	public class CFStringRef extends PointerType {
		public static CFStringRef toCFString(String s) {
			final char[] chars = s.toCharArray();
			int length = chars.length;
			return CoreFoundation.INSTANCE.CFStringCreateWithCharacters(null,
					chars, new NativeLong(length));
		}
	}

	CFStringRef CFStringCreateWithCharacters(Object object, char[] chars,
			NativeLong length);

	boolean CFDictionaryGetValueIfPresent(CFDictionaryRef dictionary,
			CFStringRef key, PointerType value);

	Pointer CFDictionaryGetValue(CFDictionaryRef dictionary, CFStringRef key);

	boolean CFStringGetCString(Pointer foo, Pointer buffer, long maxSize,
			int encoding);

	long CFStringGetLength(Pointer str);

	long CFStringGetMaximumSizeForEncoding(long length, int encoding);

	boolean CFBooleanGetValue(Pointer booleanRef);

}
