/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.hardware;

/**
 * The Central Processing Unit (CPU) or the processor is the portion of a computer system that carries 
 * out the instructions of a computer program, and is the primary element carrying out the computer's 
 * functions.
 * @author dblock[at]dblock[dot]org
 */
public interface Processor {
	/**
	 * Processor vendor.
	 * @return
	 *  String.
	 */
	String getVendor();
}
