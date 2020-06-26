package osp.Memory;
import java.util.*;

import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
 */

public class PageFaultHandler extends IflPageFaultHandler {
	/**
        This method handles a page fault.

        It must check and return if the page is valid,

        It must check if the page is already being brought in by some other
		thread, i.e., if the page has already pagefaulted
		(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.

        If none of the above is true, a new frame must be chosen
        and reserved until the swap in of the requested
        page into this frame is complete.

		Note that you have to make sure that the validating thread of
		a page is set correctly. To this end, you must set the page's
		validating thread using setValidatingThread() when a pagefault
		happens and you must set it back to null when the pagefault is over.

		If no free frame could be found, then a page replacement algorithm
		must be used to select a victim page to be replaced.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated
        from the frame and marked invalid. After the swap-in, the
        frame must be marked clean. The swap-ins and swap-outs
        must be performed using regular calls to read() and write().

	 ********The student implementation should define additional methods, e.g,
        a method to search for an available frame, and a method to select
        a victim page making its frame available.

		Note: multiple threads might be waiting for completion of the
		page fault. The thread that initiated the pagefault would be
		waiting on the IORBs that are tasked to bring the page in (and
		to free the frame during the swapout). However, while
		pagefault is in progress, other threads might request the same
		page. Those threads won't cause another pagefault, of course,
		but they would enqueue themselves on the page (a page is also
		an Event!), waiting for the completion of the original
		pagefault. It is thus important to call notifyThreads() on the
		page at the end -- regardless of whether the pagefault
		succeeded in bringing the page in or not.

        @param thread		 the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page 		 the memory page

		@return SUCCESS 	 is everything is fine; FAILURE if the thread
		dies while waiting for swap in or swap out or if the page is
		already in memory and no page fault was necessary (well, this
		shouldn't happen, but...). In addition, if there is no frame
		that can be allocated to satisfy the page fault, then it
		should return NotEnoughMemory

        @OSPProject Memory
	 */

