package osp.Memory;
import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
 */
public class MMU extends IflMMU {	

	// declaring static variables for ESC 
	public static int Cursor;
	public static int wantFree;

	/**
        This method is called once before the simulation starts.
		Can be used to initialize the frame table and other static variables.

        @OSPProject Memory        
	*/
	// Author: Dareen Bukhari   - 1607281 
	// Last Update: April 14, 2020 
	public static void init() { // Initialize the static variables
		Cursor = 0;
		wantFree = 1;

		for(int i=0; i< MMU.getFrameTableSize(); i++) // Initializing the frame entries 
			setFrame(i, new FrameTableEntry(i)); // Set each entry to an object from FrameTableEntry

	}

	/**
       This method handlies memory references. The method must
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault
       by making an interrupt if the page is invalid, finally,
       if the page is still valid, i.e., not swapped out by another
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue,
       and it is possible that some other thread will take away the frame.)

       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
	 */

	// Author: Dareen Bukhari   - 1607281
	//         Nada Abughazalah - 1606827
	// Last Update: April 14, 2020 
	static public PageTableEntry do_refer(int memoryAddress,int referenceType, ThreadCB thread) {

		int pageSize = (int) Math.pow(2, getVirtualAddressBits()-getPageAddressBits()); // Calculate the page size  	
		int pageNumber = memoryAddress/pageSize; // Calculate the page number

		PageTableEntry pageTableEntry = getPTBR().pages[pageNumber]; // Get the pageTableEntry that points to the passed memoryAddress

		if(pageTableEntry.isValid() == false) { // If page is invalid

			if (pageTableEntry.getValidatingThread() == null) { // If no other thread caused a PF on this page
				// Initiate page fault using interrupt. Set the appropriate InterruptVector fields: 
				InterruptVector.setInterruptType(referenceType); // Interrupt type
				InterruptVector.setPage(pageTableEntry); // Page that caused interrupt
				InterruptVector.setReferenceType(referenceType); // Memory reference type
				InterruptVector.setThread(thread); // Thread about to cause interrupt
				CPU.interrupt(PageFault); // Call the interrupt of type PageFault
			}
			else  // Another thread caused the PF, and the page is already on its way to main memory
				thread.suspend(pageTableEntry); // Suspend the thread on the page until page becomes valid

			if(thread.getStatus() == ThreadKill) // Thread got killed waiting to become valid
				return pageTableEntry;  // then return pageTableEntry and exit 		

		} 
		pageTableEntry.getFrame().setReferenced(true); // Set reference bit to true
		if(referenceType == MemoryWrite) // If the reference type is MemoryWrite
			pageTableEntry.getFrame().setDirty(true); // Only then, set the dirty bit to true
		return pageTableEntry; 			
	}    


	/** Called by OSP after printing an error message. The student can
		insert code here to print various tables and data structures
		in their state just after the error happened.  The body can be
		left empty, if this feature is not used.

		@OSPProject Memory
	 */
	public static void atError() {
		// your code goes here (if needed)
	}

	/** Called by OSP after printing a warning message. The student
		can insert code here to print various tables and data
		structures in their state just after the warning happened.
		The body can be left empty, if this feature is not used.

      @OSPProject Memory
	 */
	public static void atWarning() {
		// your code goes here (if needed)
	}
}
