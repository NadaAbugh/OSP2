package osp.Threads;

import osp.IFLModules.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**    
       The timer interrupt handler.  This class is called upon to
       handle timer interrupts.

       @OSPProject Threads
 */
public class TimerInterruptHandler extends IflTimerInterruptHandler
{
	/**
       This basically only needs to reset the times and dispatch
       another process.

       @OSPProject Threads
	 */
	 /* AUTHORS				ID
	 * Dareen Bukhari		1607281
	 * Nada Abughazalah		1606827
	 * LAST EDIT: 08 MAR 2020
	 */
	public void do_handleInterrupt() {

		HTimer.set(0); //set interrupt timer to 0
		ThreadCB.dispatch(); //schedule next thread to run

	}


	/*
       Feel free to add methods/fields to improve the readability of your code
	 */

}

/*
      Feel free to add local classes to improve the readability of your code
 */