	// Author: Dareen Bukhari   - 1607281
	//         Nada Abughazalah - 1606827
	// Last Update: April 18, 2020
	public static int do_handlePageFault(ThreadCB thread, int referenceType, PageTableEntry page) {

		// First, check for these cases: 1. Pagefault handler might be called incorrectly by other methods
		if (page.isValid() == true) { // Check if page is valid
			page.notifyThreads(); // Notify threads waiting on the page
			ThreadCB.dispatch(); // Call dispatch
			return FAILURE;
		}

		// 2. All frames are locked or reserved
		int lockedReservedCount = 0;
		for (int i=0; i<MMU.getFrameTableSize(); i++) { // Iterate through frame table entries
			if (MMU.getFrame(i).isReserved()==true || MMU.getFrame(i).getLockCount()>0 ) // If frame i is reserved or locked
				lockedReservedCount++; // Increment the counter
		}
		if (lockedReservedCount == MMU.getFrameTableSize()){ //If the counter equals the frame table size
			page.notifyThreads(); // Notify threads waiting on the page
			ThreadCB.dispatch(); // Call dispatch
			return NotEnoughMemory; // Return NotEnoughMemory and exit
		}

		page.setValidatingThread(thread); //Set the thread that caused a pagefault on the page
		FrameTableEntry frame;
		SystemEvent pfEvent = new SystemEvent("Kernel mode - Page fault occurred"); // Create new SystemEvent object

		thread.suspend(pfEvent); // Suspend thread on pfEevent

		//Now, process the pagefault
		if(numFreeFrames() > 0) {	//If there is a free frame, use it
			frame = getFreeFrame();
			page.setFrame(frame);
			frame.setReserved(thread.getTask()); 
			swapIn(thread, page);

			if (thread.getStatus() == ThreadKill) { //If pagefault-causing thread got killed waiting for swap out/swap in
				page.notifyThreads(); // Notify threads waiting on the page
				page.setValidatingThread(null);// Page fault is over, so validting thread becomes null
				page.setFrame(null); // Set page's frame to null
				pfEvent.notifyThreads(); // Notify the thread that caused the pagefault
				ThreadCB.dispatch(); // Call dispatch
				return FAILURE;
			}
		}

		else { 
			frame  = SecondChance(); // Call a page replacement algorithm
			frame.setReserved(thread.getTask()); // Reserve the frame

			if(frame.isDirty()==true) { //Frame is dirty
				PageTableEntry p = frame.getPage(); // Store the page in p
				swapOut(thread, frame); // Perform swap out
				if (thread.getStatus() == ThreadKill) {//If pagefault-causing thread got killed waiting for swap out/swap in
					page.notifyThreads(); // Notify threads waiting on the page
					page.setValidatingThread(null);// Page fault is over, so validting thread becomes null
					page.setFrame(null); // Set page's frame to null
					pfEvent.notifyThreads(); // Notify the thread that caused the pagefault
					ThreadCB.dispatch();
					return FAILURE;
				}
				//Free the dirty frame and update 
				frame.setReferenced(false); // Change reference bit to false
				p.setValid(false); // Set valid bit to 0
				p.setFrame(null); // Set page's frame to null
				frame.setDirty(false); // Change dirty bit to false
				frame.setPage(null); // Nullify the frame's page
				page.setFrame(frame); // Set the page's frame

				swapIn(thread, page); // Swap in the new page

				if (thread.getStatus() == ThreadKill) {//If pagefault-causing thread got killed waiting for swap in
					page.notifyThreads(); // Notify threads waiting on the page
					page.setValidatingThread(null);// Page fault is over, so validting thread becomes null
					page.setFrame(null); // Set page's frame to null
					pfEvent.notifyThreads(); // Notify the thread that caused the pagefault
					ThreadCB.dispatch(); // Call dispatch
					return FAILURE;
				}

			} 

			else { // Frame is NOT dirty
				page.setFrame(frame); // Set the page's frame
				swapIn(thread, page); // Swap in the new page
				if (thread.getStatus() == ThreadKill) {//If pagefault-causing thread got killed waiting for swap out/swap in
					page.notifyThreads(); // Notify threads waiting on the page
					page.setValidatingThread(null);// Page fault is over, so validting thread becomes null
					page.setFrame(null); // Set page's frame to null
					pfEvent.notifyThreads(); // Notify the thread that caused the pagefault
					ThreadCB.dispatch(); // Call dispatch
					return FAILURE;
				}
			}

		} // Page replacement done

		// If all is well, update page table and frame table
		page.setValid(true); // Set valid bit to 1
		frame.setPage(page); // Set the frame's page to the new page
		frame.setReferenced(true); // Set referenced bit to 1
		// Dirty bit = 1 only if reference type is MemoryLock 
		if (referenceType==MemoryLock || referenceType==MemoryRead)
			frame.setDirty(false); 
		else
			frame.setDirty(true);

		//Final steps
		frame.setUnreserved(thread.getTask()); // Un-reserve the frame that satisfied the pagefult
		page.setValidatingThread(null);	// Page fault is over, so validting thread becomes null
		page.notifyThreads(); // Notify threads waiting on the page
		pfEvent.notifyThreads(); // Notify the thread that caused the pagefault
		ThreadCB.dispatch(); // Call dispatch
		return SUCCESS;
	}

	/**
    	Returns the current number of free frames. It does not matter where the
		search in the frame table starts, but this method must not change the value
		of the reference bits, dirty bits or MMU.Cursor
	 */
	// Author: Dareen Bukhari   - 1607281
	//         Nada Abughazalah - 1606827
	// Last Update: April 17, 2020
	static int numFreeFrames() {
		int freeFramesCount = 0;
		for (int i=0; i<MMU.getFrameTableSize(); i++) {
			if (!MMU.getFrame(i).isReserved() //If frame not reserved
					&& MMU.getFrame(i).getPage()==null //and page is null
					&& MMU.getFrame(i).getLockCount()==0 //and frame not locked,
					&& !MMU.getFrame(i).isReferenced() //not referenced, 
					&& !MMU.getFrame(i).isDirty()) //and not dirty
				freeFramesCount++;
		}
		return freeFramesCount;
	}

