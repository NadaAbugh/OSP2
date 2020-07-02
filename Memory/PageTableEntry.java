package osp.Memory;
import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
The PageTableEntry object contains information about a specific virtual
page in memory, including the page frame in which it resides.

@OSPProject Memory
*/

public class PageTableEntry extends IflPageTableEntry {

	/**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);

       as its first statement.

       @OSPProject Memory
	 */

	// Author: Dareen Bukhari - 1607281
	// Last Update: April 6, 2020
	public PageTableEntry(PageTable ownerPageTable, int pageNumber) {
		super(ownerPageTable, pageNumber); // Calls the superclass constructor
	}


	/**
       This method increases the lock count on the page by one.

	   The method must FIRST increment lockCount, THEN
	   check if the page is valid, and if it is not and no
	   page validation event is present for the page, start page fault
	   by calling PageFaultHandler.handlePageFault().

	   @return SUCCESS or FAILURE
	   FAILURE happens when the pagefault due to locking fails or
	   that the created IORB thread gets killed.

	   @OSPProject Memory
	 */

	// Author: Dareen Bukhari   - 1607281, 
	//         Nada AbuGhazalah - 1606827
	// Last Update: April 13, 2020 
	public int do_lock(IORB iorb) {
		// Check if the page is in main memory by checking the validity of the page
		if (this.isValid() == false) { // Initiate page fault
		
			if (getValidatingThread() == null) { // Page not involved in page fault
				PageFaultHandler.handlePageFault(iorb.getThread(), MemoryLock, this); //Page fault caused by locking
				
				if(iorb.getThread().getStatus() == ThreadKill) //If thread got killed waiting
					return FAILURE;
			}
			else if (getValidatingThread() != iorb.getThread()) {  // NThread2 of same task, if Th2<>Th1, SUSPEND 
				// Thread that created iorb killed while waiting for lock to complete
				iorb.getThread().suspend(this); // Suspend the thread until page (ie, this) becomes valid		
				// If page is still invalid			
				if(this.isValid() == false) 
					return FAILURE;
			}
		}
		
		// If page was locked successfully or if Th2=Th1
		getFrame().incrementLockCount();
		return SUCCESS; 
	}


	/**
       This method decreases the lock count on the page by one.

	   This method must decrement lockCount, but not below zero.

	   @OSPProject Memory
	 */

	// Author: Dareen Bukhari   - 1607281, 
	//         Nada AbuGhazalah - 1606827
	// Last Update: April 8, 2020
	public void do_unlock() { 

		if(this.getFrame().getLockCount() > 0) { // Ensure the lock count not negative
			this.getFrame().decrementLockCount(); // Decrement the lock count
		}

	}

}