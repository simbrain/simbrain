/*
 * Created on Aug 13, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.simnet.util;

/**
 * 
 * <b>UniqueID</b>
 */
public class UniqueID {
  static long current= System.currentTimeMillis();
  
  static public synchronized String get(){
		    return "" + current++;
  }
}