	/**
	Returns the first free frame starting the search from frame[0]
	 */
	// Author: Dareen Bukhari   - 1607281
	//         Nada Abughazalah - 1606827
	// Last Update: April 17, 2020
	static FrameTableEntry getFreeFrame() { //this is what we used
		//FrameTableEntry frame = null; // Create frame variable
		for (int i=0; i<MMU.getFrameTableSize(); i++) { // Loop through the frame table
			if(!MMU.getFrame(i).isReserved() //If frame not reserved
					&& MMU.getFrame(i).getPage()==null //and page is null
					&& MMU.getFrame(i).getLockCount()==0 //and frame not locked,
					&& !MMU.getFrame(i).isReferenced() //not referenced, 
					&& !MMU.getFrame(i).isDirty()) { //and not dirty
				return MMU.getFrame(i); // Retrieve this frame
			}
		}
		return null;
	}

	/**
    Frees frames using the following Second Chance approach
    and returns one frame. The search uses the MMU variable MMU.Cursor to
    specify the starting frame index of the search.	
	 */
	// Author: Dareen Bukhari   - 1607281
	//         Nada Abughazalah - 1606827
	// Last Update: April 18, 2020
	static FrameTableEntry SecondChance() {

		boolean i = true;
		int counter = 0;
		int frameID = 0;
		boolean frameIDFlag = false;
		///Phase I
		while(i) { 

			if (numFreeFrames() == MMU.wantFree) //If free frames equals wantFree
				return getFreeFrame();

			if(MMU.getFrame(MMU.Cursor).isReferenced() == true) //Frame is not referenced
				MMU.getFrame(MMU.Cursor).setReferenced(false); //Set its reference bit to 0

			// Checking for a clean page
			else if (MMU.getFrame(MMU.Cursor).getPage() != null // Page not null
					&& !MMU.getFrame(MMU.Cursor).isReferenced()  // Not referenced
					&& MMU.getFrame(MMU.Cursor).getLockCount()==0 // Not locked
					&& !MMU.getFrame(MMU.Cursor).isReserved() // Not reserved
					&& !MMU.getFrame(MMU.Cursor).isDirty()) { // Not dirty 

				// Freeing frames until it equals wantFree
				PageTableEntry p = MMU.getFrame(MMU.Cursor).getPage(); // Save frame's page
				MMU.getFrame(MMU.Cursor).setPage(null); // Set page to null
				MMU.getFrame(MMU.Cursor).setDirty(false); // Dirty bit = 0
				MMU.getFrame(MMU.Cursor).setReferenced(false); // Referenced bit = 0
				// Updating on page table: page becomes invalid and frame null
				p.setValid(false);
				p.setFrame(null);
			}

			// Get the ID of the first dirty frame if there aren't enough clean frames
			if (MMU.getFrame(MMU.Cursor).getLockCount()==0 
					&& !MMU.getFrame(MMU.Cursor).isReserved()
					&& MMU.getFrame(MMU.Cursor).isDirty()
					&& !frameIDFlag) {
				frameID = MMU.getFrame(MMU.Cursor).getID();
				frameIDFlag = true;
			}

			MMU.Cursor = (MMU.Cursor + 1) % MMU.getFrameTableSize(); // Increment the cursor

			counter++; // Increment the counter
			if(counter == (MMU.getFrameTableSize()*2)) // If we have cycled through the whole frame
				i = false; //Exit the while loop
		} // End while(i) 

		///Phase II - iterated through the frame table and exited
		if (frameIDFlag==true && numFreeFrames() < MMU.wantFree) 
			return MMU.getFrame(frameID); // Return first dirty frame

		else //If no. of free frames not equal to wantFree
			return getFreeFrame();	
	}

	// Author: Dareen Bukhari   - 1607281
	//         Nada Abughazalah - 1606827
	// Last Update: April 17, 2020
	public static void swapIn(ThreadCB thread, PageTableEntry page) {
		// Perform read operation of the file to get the page to be swapped in
		page.getTask().getSwapFile().read(page.getID(), page, thread);
	}

	// Author: Dareen Bukhari   - 1607281
	//         Nada Abughazalah - 1606827
	// Last Update: April 17, 2020
	public static void swapOut(ThreadCB thread, FrameTableEntry frame) {
		// Perform write operation of the file to write the frame to be swapped out
		PageTableEntry p = frame.getPage();
		p.getTask().getSwapFile().write(p.getID(), p, thread);    	
	}
}
